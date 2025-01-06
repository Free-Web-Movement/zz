package io.github.freewebmovement.zz.bussiness

import android.content.SharedPreferences

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


class Settings(private val preference: SharedPreferences) {
    // Message Persistence Period
    var messagePeriod: Int = 0
        get() {
            field = preference.getInt(MESSAGE_PERSISTENCE_PERIOD, 0)
            return field
        }
        set(value) {
            val editor = preference.edit()
            editor.putInt(MESSAGE_PERSISTENCE_PERIOD, value)
            editor.apply()
            field = value
        }
    var localServerPort: Int = 0
        get() {
            field = preference.getInt(LOCAL_SERVER_PORT, 0)
            if (field == 0) {
                val min = (1u shl 10) + 1u
                val max = (1u shl 16) - 1u
                field = (min..max).random().toInt()
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
    var messageTypeSupported: Int = 0
        get() {
            field = preference.getInt(MESSAGE_TYPE, 0)
            return field
        }
        set(value) {
            val editor = preference.edit()

            editor.putInt(MESSAGE_TYPE, value)
            editor.apply()

            field = value
        }

    var realtimeTypeSupported: Int = 0
        get() {
            field = preference.getInt(REALTIME_COMMUNICATION_TYPE, 0)
            return field
        }
        set(value) {
            val editor = preference.edit()
            editor.putInt(REALTIME_COMMUNICATION_TYPE, value)
            editor.apply()
            field = value
        }
}