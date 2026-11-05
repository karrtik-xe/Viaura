package com.example.util.network

import android.content.Context
import com.example.util.NetworkBypass
import okhttp3.Dns
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.Proxy
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap

/**
 * DNS-over-HTTPS resolver with automatic fallback:
 * 1. User selected DNS (from NetworkBypass)
 * 2. Cloudflare (1.1.1.1)
 * 3. Google (8.8.8.8)
 * 4. AdGuard DNS
 * 5. System resolver fallback
 *
 * Remembers which DoH provider succeeded per host, caches IP answers in memory with TTL,
 * and seamlessly flushes on network changes.
 */
object ViauraDns : Dns {
    private const val DNS_CACHE_TTL_MS = 10 * 60_000L

    data class DohEndpoint(val name: String, val url: String)

    val CLOUDFLARE = DohEndpoint("Cloudflare", "https://1.1.1.1/dns-query")
    val GOOGLE = DohEndpoint("Google", "https://8.8.8.8/resolve")
    val ADGUARD = DohEndpoint("AdGuard", "https://dns.adguard.com/dns-query")

    // Host -> (List of IP addresses, expiration timestamp)
    private val memoryCache = ConcurrentHashMap<String, Pair<List<InetAddress>, Long>>()

    // Host -> Working DoH URL
    private val hostPreferredDoh = ConcurrentHashMap<String, String>()

    // Host -> DoH URL whose answer did not lead to a working connection (that provider is tried last for the host)
    private val hostAvoidDoh = ConcurrentHashMap<String, String>()

    @Volatile private var appContext: Context? = null

    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
    }

    fun clearCache() {
        memoryCache.clear()
        hostPreferredDoh.clear()
        hostAvoidDoh.clear()
    }

    /**
     * A connection to [hostname] failed although the name resolved (blocked / wrong address): the cached answer is dropped
     * and the next lookup asks the NEXT DoH provider first. The provider that finally works is remembered for the host.
     */
    fun reportConnectFailure(hostname: String) {
        val host = hostname.trim().lowercase()
        if (host.isEmpty() || isIpLiteral(host) || isLocalHost(host)) return
        memoryCache.remove(host)
        hostPreferredDoh.remove(host)?.let { hostAvoidDoh[host] = it }
    }

    override fun lookup(hostname: String): List<InetAddress> {
        val host = hostname.trim().lowercase()
        if (host.isEmpty()) throw UnknownHostException("Empty hostname")

        // 1. IP literal check
        if (isIpLiteral(host)) {
            return try {
                listOf(InetAddress.getByName(host))
            } catch (t: Throwable) {
                throw UnknownHostException("Invalid IP: $host")
            }
        }

        // 2. Localhost check
        if (isLocalHost(host)) {
            return try {
                Dns.SYSTEM.lookup(host)
            } catch (t: Throwable) {
                throw UnknownHostException("Failed to resolve local host: $host")
            }
        }

        // 3. In-memory DNS cache
        val now = System.currentTimeMillis()
        memoryCache[host]?.let { (ips, expiry) ->
            if (now < expiry && ips.isNotEmpty()) {
                return ips
            }
        }

        // 4. Build fallback provider list
        val providers = mutableListOf<String>()
        val ctx = appContext
        if (ctx != null) {
            try {
                if (NetworkBypass.isDohEnabled(ctx)) {
                    val userDohIdx = NetworkBypass.dohProviderIndex(ctx)
                    val userProvider = NetworkBypass.dohProviders.getOrNull(userDohIdx)?.url
                    if (!userProvider.isNullOrBlank()) {
                        providers.add(userProvider)
                    }
                }
            } catch (_: Throwable) {}
        }

        val preferred = hostPreferredDoh[host]
        if (!preferred.isNullOrBlank() && !providers.contains(preferred)) {
            providers.add(0, preferred)
        }

        val defaultList = listOf(CLOUDFLARE.url, GOOGLE.url, ADGUARD.url)
        for (url in defaultList) {
            if (!providers.contains(url)) {
                providers.add(url)
            }
        }
        // the provider whose answer did not connect goes last for this host
        hostAvoidDoh[host]?.let { avoid ->
            if (providers.size > 1 && providers.remove(avoid)) providers.add(avoid)
        }

        // 5. Try DoH providers in order
        for (providerUrl in providers) {
            try {
                val resolved = queryDoh(providerUrl, host)
                if (!resolved.isNullOrEmpty()) {
                    memoryCache[host] = Pair(resolved, now + DNS_CACHE_TTL_MS)
                    hostPreferredDoh[host] = providerUrl
                    return resolved
                }
            } catch (_: Throwable) {}
        }

        // 6. System resolver fallback
        try {
            val systemResolved = Dns.SYSTEM.lookup(host)
            if (systemResolved.isNotEmpty()) {
                memoryCache[host] = Pair(systemResolved, now + DNS_CACHE_TTL_MS)
                return systemResolved
            }
        } catch (e: UnknownHostException) {
            throw e
        } catch (t: Throwable) {
            throw UnknownHostException("Unable to resolve $host: ${t.message}")
        }

        throw UnknownHostException("Could not resolve host: $host")
    }

    private fun queryDoh(dohBaseUrl: String, host: String): List<InetAddress>? {
        var conn: HttpURLConnection? = null
        return try {
            val sep = if (dohBaseUrl.contains('?')) '&' else '?'
            val encodedHost = URLEncoder.encode(host, "UTF-8")
            val targetUrl = "$dohBaseUrl${sep}name=$encodedHost&type=A"

            conn = (URL(targetUrl).openConnection(Proxy.NO_PROXY) as HttpURLConnection).apply {
                connectTimeout = 3_500
                readTimeout = 3_500
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/dns-json")
                setRequestProperty("User-Agent", "Viaura/1.0")
            }

            if (conn.responseCode != 200) return null

            val text = conn.inputStream.use { ins ->
                val bos = ByteArrayOutputStream()
                val buf = ByteArray(4096)
                var r: Int
                while (ins.read(buf).also { r = it } > 0) {
                    bos.write(buf, 0, r)
                }
                bos.toString("UTF-8")
            }

            val addresses = ArrayList<InetAddress>()
            val root = JSONObject(text)
            val answers = root.optJSONArray("Answer") ?: return null

            for (i in 0 until answers.length()) {
                val obj = answers.optJSONObject(i) ?: continue
                val type = obj.optInt("type", -1)
                // Type 1 = A record (IPv4), Type 28 = AAAA (IPv6)
                if (type == 1 || type == 28) {
                    val data = obj.optString("data", "").trim()
                    if (data.isNotEmpty()) {
                        try {
                            addresses.add(InetAddress.getByName(data))
                        } catch (_: Throwable) {}
                    }
                }
            }
            if (addresses.isNotEmpty()) addresses else null
        } catch (_: IOException) {
            null
        } catch (_: Throwable) {
            null
        } finally {
            try { conn?.disconnect() } catch (_: Throwable) {}
        }
    }

    private fun isIpLiteral(host: String): Boolean =
        host.contains(':') || host.all { it.isDigit() || it == '.' }

    private fun isLocalHost(host: String): Boolean {
        if (host == "localhost" || host.endsWith(".local")) return true
        if (host.startsWith("127.") || host.startsWith("10.") || host.startsWith("192.168.") || host == "::1") return true
        if (host.startsWith("172.")) {
            val second = host.split('.').getOrNull(1)?.toIntOrNull()
            if (second != null && second in 16..31) return true
        }
        return false
    }
}
