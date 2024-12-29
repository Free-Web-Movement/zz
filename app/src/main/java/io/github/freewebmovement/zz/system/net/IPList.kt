package io.github.freewebmovement.zz.system.net

import android.util.Log
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface


class IPList {
    companion object {
        private var initIPv4 = false
        private var initIPv6 = false
        private lateinit var ipv4IPs: List<String>
        private lateinit var ipv6IPs: List<String>

        private fun isPublic(inetAddress: InetAddress): Boolean {
            return !inetAddress.isLoopbackAddress && !inetAddress.isSiteLocalAddress
                    && !inetAddress.isAnyLocalAddress && !inetAddress.isLinkLocalAddress &&
                    !inetAddress.isMCOrgLocal && !inetAddress.isMCNodeLocal &&
                    !inetAddress.isMCLinkLocal && !inetAddress.isMCSiteLocal
        }

        private fun addresses(port:Int, isIPV6: Boolean) : List<String> {
            val addresses = ArrayList<String>()
            try {
                val networkInterfaces = NetworkInterface.getNetworkInterfaces()
                while (networkInterfaces.hasMoreElements()) {
                    val enumIpAddresses = networkInterfaces.nextElement().inetAddresses
                    while (enumIpAddresses.hasMoreElements()) {
                        val inetAddress = enumIpAddresses.nextElement()
                        if (isPublic(inetAddress)) {
                            val address =
                                inetAddress.hostAddress?.toString()?.split("%")?.get(0).toString()
                            if ( !isIPV6 && inetAddress is Inet4Address) {
                                addresses.add("http://$address:$port/")
                                addresses.add("http://$address:$port/$DOWNLOAD_URI")
                            }
                            if (isIPV6 && inetAddress is Inet6Address){
                                addresses.add("http://[$address]:$port/")
                                addresses.add("http://[$address]:$port/$DOWNLOAD_URI")
                            }
                        }
                    }
                }
            } catch (ex: Exception) {
                Log.e("IP Address", ex.toString())
            }
            return addresses.distinct()
        }

        fun v4s(port: Int): List<String> {
            if (initIPv4) {
                return ipv4IPs
            }
            ipv4IPs = addresses(port,false)
            initIPv4 = true
            return ipv4IPs
        }

        fun v6s(port: Int): List<String> {
            if (initIPv6) {
                return ipv6IPs
            }
            ipv6IPs = addresses(port,true)
            initIPv6 = true
            return ipv6IPs
        }

        fun hasPublicIP(port:Int) : Boolean {
            val ipv4 = v4s(port)
            val ipv6 = v6s(port)
            return  ipv4.isNotEmpty() || ipv6.isNotEmpty()
        }
    }
}