package io.github.freewebmovement.zz.net

import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface


class IPList {
    companion object {
        lateinit var ipv4IPs: List<String>
        lateinit var ipv6IPs: List<String>
        var initIPV4 = false;
        var initIPV6 = false;
        var initIP = false;

        private fun isPublic(inetAddress: InetAddress): Boolean {
            return !inetAddress.isLoopbackAddress && !inetAddress.isSiteLocalAddress
                    && !inetAddress.isAnyLocalAddress && !inetAddress.isLinkLocalAddress &&
                    !inetAddress.isMCOrgLocal && !inetAddress.isMCNodeLocal &&
                    !inetAddress.isMCLinkLocal && !inetAddress.isMCSiteLocal;
        }

        fun v4s(port: Int): List<String> {
            if (initIPV4) {
                return ipv4IPs;
            }
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
                        println(
                            "download: " + Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOWNLOADS
                            )
                        );
                        println(
                            "download: " + Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOCUMENTS
                            )
                        )

                        val directory: File = File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                .toString()
                        )
                        val files = directory.listFiles()
                        if (files != null) {
                            Log.d("Files", "Size: " + files.size)
                            if (files.isNotEmpty()) {
                                for (i in files.indices) {
                                    Log.d("Files", "FileName:" + files[i].name)
                                }
                            } else {
                                val file: File = File(
                                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                        .toString() + "/" + File.separator + "a.txt"
                                )

                                file.createNewFile();
                                if (!file.exists()) {
                                    val fo: OutputStream = FileOutputStream(file)
                                    fo.write("Hello Ktor!".toByteArray())
                                    fo.flush()
                                    fo.close()
                                    println("file created: $file")
                                }
                            }
                        }


                        if (isPublic(inetAddress) && inetAddress is Inet4Address) {
                            val address =
                                inetAddress.hostAddress?.toString()?.split("%")?.get(0).toString();
                            addresses.add("http://$address:$port")
                            addresses.add("http://$address:$port/$DOWNLOAD_URI");
                        }
                    }
                }
            } catch (ex: Exception) {
                Log.e("IP Address", ex.toString())
            }
            ipv4IPs = addresses.distinct();
            initIPV4 = true;
            return ipv4IPs;
        }

        fun v6s(port: Int): List<String> {
            if (initIPV6) {
                return ipv6IPs;
            }
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

                        if (isPublic(inetAddress) && inetAddress is Inet6Address) {
                            val address =
                                inetAddress.hostAddress?.toString()?.split("%")?.get(0).toString();
                            addresses.add("http://[$address]:$port")
                        }
                    }
                }
            } catch (ex: Exception) {
                Log.e("IP Address", ex.toString())
            }
            ipv6IPs = addresses.distinct();
            initIPV6 = true;
            return ipv6IPs;
        }

        fun hasPublicIP(port:Int) : Boolean {
            val ipv4 = this.v4s(port);
            val ipv6 = this.v6s(port);
            println(ipv6.size);
            println(ipv6.size);
            return  ipv4.isNotEmpty() || ipv6.isNotEmpty()
        }
    }
}