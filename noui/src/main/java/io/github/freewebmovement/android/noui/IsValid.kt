package io.github.freewebmovement.android.noui

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

        fun port(port: Int): Boolean {
            val min:Int = (1 shl 10) + 1
            val max:Int = (1 shl 16) - 1
            return port in min..max
        }
    }
}