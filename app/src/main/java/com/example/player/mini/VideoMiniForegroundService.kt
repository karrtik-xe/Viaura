package com.example.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

/**
 * Foreground service that keeps the video mini player alive (and its sound playing) when the screen is turned off
 * or the app is in the background. mpv plays the audio; this service only holds the process at foreground priority
 * and shows a small notification with Play/Pause and Close.
 */
class VideoMiniPlaybackService : Service() {

    companion object {
        private const val CHANNEL_ID = "video_mini_playback_channel_v2"
        private const val OLD_CHANNEL_ID = "video_mini_playback_channel_v1"
        private const val NOTIFICATION_ID = 4041
        private const val ACTION_START = "com.example.player.VIDEO_MINI_START"
        private const val ACTION_REFRESH = "com.example.player.VIDEO_MINI_REFRESH"
        private const val ACTION_TOGGLE = "com.example.player.VIDEO_MINI_TOGGLE"
        private const val ACTION_CLOSE = "com.example.player.VIDEO_MINI_CLOSE"
        private const val ACTION_STOP = "com.example.player.VIDEO_MINI_STOP"

        fun start(context: Context) = send(context, ACTION_START, foreground = true)
        fun refresh(context: Context) = send(context, ACTION_REFRESH, foreground = false)
        fun stop(context: Context) = send(context, ACTION_STOP, foreground = false)

        private fun send(context: Context, action: String, foreground: Boolean) {
            try {
                val intent = Intent(context.applicationContext, VideoMiniPlaybackService::class.java).setAction(action)
                if (foreground && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context.applicationContext, intent)
                } else {
                    context.applicationContext.startService(intent)
                }
            } catch (_: Throwable) {}
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        startForegroundCompat(buildNotification())
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Viaura:VideoMiniPlayback")?.apply {
                setReferenceCounted(false)
                acquire(4 * 60 * 60 * 1000L)
            }
        } catch (_: Throwable) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { shutdown(); return START_NOT_STICKY }
            ACTION_CLOSE -> { VideoMiniPlayerManager.close(); shutdown(); return START_NOT_STICKY }
            ACTION_TOGGLE -> VideoMiniPlayerManager.togglePlayPause()
        }
        if (!VideoMiniPlayerManager.isActive) { shutdown(); return START_NOT_STICKY }
        startForegroundCompat(buildNotification())
        return START_NOT_STICKY
    }

    private fun shutdown() {
        try { ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE) } catch (_: Throwable) {}
        stopSelf()
    }

    override fun onDestroy() {
        try { wakeLock?.let { if (it.isHeld) it.release() } } catch (_: Throwable) {}
        wakeLock = null
        super.onDestroy()
    }

    private fun startForegroundCompat(n: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(this, NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, n)
            }
        } catch (_: Throwable) {}
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        // The old channel was IMPORTANCE_LOW (status bar icon + song-style card on top); a channel's importance cannot change, so it is replaced.
        try { nm.deleteNotificationChannel(OLD_CHANNEL_ID) } catch (_: Throwable) {}
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Video mini player", NotificationManager.IMPORTANCE_MIN).apply {
                    setShowBadge(false)
                    setSound(null, null)
                    enableVibration(false)
                    enableLights(false)
                    lockscreenVisibility = Notification.VISIBILITY_SECRET
                }
            )
        }
    }

    private fun serviceIntent(action: String, code: Int): PendingIntent =
        PendingIntent.getService(
            this, code,
            Intent(this, VideoMiniPlaybackService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun buildNotification(): Notification {
        val playing = VideoMiniPlayerManager.currentIsPlaying()
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(VideoMiniPlayerManager.currentTitle())
            .setContentText(if (playing) "Playing • Mini player" else "Paused • Mini player")
            .setContentIntent(open)
            .setOngoing(playing)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
            .addAction(
                if (playing) R.drawable.viaura_pause_solid else R.drawable.viaura_play_solid,
                if (playing) "Pause" else "Play",
                serviceIntent(ACTION_TOGGLE, 1)
            )
            .addAction(R.drawable.viaura_close_circle, "Close", serviceIntent(ACTION_CLOSE, 2))
            .build()
    }
}
