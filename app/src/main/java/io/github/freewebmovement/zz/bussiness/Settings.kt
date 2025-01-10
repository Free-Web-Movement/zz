package io.github.freewebmovement.zz.bussiness

import android.content.SharedPreferences
import kotlin.reflect.KProperty

enum class MessageType(i: Int) {
    TEXT(0b1),
    IMAGE(0b10),
    VOICE(0b100),
    VIDEO(0b1000)
}

enum class RealtimeType(i: Int) {
    VOICE(0b1),
    VIDEO(0b10)
}

private const val MESSAGE_PERSISTENCE_PERIOD = "MESSAGE_PERSISTENCE_PERIOD"
private const val LOCAL_SERVER_PORT = "LOCAL_SERVER_PORT"
private const val MESSAGE_TYPE = "MESSAGE_TYPE"
private const val REALTIME_COMMUNICATION_TYPE = "REALTIME_COMMUNICATION_TYPE"
private const val MINE_PROFILE_IMAGE_URI = "MINE_PROFILE_IMAGE_URI"
private const val MINE_PROFILE_NICKNAME = "MINE_PROFILE_NICKNAME"
private const val MINE_PROFILE_SIGNATURE = "MINE_PROFILE_SIGNATURE"


@Suppress("UNCHECKED_CAST")
class PreferenceAccessor<T>(
    private val preference: SharedPreferences,
    private val key: String,
    private var field: T
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        field = when (field) {
            is Int -> preference.getInt(key, field as Int) as T
            is String -> preference.getString(key, field as String) as T
            else -> {
                throw IllegalArgumentException("Unsupported Type!")
            }
        }
        return field
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val editor = preference.edit()
        when (field) {
            is Int -> editor.putInt(key, value as Int)
            is String -> editor.putString(key, value as String)
        }
        editor.apply()
        field = value
    }
}

class Settings(private val preference: SharedPreferences) {
    // Message Persistence Period
    var messagePeriod: Int by PreferenceAccessor(
        preference, MESSAGE_PERSISTENCE_PERIOD,
        field = 0
    )
    var messageTypeSupported: Int by PreferenceAccessor(
        preference, MESSAGE_TYPE,
        field = 0
    )
    var realtimeTypeSupported: Int by PreferenceAccessor(
        preference, REALTIME_COMMUNICATION_TYPE,
        field = 0
    )

    var mineProfileImageUri: String by PreferenceAccessor(
        preference, MINE_PROFILE_IMAGE_URI,
        field = ""
    )
    var mineProfileNickname: String by PreferenceAccessor(
        preference, MINE_PROFILE_NICKNAME,
        field = ""
    )
    var mineProfileSignature: String by PreferenceAccessor(
        preference, MINE_PROFILE_SIGNATURE,
        field = ""
    )
    var localServerPort: Int = 0
        get() {
            field = preference.getInt(LOCAL_SERVER_PORT, 0)
            if (field == 0) {
                val min = (1u shl 10) + 1u
                val max = (1u shl 16) - 1u
                field = (min..max).random().toInt()
                val editor = preference.edit()
                editor.putInt(LOCAL_SERVER_PORT, field)
                editor.apply()
            }
            return field
        }
        set(value) {
            if (value <= 1024 && value != 0) {
                throw Exception("Value must be larger than 1024!")
            }
            val editor = preference.edit()
            editor.putInt(LOCAL_SERVER_PORT, value)
            editor.apply()
            field = value
        }
}