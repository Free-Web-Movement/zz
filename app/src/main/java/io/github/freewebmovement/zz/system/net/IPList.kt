package io.github.freewebmovement.zz.system.net

import android.util.Log
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface


class IPList private constructor(port: Int) {
	var ipv4IPs: List<String>
	var ipv6IPs: List<String>

	init {
		ipv4IPs = v4s(port)
		ipv6IPs = v6s(port)
	}

	fun hasPublicIP(): Boolean {
		return ipv4IPs.isNotEmpty() || ipv4IPs.isNotEmpty()
	}

	private fun isPublic(inetAddress: InetAddress): Boolean {
		return !inetAddress.isLoopbackAddress && !inetAddress.isSiteLocalAddress
				&& !inetAddress.isAnyLocalAddress && !inetAddress.isLinkLocalAddress &&
				!inetAddress.isMCOrgLocal && !inetAddress.isMCNodeLocal &&
				!inetAddress.isMCLinkLocal && !inetAddress.isMCSiteLocal
	}

	private fun addresses(port: Int, isIPV6: Boolean): List<String> {
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
						if (!isIPV6 && inetAddress is Inet4Address) {
							addresses.add("http://$address:$port/")
							addresses.add("http://$address:$port/$DOWNLOAD_URI")
						}
						if (isIPV6 && inetAddress is Inet6Address) {
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

	private fun v4s(port: Int): List<String> {
		ipv4IPs = addresses(port, false)
		return ipv4IPs
	}

	private fun v6s(port: Int): List<String> {
		ipv6IPs = addresses(port, true)
		return ipv6IPs
	}

	companion object {
		private var instance: IPList? = null
		fun getInstance(port: Int): IPList {
			if (instance != null) {
				instance = IPList(port)
			}
			return instance!!
		}
	}
}