package io.github.freewebmovement.android.noui

import android.content.Context
import android.util.Log
import clash.Clash
import clash.ClashStartOptions
import io.github.freewebmovement.fwmc.R
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

/**
 * Manages the bundled Clash core used for rule-based split routing.
 *
 * The Clash library reads `<homeDir>/config.yaml` and needs `<homeDir>/Country.mmdb`
 * for GEOIP rules. Both are seeded from the app resources on first use; the
 * config is rewritten on every start so the SOCKS listener and the upstream
 * trojan proxy port always match the ports chosen for this session.
 */
internal class ClashConfig(private val context: Context) {

    companion object {
        private const val TAG = "ClashConfig"
        private const val CONFIG_NAME = "config.yaml"
        private const val MMDB_NAME = "Country.mmdb"
        private const val LISTEN_HOST = "127.0.0.1"
        private val DEFAULT_DNS_SERVERS = listOf("223.5.5.5", "119.29.29.29", "114.114.114.114")

        /** Returns [preferred] when it is free, otherwise a random free TCP port. */
        fun findFreePort(preferred: Int): Int {
            if (preferred in 1..65535 && !isPortTaken(preferred)) return preferred
            return findFreePort()
        }

        /** Returns a random free TCP port. */
        fun findFreePort(): Int {
            repeat(20) {
                val port = runCatching {
                    ServerSocket(0, 1, InetAddress.getByName(LISTEN_HOST)).use { it.localPort }
                }.getOrNull() ?: return@repeat
                if (!isPortTaken(port)) return port
            }
            return 0
        }

        fun isPortTaken(port: Int): Boolean = try {
            Socket().use { it.connect(InetSocketAddress(LISTEN_HOST, port), 120) }
            true
        } catch (_: IOException) {
            false
        }

        fun waitForPort(port: Int, timeoutMs: Long): Boolean {
            val deadline = System.currentTimeMillis() + timeoutMs
            while (System.currentTimeMillis() < deadline) {
                if (isPortTaken(port)) return true
                runCatching { Thread.sleep(100) }
            }
            return false
        }
    }

    @Volatile
    private var started = false

    /**
     * Writes the Clash config (with the given ports) and the GeoIP database to
     * [homeDir], creating the directory when needed.
     */
    fun prepare(
        homeDir: File,
        socksPort: Int,
        trojanPort: Int,
        allowLan: Boolean = false,
        dnsServers: List<String> = emptyList(),
    ): File {
        if (!homeDir.exists()) homeDir.mkdirs()
        copyIfMissing(homeDir, MMDB_NAME, R.raw.country)
        val template = context.resources.openRawResource(R.raw.clash_config)
            .bufferedReader()
            .use { it.readText() }
        val file = File(homeDir, CONFIG_NAME)
        file.writeText(rewriteConfig(template, socksPort, trojanPort, allowLan, dnsServers))
        return file
    }

    /** Starts Clash and waits until its SOCKS listener is reachable. */
    fun start(
        homeDir: File,
        socksPort: Int,
        trojanPort: Int,
        allowLan: Boolean = false,
    ): Boolean {
        val options = ClashStartOptions()
        options.homeDir = homeDir.absolutePath
        options.socksListener = "${if (allowLan) "*" else LISTEN_HOST}:$socksPort"
        options.trojanProxyServer = "$LISTEN_HOST:$trojanPort"
        options.trojanProxyServerUdpEnabled = true
        runCatching { Clash.start(options) }.onFailure {
            Log.e(TAG, "Clash.start failed", it)
        }
        val up = waitForPort(socksPort, 5000)
        if (!up) Log.e(TAG, "Clash SOCKS listener $socksPort did not come up")
        started = up
        return up
    }

    fun stop() {
        if (!started) return
        started = false
        runCatching { Clash.stop() }.onFailure { Log.w(TAG, "Clash.stop failed", it) }
    }

    private fun rewriteConfig(
        text: String,
        socksPort: Int,
        trojanPort: Int,
        allowLan: Boolean,
        dnsServers: List<String>,
    ): String {
        val lines = text.split('\n').toMutableList()
        for (i in lines.indices) {
            val line = lines[i]
            when {
                line.trimStart().startsWith("socks-port:") -> lines[i] = "socks-port: $socksPort"
                line.trimStart().startsWith("allow-lan:") -> lines[i] = "allow-lan: $allowLan"
                line.contains("name: \"trojan\"") && line.contains("port:") ->
                    lines[i] = line.replace(Regex("port:\\s*\\d+"), "port: $trojanPort")
            }
        }
        rewriteDnsNameservers(lines, dnsServers)
        return lines.joinToString("\n")
    }

    /**
     * Prefers the active network's DNS servers so DIRECT rules still resolve on
     * networks that block the bundled public resolvers. The bundled public
     * resolvers stay as fallbacks.
     */
    private fun rewriteDnsNameservers(lines: MutableList<String>, dnsServers: List<String>) {
        val header = lines.indexOfFirst { it.trimStart().startsWith("nameserver:") }
        if (header < 0) return
        var end = header + 1
        while (end < lines.size && lines[end].trimStart().startsWith("- ")) end++
        val servers = (dnsServers + DEFAULT_DNS_SERVERS).distinct().filter { it.isNotBlank() }
        if (servers.isEmpty()) return
        lines.subList(header + 1, end).clear()
        lines.addAll(header + 1, servers.map { "    - $it" })
    }

    private fun copyIfMissing(homeDir: File, name: String, resId: Int) {
        val target = File(homeDir, name)
        if (target.exists() && target.length() > 0L) return
        context.resources.openRawResource(resId).use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
    }
}
