package com.helboy.nemotalk.network

import com.helboy.nemotalk.data.PreferencesManager
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Route
import java.net.InetSocketAddress
import java.net.Proxy

/**
 * In-app proxy support for NeMoTalk.
 *
 * ZeroNet/Zray (or any local SOCKS5/HTTP proxy) is reached at 127.0.0.1 and is
 * applied ONLY to NeMoTalk's own OkHttp clients. Nothing else on the device is
 * touched — no VpnService, no system-wide proxy setting. When the proxy toggle
 * is off (or its address is blank), clients are built unproxied as before.
 *
 * ponytail: a fresh builder per client rather than a shared singleton — each
 * client owns its own timeouts, and rebuilds are cheap relative to network I/O.
 */
object ProxySupport {

    private const val DEFAULT_HOST = "127.0.0.1"
    private const val DEFAULT_SOCKS_PORT = 10808
    private const val DEFAULT_HTTP_PORT = 10809

    /** True only when the user has turned the in-app proxy on AND given an address. */
    fun isEnabled(prefs: PreferencesManager): Boolean =
        prefs.proxyEnabled && prefs.proxyAddress.isNotBlank()

    /**
     * The SOCKS5 endpoint for proxied traffic (Zray's default inbound).
     * Falls back to the Zray defaults when the user left fields empty.
     */
    fun socksEndpoint(prefs: PreferencesManager): String {
        val host = prefs.proxyAddress.ifBlank { DEFAULT_HOST }
        val port = prefs.proxyPort.ifBlank { DEFAULT_SOCKS_PORT.toString() }
        return "$host:$port"
    }

    /** The HTTP CONNECT endpoint, for clients that prefer it over SOCKS5. */
    fun httpEndpoint(prefs: PreferencesManager): String {
        val host = prefs.proxyAddress.ifBlank { DEFAULT_HOST }
        val port = prefs.proxyHttpPort.ifBlank { DEFAULT_HTTP_PORT.toString() }
        return "$host:$port"
    }

    private fun parsePort(raw: String, default: Int): Int =
        raw.trim().toIntOrNull()?.coerceIn(1, 65535) ?: default

    /**
     * Apply the proxy to an OkHttp builder. Returns the same builder so callers
     * can keep their timeout/auth chain fluent.
     */
    fun applyTo(builder: OkHttpClient.Builder, prefs: PreferencesManager): OkHttpClient.Builder {
        if (!isEnabled(prefs)) return builder

        val host = prefs.proxyAddress.ifBlank { DEFAULT_HOST }
        val type = prefs.proxyType
        val (port, auth) = when (type) {
            PreferencesManager.PROXY_TYPE_HTTP -> {
                val p = parsePort(prefs.proxyHttpPort, DEFAULT_HTTP_PORT)
                p to true
            }
            else -> {
                // SOCKS5
                val p = parsePort(prefs.proxyPort, DEFAULT_SOCKS_PORT)
                p to false
            }
        }

        builder.proxy(java.net.Proxy(type, InetSocketAddress(host, port)))

        if (auth) {
            // HTTP CONNECT basic auth (SOCKS5 user/pass is handled by the JVM
            // through java.net's authenticator only for java.net.Socket — OkHttp
            // does not attach credentials to SOCKS, so we keep auth HTTP-only).
            builder.proxyAuthenticator(BasicProxyAuthenticator(prefs.proxyUsername, prefs.proxyPassword))
        }

        return builder
    }

    private class BasicProxyAuthenticator(
        private val username: String,
        private val password: String
    ) : Authenticator {
        override fun authenticate(route: Route?, response: okhttp3.Response): Request? {
            if (response.code == 407) {
                val credential = Credentials.basic(username, password)
                return response.request.newBuilder()
                    .header("Proxy-Authorization", credential)
                    .build()
            }
            return null
        }
    }
}
