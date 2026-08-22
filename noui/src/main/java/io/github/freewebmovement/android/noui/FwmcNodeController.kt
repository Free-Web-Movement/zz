package io.github.freewebmovement.android.noui

import rs.zz.coin.FwmcNode

/**
 * Controls the embedded fwmc node lifecycle.
 *
 * The node runs on the phone and serves its unified HTTP/WS/Web-UI on [port].
 * The Web UI is reachable at `http://127.0.0.1:<port>/` on the device and via
 * the LAN IPs for desktop access.
 */
class FwmcNodeController(private var dataDir: String) {
    private var node: FwmcNode? = null

    val isRunning: Boolean
        get() = node?.running() ?: false

    val port: Int
        get() = node?.port() ?: 0

    val address: String
        get() = node?.address() ?: ""

    /** Base URL of the node's web UI on the device. */
    val webUrl: String
        get() = if (port == 0) "" else "http://127.0.0.1:$port/"

    /**
     * Start the node in the background.
     *
     * @param port 0 to let the OS assign a random free port (1025-65535 range
     *             persisted in settings); a fixed port otherwise.
     */
    fun start(port: Int): Int {
        if (node?.running() == true) return node!!.port()
        val n = FwmcNode()
        if (!n.startNode(port, dataDir)) return 0
        node = n
        return n.port()
    }

    fun stop() {
        node?.destroy()
        node = null
    }

    fun destroy() {
        stop()
    }
}
