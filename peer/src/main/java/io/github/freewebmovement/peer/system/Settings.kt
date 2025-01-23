package io.github.freewebmovement.peer.system

import io.github.freewebmovement.peer.interfaces.IPreference
import kotlin.reflect.KProperty

//enum class MessageType(i: Int) {
//    TEXT(0b1),
//    IMAGE(0b10),
//    VOICE(0b100),
//    VIDEO(0b1000)
//}
//
//enum class RealtimeType(i: Int) {
//    VOICE(0b1),
//    VIDEO(0b10)
//}

private const val MESSAGE_PERSISTENCE_PERIOD = "MESSAGE_PERSISTENCE_PERIOD"
private const val LOCAL_SERVER_PORT = "LOCAL_SERVER_PORT"
private const val MESSAGE_TYPE = "MESSAGE_TYPE"
private const val REALTIME_COMMUNICATION_TYPE = "REALTIME_COMMUNICATION_TYPE"
private const val MINE_PROFILE_IMAGE_URI = "MINE_PROFILE_IMAGE_URI"
private const val MINE_PROFILE_NICKNAME = "MINE_PROFILE_NICKNAME"
private const val MINE_PROFILE_INTRO = "MINE_PROFILE_INTRO"


class PreferenceAccessor<T>(
    private val preference: IPreference,
    private val key: String,
    private var field: T
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        field = preference.read(key, field)
        return field
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        preference.save(key, value)
        field = value
    }
}

class Settings(private val preference: IPreference) {
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
    var mineProfileIntro: String by PreferenceAccessor(
        preference, MINE_PROFILE_INTRO,
        field = ""
    )
    var localServerPort: Int = 0
        get() {
            field = preference.read(LOCAL_SERVER_PORT, 0)
            if (field == 0) {
                val min = (1u shl 10) + 1u
                val max = (1u shl 16) - 1u
                field = (min..max).random().toInt()
                preference.save(LOCAL_SERVER_PORT, field)
            }
            return field
        }
        set(value) {
            if (value <= 1024 && value != 0) {
                throw Exception("Value must be larger than 1024!")
            }
            preference.save(LOCAL_SERVER_PORT, value)
            field = value
        }
}