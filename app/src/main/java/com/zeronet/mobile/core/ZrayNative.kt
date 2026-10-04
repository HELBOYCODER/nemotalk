package com.zeronet.mobile.core

/**
 * Minimal JNI bridge to ZeroNet's `zray-mobile` core (libzray_mobile.so).
 *
 * The core is a self-contained Rust engine shipped as a single stripped ELF
 * shared library (~13 MB). It exposes a SOCKS5 + HTTP-CONNECT inbound on
 * 127.0.0.1, so NeMoTalk can route only its own traffic through it while the
 * rest of the system stays untouched.
 *
 * We deliberately call a tiny subset of the full JNI surface — the parts
 * needed to start/stop the proxy and to scan & test free server links:
 *  - [init] / [start] / [stop] / [isRunning]
 *  - [scan] (IP/port scan of free servers) / [testLinks]
 *
 * Native method names are fixed by the library's exported symbols
 * (Java_com_zeronet_mobile_core_ZrayNative_*), so the class *must* live at
 * package `com.zeronet.mobile.core`. We keep it there and expose the surface
 * through the friendlier [ZeroProxyEngine] wrapper.
 */

fun interface NativeListener {
    /** Delivered on a background thread: one JSON-object-per-line event batch. */
    fun onEvents(batch: String)
}

object ZrayNative {
    @JvmStatic external fun init(dataDir: String, logLevel: String): String?
    @JvmStatic external fun setLogLevel(level: String): String?

    @JvmStatic external fun start(configJson: String): String?
    @JvmStatic external fun reload(configJson: String): String?
    @JvmStatic external fun stop(): String?
    @JvmStatic external fun isRunning(): Boolean
    @JvmStatic external fun stats(): String?
    @JvmStatic external fun networkChanged(): String?

    /** Resolve a config request JSON into the engine's full runtime config. */
    @JvmStatic external fun buildConfig(requestJson: String): String

    /** Scan candidate servers. Events arrive on [listener]; returns a job handle. */
    @JvmStatic external fun scan(requestJson: String, listener: NativeListener): Long

    /** Test a list of links for reachability through the current setup. */
    @JvmStatic external fun testLinks(requestJson: String, listener: NativeListener): Long

    /** Register a free WARP account and probe for working servers. */
    @JvmStatic external fun warpRegister(requestJson: String, listener: NativeListener): Long

    @JvmStatic external fun cancel(handle: Long)

    /**
     * Called from Rust for every outbound socket the engine opens. Without a
     * VpnService tunnel there is nothing to escape from, so every fd is fine.
     */
    @JvmStatic
    fun protect(fd: Int): Boolean = true
}
