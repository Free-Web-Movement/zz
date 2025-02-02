package io.github.freewebmovement.peer.system

import com.russhwolf.settings.Settings
import io.github.freewebmovement.peer.types.IPScopeType
import io.github.freewebmovement.peer.types.IPType
import io.github.freewebmovement.peer.interfaces.IPreference

class Preference(private val settings: Settings) : IPreference {
    override fun <T> save(key: String, value: T) {
        when (value) {
            is IPType -> {
                val temp = when(value) {
                    IPType.IPV6 -> 1
                    else -> 0
                }
                settings.putInt(key, temp)
            }
            is IPScopeType -> {
                val temp = when(value) {
                    IPScopeType.PUBLIC -> 1
                    else -> 0
                }
                settings.putInt(key, temp)
            }
            is Int -> settings.putInt(key, value as Int)
            is String -> settings.putString(key, value as String)
            is Boolean -> settings.putBoolean(key, value as Boolean)
            else -> {
                throw IllegalArgumentException("Unsupported Type!")
            }
        }
    }
    override fun <T> read(key: String, value: T) : T {
        return when (value) {
            is IPType -> {
                val temp = settings.getInt(key, 0)
                return when(temp) {
                    1 -> IPType.IPV6 as T
                    else -> IPType.IPV4 as T
                }
            }
            is IPScopeType -> {
                val temp = settings.getInt(key, 0)
                return when(temp) {
                    1 -> IPScopeType.PUBLIC as T
                    else -> IPScopeType.LOCAL as T
                }
            }
            is Int -> settings.getInt(key, value as Int) as T
            is String -> settings.getString(key, value as String) as T
            is Boolean -> settings.getBoolean(key, value as Boolean) as T
            else -> {
                throw IllegalArgumentException("Unsupported Type!")
            }
        }
    }
}

