package io.github.freewebmovement.android.noui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.russhwolf.settings.Settings
import io.github.freewebmovement.fwmc.R
import io.github.freewebmovement.peer.system.KVSettings
import io.github.freewebmovement.peer.system.Preference
import io.github.freewebmovement.peer.system.TrojanSetting
import org.json.JSONArray
import org.json.JSONObject
import rs.zz.coin.FwmcTrojan
import tun2socks.Tun2socks
import tun2socks.Tun2socksStartOptions

/**
 * Foreground VPN service that runs the embedded trojan client and routes
 * device traffic through it with go-tun2socks.
 *
 * Traffic flow (non-Clash mode, matching igniter):
 *
 * ```
 * app traffic -> tun0 -> go-tun2socks -> 127.0.0.1:<localPort>
 *   -> trojan client (TLS) -> remote trojan server -> internet
 * ```
 *
 * The app itself is excluded from the tunnel so the fwmc node and the
 * trojan outbound connection do not loop back into the VPN.
 */
class TrojanService : VpnService() {

    @Volatile
    private var worker: Thread? = null

    private var vpnInterface: ParcelFileDescriptor? = null
    private var tun2socksStarted = false
    private var gateway: TrojanGateway? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        TrojanStatusNotifier.ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopClient()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForegroundCompat(
                    TrojanStatusNotifier.build(
                        this,
                        running = true,
                        text = "Trojan 服务启动中…",
                    )
                )
                startClientAsync()
            }
        }
        return START_STICKY
    }

    override fun onRevoke() {
        stopClient()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopClient()
        TrojanStatusNotifier.showStopped(this)
        super.onDestroy()
    }

    private fun startClientAsync() {
        if (worker?.isAlive == true) return
        worker = Thread({ startClient() }, "trojan-service-start").apply { start() }
    }

    private fun startClient() {
        val setting = KVSettings(Preference(Settings())).trojan
        val server = setting.currentServer()
        if (server == null || server.remoteAddr.isBlank() || server.password.isBlank()) {
            running = false
            notifyText("Trojan 未配置服务器")
            stopSelf()
            return
        }

        val config = JSONObject().apply {
            put("run_type", "client")
            put("local_addr", server.localAddr.ifBlank { TrojanSetting.DEFAULT_LOCAL_ADDR })
            put("local_port", server.localPort)
            put("remote_addr", server.remoteAddr)
            put("remote_port", server.remotePort)
            put("password", JSONArray().put(server.password))
            put("log_level", 1)
            put("ssl", JSONObject().apply {
                put("verify", server.verifyCert)
                put("verify_hostname", server.verifyCert)
                put("sni", server.sni)
            })
        }

        val result = runCatching {
            JSONObject(FwmcTrojan.start(config.toString()))
        }.getOrElse {
            JSONObject().put("success", false).put("error", it.message ?: "start failed")
        }

        if (!result.optBoolean("success", false)) {
            val error = result.optString("error").ifBlank { "unknown error" }
            running = false
            notifyText("Trojan 启动失败：$error")
            stopSelf()
            return
        }

        val port = result.optInt("local_port", server.localPort)
        val gateway = TrojanGateway(
            trojanPort = port,
            bypassLan = setting.bypassLan,
            cnDirect = setting.cnDirectEnabled,
            directDomains = setting.loadDirectDomains(),
            proxyDomains = setting.loadProxyDomains(),
            blockedDomains = setting.loadBlockedDomains(),
            unreachableDomains = setting.loadUnreachableDomains(),
        )
        val gatewayPort = runCatching { gateway.start() }.getOrElse {
            runCatching { FwmcTrojan.stop() }
            running = false
            notifyText("Trojan 分流网关启动失败")
            stopSelf()
            return
        }
        this.gateway = gateway
        if (!establishVpn(gatewayPort)) {
            gateway.stop()
            this.gateway = null
            runCatching { FwmcTrojan.stop() }
            running = false
            notifyText("Trojan VPN 启动失败")
            stopSelf()
            return
        }

        running = true
        startedAt = System.currentTimeMillis()
        notifyText("Trojan 运行中 · SOCKS5 127.0.0.1:$port")
    }

    /** Build the tun interface and hand its fd to go-tun2socks. */
    private fun establishVpn(socksPort: Int): Boolean {
        return try {
            val setting = KVSettings(Preference(Settings())).trojan
            val builder = Builder()
            builder.setSession("zz Trojan")
            builder.setMtu(VPN_MTU)
            builder.addAddress(VPN_IPV4, 30)

            if (setting.bypassLan) {
                // Route only public IPv4 ranges so LAN/private destinations
                // bypass the VPN (same approach as igniter's Clash mode).
                TROJAN_PUBLIC_IPV4_ROUTES.forEach { route ->
                    val parts = route.split("/", limit = 2)
                    if (parts.size == 2) {
                        runCatching { builder.addRoute(parts[0], parts[1].toInt()) }
                    }
                }
            } else {
                builder.addRoute("0.0.0.0", 0)
            }

            if (setting.enableIpv6) {
                builder.addAddress(VPN_IPV6, 126)
                builder.addRoute("::", 0)
                VPN_DNS_SERVERS_V6.forEach { builder.addDnsServer(it) }
            }

            VPN_DNS_SERVERS.forEach { builder.addDnsServer(it) }

            // Always exclude zz itself to avoid a routing loop.
            try {
                builder.addDisallowedApplication(packageName)
            } catch (_: Exception) {
                // package lookup failure should not abort the VPN
            }

            // Per-app split tunneling: only selected apps use the proxy.
            if (!setting.proxyAllApps) {
                val proxied = setting.loadProxiedApps()
                packageManager.getInstalledApplications(0).forEach { app ->
                    val launchable = runCatching {
                        packageManager.getLaunchIntentForPackage(app.packageName) != null
                    }.getOrDefault(false)
                    if (launchable && app.packageName != packageName && app.packageName !in proxied) {
                        runCatching { builder.addDisallowedApplication(app.packageName) }
                    }
                }
            }

            val pfd = builder.establish() ?: return false
            vpnInterface = pfd

            val options = Tun2socksStartOptions().apply {
                tunFd = pfd.fd.toLong()
                socks5Server = "127.0.0.1:$socksPort"
                enableIPv6 = setting.enableIpv6
                mtu = VPN_MTU.toLong()
                fakeIPRange = ""
            }
            Tun2socks.setLoglevel("info")
            Tun2socks.start(options)
            tun2socksStarted = true
            vpnActive = true
            true
        } catch (e: Exception) {
            android.util.Log.e("TrojanService", "establishVpn failed", e)
            vpnInterface?.let { runCatching { it.close() } }
            vpnInterface = null
            false
        }
    }

    @Synchronized
    private fun stopClient() {
        if (worker?.isAlive == true) {
            worker?.join(3000)
        }
        worker = null
        val fd = vpnInterface
        vpnInterface = null
        if (tun2socksStarted) {
            // go-tun2socks owns/closes the tun fd. Detach it from the
            // ParcelFileDescriptor first so the PFD does not close the same
            // fd again (fdsan double-close on this Android build).
            runCatching { fd?.detachFd() }
            runCatching { Tun2socks.stop() }
            tun2socksStarted = false
        } else {
            fd?.let { runCatching { it.close() } }
        }
        vpnActive = false
        val g = gateway
        gateway = null
        g?.stop()
        if (running) {
            runCatching { FwmcTrojan.stop() }
            running = false
        }
        startedAt = 0L
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                TrojanStatusNotifier.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED,
            )
        } else {
            startForeground(TrojanStatusNotifier.NOTIFICATION_ID, notification)
        }
    }

    private fun notifyText(text: String) {
        TrojanStatusNotifier.showRunning(this, text)
    }

    companion object {
        const val ACTION_STOP = "io.github.freewebmovement.fwmc.TROJAN_STOP"
        const val ACTION_START = "io.github.freewebmovement.fwmc.TROJAN_START"

        private const val VPN_IPV4 = "172.19.0.1"
        private const val VPN_IPV6 = "fdfe:dcba:9876::1"
        private const val VPN_MTU = 1500
        private val VPN_DNS_SERVERS = arrayOf("223.5.5.5", "119.29.29.29", "114.114.114.114")
        private val VPN_DNS_SERVERS_V6 = arrayOf("2400:3200::1", "2400:3200:baba::1")

        @Volatile
        var running: Boolean = false
            private set

        /** Wall-clock start time of the running service, 0 when stopped. */
        @Volatile
        var startedAt: Long = 0L
            private set

        /** Whether the tun/tun2socks VPN interface is up. */
        @Volatile
        var vpnActive: Boolean = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, TrojanService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, TrojanService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
