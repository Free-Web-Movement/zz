package io.github.freewebmovement.zz.system.net

import android.util.Log
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface


class IPList private constructor(port: Int) {
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
                            when (inetAddress) {
                                is Inet4Address -> ipv4IPLocal.add(address)
                                is Inet6Address -> ipv6IPLocal.add(address)
                            }
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e("IP Address", ex.toString())
        }
    }

    private fun isPublic(inetAddress: InetAddress): Boolean {
        return !inetAddress.isLoopbackAddress && !inetAddress.isSiteLocalAddress
                && !inetAddress.isAnyLocalAddress && !inetAddress.isLinkLocalAddress &&
                !inetAddress.isMCOrgLocal && !inetAddress.isMCNodeLocal &&
                !inetAddress.isMCLinkLocal && !inetAddress.isMCSiteLocal
    }

    fun toHTTPV6Uris(ipv6s: List<String>): List<String> {
        var uris = ArrayList<String>()
        ipv6s.forEach {
            uris.add("http://[$it]:$port/")
        }
        return uris
    }

    fun toHTTPV4Uris(ipv4s: List<String>): List<String> {
        var uris = ArrayList<String>()
        ipv4s.forEach {
            uris.add("http://$it:$port/")
        }
        return uris
    }

    companion object {
        var port: Int = 0
        private var instance: IPList? = null

        fun getInstance(inPort: Int): IPList {
            if (instance == null) {
                port = inPort
                instance = IPList(port)
            }
            return instance!!
        }
    }
}