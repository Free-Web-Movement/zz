package io.github.freewebmovement.peer.system

import io.github.freewebmovement.peer.types.IPScopeType
import io.github.freewebmovement.peer.types.IPType
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface


class IPList private constructor() {
    companion object {
        var ipv4IPLocal = ArrayList<String>()
        var ipv4IPPublic = ArrayList<String>()
        var ipv6IPLocal = ArrayList<String>()
        var ipv6IPPublic = ArrayList<String>()

        init {
            try {
                val networkInterfaces = NetworkInterface.getNetworkInterfaces()
                while (networkInterfaces.hasMoreElements()) {
                    val enumIpAddresses = networkInterfaces.nextElement().inetAddresses
                    while (enumIpAddresses.hasMoreElements()) {
                        val inetAddress = enumIpAddresses.nextElement()
                        val address =
                            inetAddress.hostAddress?.toString()?.split("%")?.get(0).toString()
                        when (isPublic(inetAddress)) {
                            true -> {
                                when (inetAddress) {
                                    is Inet4Address -> ipv4IPPublic.add(address)
                                    is Inet6Address -> ipv6IPPublic.add(address)
                                }
                            }

                            false -> {
                                if (!inetAddress.isLoopbackAddress) {
                                    when (inetAddress) {
                                        is Inet4Address -> ipv4IPLocal.add(address)
                                        is Inet6Address -> ipv6IPLocal.add(address)
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("IP Address Error: \n$e")
            }
        }

        private fun isPublic(inetAddress: InetAddress): Boolean {
            return !inetAddress.isLoopbackAddress && !inetAddress.isSiteLocalAddress
                    && !inetAddress.isAnyLocalAddress && !inetAddress.isLinkLocalAddress &&
                    !inetAddress.isMCOrgLocal && !inetAddress.isMCNodeLocal &&
                    !inetAddress.isMCLinkLocal && !inetAddress.isMCSiteLocal
        }

        fun hasPublicIPs(): Boolean {
            return ipv4IPPublic.isNotEmpty() || ipv6IPPublic.isNotEmpty()
        }

        fun toHttpUrl(list: ArrayList<String>, port: Int, ipType: IPType): String {
            if (list.isEmpty()) {
                return ""
            }
            return when(ipType) {
                IPType.IPV4 -> {
                    "http://${list[0]}:$port"
                }

                IPType.IPV6 ->  {
                    "http://[${list[0]}]:$port"
                }
            }
        }

        fun toIP(list: ArrayList<String>): String {
            if (list.isNotEmpty()) {
                return list[0]
            }
            return ""
        }

        fun toScope(scope: IPScopeType, local: ArrayList<String>, pub: ArrayList<String>): String {
            return when (scope) {
                IPScopeType.LOCAL -> {
                    toIP(local)
                }

                IPScopeType.PUBLIC -> {
                    toIP(pub)
                }
            }
        }

        fun getUri(port: Int): String {
            var uri = toHttpUrl(ipv4IPLocal, port, IPType.IPV4)
            if (uri == "") {
                uri = toHttpUrl(ipv4IPPublic, port, IPType.IPV4)
            }
            if (uri == "") {
                uri = toHttpUrl(ipv6IPPublic, port, IPType.IPV6)
            }
            return uri
        }

        fun getPublicUri(port: Int): String {
            var uri = toHttpUrl(ipv6IPPublic, port, IPType.IPV6)
            if (uri == "") {
                uri = toHttpUrl(ipv4IPLocal, port, IPType.IPV4)
            }
            return uri
        }

        fun getLocalUri(port: Int): String {
            return toHttpUrl(ipv4IPLocal, port, IPType.IPV4)
        }

        fun getPublicType(): IPType {
            if (ipv6IPPublic.isNotEmpty()) {
                return IPType.IPV6
            }
            return IPType.IPV4
        }

        fun getIP(type: IPType, scope: IPScopeType): String {
            return when (type) {
                IPType.IPV4 -> {
                    toScope(scope, ipv4IPLocal, ipv4IPPublic)
                }
                IPType.IPV6 -> {
                    toScope(scope, ipv6IPLocal, ipv6IPPublic)
                }
            }
        }

        fun toHTTPV6Uris(ipv6s: List<String>, port: Int): List<String> {
            val uris = ArrayList<String>()
            ipv6s.forEach {
                uris.add("http://[$it]:$port/")
            }
            return uris
        }

        fun toHTTPV4Uris(ipv4s: List<String>, port: Int): List<String> {
            val uris = ArrayList<String>()
            ipv4s.forEach {
                uris.add("http://$it:$port/")
            }
            return uris
        }
    }
}