package io.github.freewebmovement.android.noui

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import com.russhwolf.settings.Settings
import io.github.freewebmovement.peer.system.KVSettings
import io.github.freewebmovement.peer.system.Preference
import io.github.freewebmovement.peer.system.TrojanSetting
import org.json.JSONArray
import org.json.JSONObject
import rs.zz.coin.FwmcTrojan
import tun2socks.Tun2socks
import tun2socks.Tun2socksStartOptions
import java.io.File
import java.net.Inet4Address
import java.net.Inet6Address

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
    private var clash: ClashConfig? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        SystemStatusNotifier.ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopClient()
                SystemStatusNotifier.setTrojan(this, false, "已停止")
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                SystemStatusNotifier.setTrojan(this, true, "启动中…")
                startForegroundCompat()
                startClientAsync()
            }
        }
        return START_STICKY
    }

    override fun onRevoke() {
        stopClient()
        SystemStatusNotifier.setTrojan(this, false, "已停止")
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    override fun onDestroy() {
        stopClient()
        SystemStatusNotifier.setTrojan(this, false, "已停止")
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
            SystemStatusNotifier.setTrojan(this, false, "未配置服务器")
            stopSelf()
            return
        }

        // In Clash mode the tun traffic lands on Clash's SOCKS listener, which
        // applies the bundled rule set (CN direct / foreign proxy), and dials
        // the local trojan client through its own listener port.
        val useClash = setting.clashEnabled
        val clashPort = if (useClash) ClashConfig.findFreePort(server.localPort) else 0
        val trojanPort = if (useClash) {
            val preferred = if (clashPort < 65535) clashPort + 1 else clashPort - 1
            ClashConfig.findFreePort(preferred)
        } else {
            server.localPort
        }

        val config = JSONObject().apply {
            put("run_type", "client")
            put("local_addr", server.localAddr.ifBlank { TrojanSetting.DEFAULT_LOCAL_ADDR })
            put("local_port", trojanPort)
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
            SystemStatusNotifier.setTrojan(this, false, "启动失败", error)
            stopSelf()
            return
        }

        val port = result.optInt("local_port", trojanPort)
        val socksPort = if (useClash) {
            val clashConfig = ClashConfig(this)
            val home = File(filesDir, "fwmc/node/clash")
            val prepared = runCatching {
                clashConfig.prepare(home, clashPort, port, setting.allowLan, activeDnsServers())
            }.onFailure {
                android.util.Log.e("TrojanService", "clash prepare failed", it)
            }.isSuccess
            if (!prepared || !clashConfig.start(home, clashPort, port, setting.allowLan)) {
                clashConfig.stop()
                runCatching { FwmcTrojan.stop() }
                running = false
                SystemStatusNotifier.setTrojan(this, false, "Clash 启动失败")
                stopSelf()
                return
            }
            this.clash = clashConfig
            clashPort
        } else {
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
                SystemStatusNotifier.setTrojan(this, false, "分流网关启动失败")
                stopSelf()
                return
            }
            this.gateway = gateway
            gatewayPort
        }
        if (!establishVpn(socksPort)) {
            gateway?.stop()
            gateway = null
            clash?.stop()
            clash = null
            runCatching { FwmcTrojan.stop() }
            running = false
            SystemStatusNotifier.setTrojan(this, false, "VPN 启动失败")
            stopSelf()
            return
        }

        running = true
        startedAt = System.currentTimeMillis()
        val label = if (useClash) "Clash 127.0.0.1:$clashPort" else "SOCKS5 127.0.0.1:$port"
        SystemStatusNotifier.setTrojan(this, true, "运行中", label)
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

            // tun2socks answers DNS locally with fake IPs. This makes domain
            // split-routing work (the gateway sees hostnames, not just IPs)
            // and removes the dependency on the remote server relaying UDP.
            builder.addRoute(FAKE_IP_ROUTE, FAKE_IP_PREFIX)

            if (setting.enableIpv6) {
                builder.addAddress(VPN_IPV6, 126)
                builder.addRoute("::", 0)
                VPN_DNS_SERVERS_V6.forEach { builder.addDnsServer(it) }
            }

            // Send every physical DNS server through the tunnel too, otherwise
            // queries can bypass the fake-IP interception via the LAN route.
            addPhysicalDnsRoutes(builder, setting.enableIpv6)

            builder.addDnsServer(FAKE_DNS_SERVER)

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
                fakeIPRange = FAKE_IP_RANGE
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

    /** DNS servers of the underlying (non-VPN) network, IPv4 first. */
    private fun activeDnsServers(): List<String> = runCatching {
        val cm = getSystemService(ConnectivityManager::class.java)
        val network = cm?.activeNetwork
        val lp = network?.let { cm.getLinkProperties(it) }
        lp?.dnsServers
            ?.sortedBy { if (it is Inet4Address) 0 else 1 }
            ?.mapNotNull { it.hostAddress?.substringBefore('%') }
            ?.filter { it.isNotBlank() }
            ?.distinct()
            .orEmpty()
    }.getOrDefault(emptyList())

    /** Route the underlying network's DNS servers into the tun device. */
    private fun addPhysicalDnsRoutes(builder: Builder, enableIpv6: Boolean) {
        runCatching {
            val cm = getSystemService(ConnectivityManager::class.java) ?: return
            val network = cm.activeNetwork ?: return
            val lp = cm.getLinkProperties(network) ?: return
            lp.dnsServers.forEach { dns ->
                when {
                    dns is Inet4Address -> builder.addRoute(dns.hostAddress, 32)
                    dns is Inet6Address && enableIpv6 -> builder.addRoute(dns.hostAddress, 128)
                }
            }
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
        val c = clash
        clash = null
        c?.stop()
        if (running) {
            runCatching { FwmcTrojan.stop() }
            running = false
        }
        startedAt = 0L
    }

    private fun startForegroundCompat() {
        val notification: Notification = SystemStatusNotifier.build(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                SystemStatusNotifier.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED,
            )
        } else {
            startForeground(SystemStatusNotifier.NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val ACTION_STOP = "io.github.freewebmovement.fwmc.TROJAN_STOP"
        const val ACTION_START = "io.github.freewebmovement.fwmc.TROJAN_START"

        private const val VPN_IPV4 = "172.19.0.1"
        private const val VPN_IPV6 = "fdfe:dcba:9876::1"
        private const val VPN_MTU = 1500
        private const val FAKE_IP_RANGE = "198.18.0.1/16"
        private const val FAKE_IP_ROUTE = "198.18.0.0"
        private const val FAKE_IP_PREFIX = 16
        private const val FAKE_DNS_SERVER = "198.18.0.1"
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
