package com.example.util.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.example.util.NetworkBypass
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Observes network availability and transitions between Wi-Fi and Cellular networks.
 * Recreates connection pools, flushes DNS caches, and notifies active playback sessions.
 */
object NetworkMonitor {
    private const val TAG = "NetworkMonitor"

    fun interface NetworkChangeListener {
        fun onNetworkChanged(isAvailable: Boolean)
    }

    private val listeners = CopyOnWriteArrayList<NetworkChangeListener>()
    @Volatile private var isRegistered = false
    @Volatile private var lastNetworkType: Int = -1
    /** The network the device uses RIGHT NOW (the default one). Only a change of THIS network is a real switch. */
    @Volatile private var defaultNetwork: Network? = null

    fun register(context: Context) {
        if (isRegistered) return
        synchronized(this) {
            if (isRegistered) return
            val appContext = context.applicationContext
            ViauraDns.init(appContext)

            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return

            // The default-network callback reports ONLY the network the phone really uses. The old request-based callback
            // reported EVERY network that had internet (Wi-Fi AND mobile data at the same time, plus every signal-strength
            // change of the mobile one), so the "type" flipped Wi-Fi <-> Cellular again and again while nothing had changed:
            // each flip emptied the connection pool and reloaded the playing video, which showed up as endless buffering.
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    handleDefaultNetwork(appContext, cm, network)
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    // same network, only its properties changed (signal, validation): not a switch
                    handleDefaultNetwork(appContext, cm, network)
                }

                override fun onLost(network: Network) {
                    if (defaultNetwork == network) {
                        defaultNetwork = null
                        notifyListeners(false)
                    }
                }
            }

            try {
                cm.registerDefaultNetworkCallback(callback)
                isRegistered = true
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to register network callback: ${t.message}")
            }
        }
    }

    private fun handleDefaultNetwork(context: Context, cm: ConnectivityManager, network: Network) {
        val caps = cm.getNetworkCapabilities(network) ?: return
        val currentType = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> 1
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> 2
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> 3
            else -> 0
        }

        val previous = defaultNetwork
        val first = previous == null && lastNetworkType == -1
        val switched = previous != null && previous != network
        // coming back after a loss (previous == null, type known) is also a new connection
        val reconnected = previous == null && lastNetworkType != -1
        defaultNetwork = network
        lastNetworkType = currentType

        if (!first && (switched || reconnected)) {
            // The phone really moved to another network (Wi-Fi <-> mobile, other Wi-Fi, or back online after a loss)
            ViauraHttpClient.onNetworkChanged()
            try { NetworkBypass.refresh(context) } catch (_: Throwable) {}
            notifyListeners(true)
        }
    }

    fun addListener(listener: NetworkChangeListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: NetworkChangeListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners(isAvailable: Boolean) {
        for (l in listeners) {
            try {
                l.onNetworkChanged(isAvailable)
            } catch (_: Throwable) {}
        }
    }
}
