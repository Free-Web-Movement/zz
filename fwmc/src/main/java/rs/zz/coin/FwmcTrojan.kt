package rs.zz.coin

/**
 * JNI wrapper for the embedded trojan service.
 *
 * The instance type (server / client / forward / nat) comes from the config's
 * `run_type` field; the wrapper does not force a role.
 */
object FwmcTrojan {
    init {
        System.loadLibrary("fwmc")
    }

    /** Start trojan with a JSON config; returns `{"success":true,...}`. */
    external fun start(configJson: String): String

    /** Stop the running instance; returns `{"success":true,"stopped":bool}`. */
    external fun stop(): String

    /** Query state; returns `{"success":true,"running":bool,...}`. */
    external fun getState(): String
}
