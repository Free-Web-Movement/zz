package io.github.freewebmovement.zz.bussiness

import androidx.datastore.preferences.core.intPreferencesKey
import io.github.freewebmovement.zz.system.net.api.crypto.Crypto
import io.github.freewebmovement.zz.system.persistence.Preference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

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

class Settings(private val preference: Preference) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val MESSAGE_PERSISTENCE_PERIOD = intPreferencesKey("MESSAGE_PERSISTENCE_PERIOD")
    private val LOCAL_SERVER_PORT = intPreferencesKey("LOCAL_SERVER_PORT")
    private val MESSAGE_TYPE = intPreferencesKey("MESSAGE_TYPE")
    private val REALTIME_COMMUNICATION_TYPE = intPreferencesKey("REALTIME_COMMUNICATION_TYPE")

    // Message Persistence Period
    var messagePeriod: Int = 0
        get() {
            coroutineScope.async {
                field = preference.read(MESSAGE_PERSISTENCE_PERIOD) ?: 0
                return@async field
            }
            return field
        }
        set(value) {
            coroutineScope.launch {
                preference.write(MESSAGE_PERSISTENCE_PERIOD, value)
            }
        }
    var localServerPort: Int = 0
        get() {
            coroutineScope.async {
                field = preference.read(LOCAL_SERVER_PORT) ?: 0
                if (field == 0) {
                    field = (((1 shl 10) + 1) ..<(1 shl 32)).random()
                }
                return@async field
            }
            return field
        }
        set(value) {
            if (value <= 1024 && value != 0) {
                throw Exception("Value must be large than 1024!")
            }
            coroutineScope.launch {
                preference.write(LOCAL_SERVER_PORT, value)
            }
        }
    var messageTypeSupported: Int = 0
        get() {
            coroutineScope.async {
                field = preference.read(MESSAGE_TYPE) ?: 0
                return@async field
            }
            return field
        }
        set(value) {
            coroutineScope.launch {
                preference.write(MESSAGE_TYPE, value)
            }
        }

    var realtimeTypeSupported: Int = 0
        get() {
            coroutineScope.async {
                field = preference.read(REALTIME_COMMUNICATION_TYPE) ?: 0
                return@async field
            }
            return field
        }
        set(value) {
            coroutineScope.launch {
                preference.write(REALTIME_COMMUNICATION_TYPE, value)
            }
        }
}