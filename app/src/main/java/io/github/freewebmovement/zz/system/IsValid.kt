package io.github.freewebmovement.zz.system

import android.net.InetAddresses
import android.os.Build
import android.util.Patterns

class IsValid {
    companion object {
        fun IP(ip: String): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                InetAddresses.isNumericAddress(ip)
            } else {
                Patterns.IP_ADDRESS.matcher(ip).matches()
            }
        }

        fun port(port: UShort): Boolean {
            val min:UShort = ((1u shl 10) + 1u).toUShort()
            val max:UShort = ((1u shl 15) + 1u).toUShort()
            return port in min..max
        }
    }
}