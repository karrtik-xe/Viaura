package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary

/**
 * Shows the reason of the previous run's crash (see CrashReporter). Same bottom sheet as the Download sheet (it follows the
 * glass / normal look of the app) with a Copy and a Close button at the very bottom.
 */
@Composable
fun CrashReportDialog(
    report: String,
    isDark: Boolean,
    glassBlurTransparency: Float,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val primary = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondary = if (isDark) DarkTextSecondary else LightTextSecondary
    DownloadGlassSheet(isDark = isDark, glassBlurTransparency = glassBlurTransparency, onDismiss = onDismiss) {
        Text(
            text = "The app crashed last time",
            color = primary, fontSize = 22.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
        )
        // short enough that the sheet never counts as "overflowing" (that adds empty room below), so Close / Copy sit at the very bottom
        val reportMax = minOf(260.dp, (LocalConfiguration.current.screenHeightDp * 0.28f).dp)
        Box(modifier = Modifier.fillMaxWidth().heightIn(max = reportMax).verticalScroll(rememberScrollState())) {
            Text(text = report, color = secondary, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Close", color = primary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(AccentGradient)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        try {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("crash report", report))
                            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                        } catch (_: Throwable) {
                        }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Copy", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
