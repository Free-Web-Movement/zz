package io.github.freewebmovement.zz.system.net

import android.util.Log
import io.github.freewebmovement.zz.bussiness.Settings
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface


class IPList private constructor(settings: Settings) {
    var settings = settings
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
        var port = settings.localServerPort
        ipv6s.forEach {
            uris.add("http://[$it]:$port/")
        }
        return uris
    }

    fun toHTTPV4Uris(ipv4s: List<String>): List<String> {
        var uris = ArrayList<String>()
        var port = settings.localServerPort
        ipv4s.forEach {
            uris.add("http://$it:$port/")
        }
        return uris
    }

    companion object {
        private var instance: IPList? = null

        fun getInstance(settings: Settings): IPList {
            if (instance == null) {
                instance = IPList(settings)
            }
            return instance!!
        }
    }
}