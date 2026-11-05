package io.github.freewebmovement.android.noui

import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.Locale
import kotlin.concurrent.thread

/**
 * Minimal SOCKS5 split-routing gateway.
 *
 * It sits between go-tun2socks and the local trojan client:
 *
 * ```
 * tun2socks -> 127.0.0.1:<gatewayPort> -> DIRECT (rules) or
 *                                        -> 127.0.0.1:<trojanPort> (proxy)
 * ```
 *
 * TCP destinations matching the direct rules (private/LAN when enabled,
 * mainland-China domains, user domains) connect directly; everything else is
 * relayed through the trojan client. UDP associations are always relayed
 * through the trojan client, so DNS keeps working.
 */
internal class TrojanGateway(
    private val trojanPort: Int,
    private val bypassLan: Boolean,
    private val cnDirect: Boolean,
    private val directDomains: Set<String>,
    private val proxyDomains: Set<String>,
    private val blockedDomains: Set<String>,
    private val unreachableDomains: Set<String>,
) {
    private enum class Decision { DIRECT, PROXY, BLOCK }

    private companion object {
        const val SOCKS5 = 0x05
        const val ATYP_IPV4 = 0x01
        const val ATYP_DOMAIN = 0x03
        const val ATYP_IPV6 = 0x04
        const val CMD_CONNECT = 0x01
        const val CMD_UDP_ASSOCIATE = 0x03
        const val REP_SUCCEEDED = 0x00
        const val REP_FAILURE = 0x01
        const val REP_UNSUPPORTED = 0x07
        const val CONNECT_TIMEOUT_MS = 10_000
    }

    @Volatile
    private var running = false
    private var serverSocket: ServerSocket? = null

    fun start(): Int {
        val server = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
        server.soTimeout = 500
        serverSocket = server
        running = true
        thread(isDaemon = true, name = "trojan-gateway-accept") {
            while (running) {
                val client = try {
                    server.accept()
                } catch (_: java.net.SocketTimeoutException) {
                    continue
                } catch (_: Exception) {
                    break
                }
                thread(isDaemon = true, name = "trojan-gateway-client") {
                    handleClient(client)
                }
            }
        }
        return server.localPort
    }

    @Synchronized
    fun stop() {
        running = false
        // Do not close the listening socket here: on this Android build the
        // ServerSocket fd can be recycled by the VPN ParcelFileDescriptor,
        // and closing it again triggers fdsan SIGABRT. The accept loop exits
        // on the next 500ms timeout; the socket is reclaimed by GC/process exit.
    }

    private fun handleClient(client: Socket) {
        client.use { socket ->
            try {
                val inp = socket.getInputStream()
                val out = socket.getOutputStream()

                val greeting = ByteArray(2)
                readFully(inp, greeting)
                if (greeting[0].toInt() and 0xFF != SOCKS5) return
                val methodCount = greeting[1].toInt() and 0xFF
                readFully(inp, ByteArray(methodCount))
                out.write(byteArrayOf(SOCKS5.toByte(), 0))
                out.flush()

                val head = ByteArray(4)
                readFully(inp, head)
                if (head[0].toInt() and 0xFF != SOCKS5) return
                val cmd = head[1].toInt() and 0xFF
                val atyp = head[3].toInt() and 0xFF
                val host = readHost(inp, atyp)
                val port = readPort(inp)

                when (cmd) {
                    CMD_CONNECT -> handleConnect(socket, inp, out, host, port)
                    CMD_UDP_ASSOCIATE -> handleUdpAssociate(socket, inp, out)
                    else -> writeReply(out, REP_UNSUPPORTED, "0.0.0.0", 0)
                }
            } catch (_: Exception) {
                // connection closed / malformed request
            }
        }
    }

    private fun handleConnect(
        client: Socket,
        inp: InputStream,
        out: OutputStream,
        host: String,
        port: Int,
    ) {
        when (decide(host)) {
            Decision.BLOCK -> {
                writeReply(out, REP_FAILURE, "0.0.0.0", 0)
            }
            Decision.DIRECT -> {
                val target = runCatching {
                    Socket().also { it.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS) }
                }.getOrNull()
                if (target == null) {
                    writeReply(out, REP_FAILURE, "0.0.0.0", 0)
                    return
                }
                writeReply(out, REP_SUCCEEDED, "0.0.0.0", 0)
                relay(client, target)
            }
            Decision.PROXY -> {
                val target = connectViaTrojan(host, port)
                if (target == null) {
                    writeReply(out, REP_FAILURE, "0.0.0.0", 0)
                    return
                }
                writeReply(out, REP_SUCCEEDED, "0.0.0.0", 0)
                relay(client, target)
            }
        }
    }

    private fun connectViaTrojan(host: String, port: Int): Socket? = runCatching {
        val socket = Socket()
        socket.connect(InetSocketAddress("127.0.0.1", trojanPort), CONNECT_TIMEOUT_MS)
        val inp = socket.getInputStream()
        val out = socket.getOutputStream()
        out.write(byteArrayOf(SOCKS5.toByte(), 1, 0))
        out.flush()
        val greeting = ByteArray(2)
        readFully(inp, greeting)
        if (greeting[0].toInt() and 0xFF != SOCKS5 || greeting[1].toInt() != 0) {
            socket.close()
            return null
        }
        writeRequest(out, CMD_CONNECT, host, port)
        out.flush()
        val reply = readReply(inp)
        if (!reply.success) {
            socket.close()
            return null
        }
        socket
    }.getOrNull()

    private fun handleUdpAssociate(
        client: Socket,
        clientIn: InputStream,
        out: OutputStream,
    ) {
        val control = runCatching {
            Socket().also {
                it.connect(InetSocketAddress("127.0.0.1", trojanPort), CONNECT_TIMEOUT_MS)
            }
        }.getOrNull()
        if (control == null) {
            writeReply(out, REP_FAILURE, "0.0.0.0", 0)
            return
        }

        try {
            val tin = control.getInputStream()
            val tout = control.getOutputStream()
            tout.write(byteArrayOf(SOCKS5.toByte(), 1, 0))
            tout.flush()
            val greeting = ByteArray(2)
            readFully(tin, greeting)
            if (greeting[0].toInt() and 0xFF != SOCKS5 || greeting[1].toInt() != 0) {
                writeReply(out, REP_FAILURE, "0.0.0.0", 0)
                return
            }
            writeRequest(tout, CMD_UDP_ASSOCIATE, "0.0.0.0", 0)
            tout.flush()
            val reply = readReply(tin)
            if (!reply.success || reply.port <= 0) {
                writeReply(out, REP_FAILURE, "0.0.0.0", 0)
                return
            }

            val relayHost = when (reply.host) {
                "0.0.0.0", "::", "" -> "127.0.0.1"
                else -> reply.host
            }
            val relayAddr = InetSocketAddress(InetAddress.getByName(relayHost), reply.port)
            val udp = DatagramSocket(0, InetAddress.getByName("127.0.0.1"))
            writeReply(out, REP_SUCCEEDED, "127.0.0.1", udp.localPort)

            // Close the UDP relay when the client closes the TCP control channel.
            thread(isDaemon = true, name = "trojan-gateway-udp-control") {
                runCatching { clientIn.read() }
                runCatching { udp.close() }
            }

            var clientAddr: InetSocketAddress? = null
            val buffer = ByteArray(65535)
            try {
                while (!udp.isClosed) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    udp.receive(packet)
                    val from = packet.socketAddress as? InetSocketAddress ?: continue
                    val fromRelay = from.address == relayAddr.address && from.port == relayAddr.port
                    if (fromRelay && clientAddr != null) {
                        udp.send(DatagramPacket(packet.data, packet.length, clientAddr))
                    } else {
                        clientAddr = from
                        udp.send(DatagramPacket(packet.data, packet.length, relayAddr))
                    }
                }
            } catch (_: Exception) {
                // socket closed
            } finally {
                runCatching { udp.close() }
            }
        } finally {
            runCatching { control.close() }
        }
    }

    private fun decide(host: String): Decision {
        val h = host.lowercase(Locale.US)
        if (matches(h, unreachableDomains)) return Decision.BLOCK
        if (matches(h, blockedDomains)) return Decision.BLOCK
        if (matches(h, proxyDomains)) return Decision.PROXY
        if (bypassLan && isIpLiteral(h) && isPrivateAddress(h)) return Decision.DIRECT
        if (cnDirect && matches(h, TROJAN_CN_DIRECT_DOMAINS)) return Decision.DIRECT
        if (directDomains.isNotEmpty() && matches(h, directDomains)) return Decision.DIRECT
        return Decision.PROXY
    }

    private fun matches(host: String, rules: Collection<String>): Boolean =
        rules.any { rule ->
            val r = rule.trim().lowercase(Locale.US)
            r.isNotEmpty() && (host == r || host.endsWith(".$r"))
        }

    private fun isIpLiteral(host: String): Boolean =
        host.contains(':') || host.matches(Regex("^\\d{1,3}(\\.\\d{1,3}){3}$"))

    private fun isPrivateAddress(host: String): Boolean {
        val addr = runCatching { InetAddress.getByName(host) }.getOrNull() ?: return false
        if (addr.isAnyLocalAddress || addr.isLoopbackAddress ||
            addr.isLinkLocalAddress || addr.isSiteLocalAddress
        ) return true
        val bytes = addr.address
        return bytes.size == 16 && (bytes[0].toInt() and 0xFE) == 0xFC
    }

    private fun relay(client: Socket, target: Socket) {
        val up = thread(isDaemon = true, name = "trojan-gateway-up") { copy(client, target) }
        val down = thread(isDaemon = true, name = "trojan-gateway-down") { copy(target, client) }
        up.join()
        down.join()
        runCatching { client.close() }
        runCatching { target.close() }
    }

    private fun copy(from: Socket, to: Socket) {
        runCatching {
            from.getInputStream().copyTo(to.getOutputStream())
        }
        runCatching { to.shutdownOutput() }
    }

    private data class SocksReply(val success: Boolean, val host: String, val port: Int)

    private fun readReply(inp: InputStream): SocksReply {
        val head = ByteArray(4)
        readFully(inp, head)
        if (head[0].toInt() and 0xFF != SOCKS5) return SocksReply(false, "", 0)
        val rep = head[1].toInt() and 0xFF
        val atyp = head[3].toInt() and 0xFF
        val host = readHost(inp, atyp)
        val port = readPort(inp)
        return SocksReply(rep == REP_SUCCEEDED, host, port)
    }

    private fun readHost(inp: InputStream, atyp: Int): String = when (atyp) {
        ATYP_IPV4 -> {
            val bytes = ByteArray(4)
            readFully(inp, bytes)
            InetAddress.getByAddress(bytes).hostAddress
        }
        ATYP_IPV6 -> {
            val bytes = ByteArray(16)
            readFully(inp, bytes)
            InetAddress.getByAddress(bytes).hostAddress
        }
        ATYP_DOMAIN -> {
            val len = inp.read()
            if (len <= 0) throw EOFException()
            val bytes = ByteArray(len)
            readFully(inp, bytes)
            String(bytes, Charsets.UTF_8)
        }
        else -> throw IllegalArgumentException("unsupported ATYP $atyp")
    }

    private fun readPort(inp: InputStream): Int {
        val hi = inp.read()
        val lo = inp.read()
        if (hi < 0 || lo < 0) throw EOFException()
        return (hi shl 8) or lo
    }

    private fun readFully(inp: InputStream, buffer: ByteArray) {
        var offset = 0
        while (offset < buffer.size) {
            val n = inp.read(buffer, offset, buffer.size - offset)
            if (n < 0) throw EOFException()
            offset += n
        }
    }

    private fun writeRequest(out: OutputStream, cmd: Int, host: String, port: Int) {
        out.write(byteArrayOf(SOCKS5.toByte(), cmd.toByte(), 0))
        val ip = parseIp(host)
        if (ip != null) {
            if (ip.size == 4) {
                out.write(ATYP_IPV4)
            } else {
                out.write(ATYP_IPV6)
            }
            out.write(ip)
        } else {
            val bytes = host.toByteArray(Charsets.UTF_8)
            out.write(ATYP_DOMAIN)
            out.write(bytes.size)
            out.write(bytes)
        }
        out.write((port shr 8) and 0xFF)
        out.write(port and 0xFF)
    }

    private fun writeReply(out: OutputStream, rep: Int, host: String, port: Int) {
        out.write(byteArrayOf(SOCKS5.toByte(), rep.toByte(), 0, ATYP_IPV4.toByte()))
        val ip = parseIp(host) ?: InetAddress.getByName("0.0.0.0").address
        out.write(if (ip.size == 4) ip else InetAddress.getByName("0.0.0.0").address)
        out.write((port shr 8) and 0xFF)
        out.write(port and 0xFF)
        out.flush()
    }

    private fun parseIp(host: String): ByteArray? {
        if (!isIpLiteral(host)) return null
        return runCatching { InetAddress.getByName(host).address }.getOrNull()
    }
}
