package rs.zz.coin

/**
 * JNI wrapper for the fwmc Rust node.
 *
 * Starts a full fwmc node (P2P + unified HTTP/WS/Web-UI server) in a background
 * thread. After [startNode], the node's Web UI is reachable at
 * `http://<ip>:<port>/` on the device and on the LAN.
 */
class FwmcNode {
    var id: Long = 0
        private set

    /** Raw JNI entry; returns the native handle (0 on failure). */
    external fun start(port: Int, dataDir: String): Long

    external fun getAddress(ptr: Long): String
    external fun getPort(ptr: Long): Int
    external fun isRunning(ptr: Long): Boolean
    external fun stop(ptr: Long)

    /**
     * Start the node and keep its handle. Returns false when the native side
     * refused to start (handle == 0).
     */
    fun startNode(port: Int, dataDir: String): Boolean {
        id = start(port, dataDir)
        return id != 0L
    }

    fun address(): String = getAddress(id)
    fun port(): Int = getPort(id)
    fun running(): Boolean = isRunning(id)

    /** Ask the node to stop and release its handle. */
    fun destroy() {
        if (id != 0L) {
            stop(id)
            id = 0L
        }
    }

    companion object {
        init {
            System.loadLibrary("fwmc")
        }
    }
}
