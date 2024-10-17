package io.github.freewebmovement.zz.net

import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
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
                        println("download: " + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
                        println("download: " + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS))

                        val directory: File = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString())
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
                                if (file.exists()) {
                                    val fo: OutputStream = FileOutputStream(file)
                                    fo.write("Hello Ktor!".toByteArray())
                                    fo.flush()
                                    fo.close()
                                    println("file created: $file")
                                }
                            }
                        }


                        if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                            val address =
                                inetAddress.hostAddress?.toString()?.split("%")?.get(0).toString();
                            addresses.add("http://$address:" + HttpServer.PORT)
                            addresses.add("http://$address:" + HttpServer.PORT + "/d");
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