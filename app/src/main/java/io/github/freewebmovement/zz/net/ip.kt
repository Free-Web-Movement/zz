package io.github.freewebmovement.zz.net

import android.util.Log
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface

class ip {
    companion object {
        fun v4s(): List<String> {
            val addresses = ArrayList<String>();
            try {
                val networkInterfaces = NetworkInterface
                    .getNetworkInterfaces()
                while (networkInterfaces.hasMoreElements()) {
                    val enumIpAddresses = networkInterfaces.nextElement()
                        .inetAddresses
                    while (enumIpAddresses.hasMoreElements()) {
                        val inetAddress = enumIpAddresses.nextElement()
                        println("ipv6 --  " + inetAddress.address)
                        println("ip1 -- $inetAddress")
                        println("ip2 --" + inetAddress.hostAddress)

                        if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                            val address =
                                inetAddress.hostAddress?.toString()?.split("%")?.get(0).toString();
                            addresses.add("http://$address:" + HttpServer.PORT)
                        }
                    }
                }
            } catch (ex: Exception) {
                Log.e("IP Address", ex.toString())
            }
            return addresses.distinct();
        }
        fun v6s(): List<String> {
            val addresses = ArrayList<String>();
            try {
                val networkInterfaces = NetworkInterface
                    .getNetworkInterfaces()
                while (networkInterfaces.hasMoreElements()) {
                    val enumIpAddresses = networkInterfaces.nextElement()
                        .inetAddresses
                    while (enumIpAddresses.hasMoreElements()) {
                        val inetAddress = enumIpAddresses.nextElement()
                        println("ipv6 --  " + inetAddress.address)
                        println("ip1 -- $inetAddress")
                        println("ip2 --" + inetAddress.hostAddress)

                        if (!inetAddress.isLoopbackAddress && inetAddress is Inet6Address) {
                            val address =
                                inetAddress.hostAddress?.toString()?.split("%")?.get(0).toString();
                            addresses.add("http://[$address]:" + HttpServer.PORT)
                        }
                    }
                }
            } catch (ex: Exception) {
                Log.e("IP Address", ex.toString())
            }
            return addresses.distinct();
        }
    }
}