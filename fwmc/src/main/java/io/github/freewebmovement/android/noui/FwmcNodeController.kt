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
     * @param tickMs 纪元运行模式：`>0` 测试模式（每 tick 的真实毫秒数），
     *               `<=0` 生产模式（真实 15 分钟/tick）。必须在节点启动前设置，
     *               因为 ticker 在节点启动时一次性选定。
     * @param startTick 测试模式下 TICK 的初始值（0..95）；达到 96 时发 EPOCH_NEW。
     *                  生产模式忽略。
     */
    fun start(port: Int, tickMs: Long = 0L, startTick: Long = 0L): Int {
        if (node?.running() == true) return node!!.port()
        // 时间仿真 / TICK 初始值必须在节点启动、创建 ticker 之前设置。
        rs.zz.coin.FwmcApi.setTimeSim(tickMs)
        rs.zz.coin.FwmcApi.setStartTick(startTick)
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
