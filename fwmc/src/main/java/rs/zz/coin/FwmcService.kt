package rs.zz.coin

/**
 * Unified service JNI bridge.
 *
 * Both server-side managed services (e.g. trojan server) and client-side
 * external-app descriptors are exposed through this single interface.
 * Weight/metering fields are reserved in responses for future accounting.
 */
object FwmcService {
    init {
        System.loadLibrary("fwmc")
    }

    /** List known service descriptors. */
    external fun list(): String

    /** Query named service status/endpoint. */
    external fun status(name: String): String

    /**
     * Start a named service.
     * For managed services, pass configJson; for external-app services pass "".
     */
    external fun start(name: String, configJson: String): String

    /** Stop a named service. */
    external fun stop(name: String): String
}
