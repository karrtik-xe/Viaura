package com.example.util.network

import android.content.Context
import com.example.util.NetworkBypass
import okhttp3.ConnectionPool
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.io.IOException
import java.net.ConnectException
import java.net.Proxy
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Single shared OkHttp client for Viaura:
 * - Shared connection pool (64 connections, 5 minutes idle TTL)
 * - HTTP/2 and HTTP/1.1 support with keep-alive
 * - Retry on connection failure enabled
 * - Fast, reliable timeouts (10s connect, 20s read/write)
 * - Backed by ViauraDns (DNS-over-HTTPS with fallback)
 * - Easily re-creates connection pool upon Wi-Fi / Mobile network transitions
 */
object ViauraHttpClient {
    const val DEFAULT_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    /**
     * When a GET / HEAD cannot connect (or the name does not resolve), the host's DNS answer is dropped, the next DNS
     * provider is asked first, and the request is repeated once. Skipped when the call was cancelled (server race).
     */
    private val dnsFailoverInterceptor = Interceptor { chain ->
        val request = chain.request()
        try {
            chain.proceed(request)
        } catch (e: IOException) {
            val connectFailure = e is ConnectException || e is SocketTimeoutException || e is UnknownHostException
            val idempotent = request.method == "GET" || request.method == "HEAD"
            if (connectFailure && idempotent && !chain.call().isCanceled()) {
                ViauraDns.reportConnectFailure(request.url.host)
                chain.proceed(request)
            } else {
                throw e
            }
        }
    }

    @Volatile
    private var connectionPool = ConnectionPool(64, 5, TimeUnit.MINUTES)

    @Volatile
    private var baseClient: OkHttpClient = buildBaseClient()

    private fun buildBaseClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .connectionPool(connectionPool)
            .dns(ViauraDns)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(dnsFailoverInterceptor)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /** Returns the primary shared OkHttpClient instance. */
    fun get(): OkHttpClient = baseClient

    /**
     * Resets the connection pool and client when the device switches networks (Wi-Fi <-> Cellular).
     */
    fun onNetworkChanged() {
        try {
            connectionPool.evictAll()
        } catch (_: Throwable) {}
        connectionPool = ConnectionPool(64, 5, TimeUnit.MINUTES)
        baseClient = buildBaseClient()
        ViauraDns.clearCache()
    }

    /**
     * Returns an OkHttpClient routed through the local AntiDPI proxy when active, or direct.
     */
    fun forContext(context: Context): OkHttpClient {
        ViauraDns.init(context)
        val proxy = try { NetworkBypass.okHttpProxy(context) } catch (_: Throwable) { null }
        return if (proxy != null && proxy.type() != Proxy.Type.DIRECT) {
            baseClient.newBuilder()
                .proxy(proxy)
                .build()
        } else {
            baseClient
        }
    }
}
