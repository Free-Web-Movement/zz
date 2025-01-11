package io.github.freewebmovement.zz.system.persistence

import android.content.SharedPreferences
import io.ktor.server.sessions.SessionStorage

class PeerSessionStorage(): SessionStorage {
    override suspend fun invalidate(id: String) {
        TODO("Not yet implemented")
    }

    override suspend fun read(id: String): String {
        TODO("Not yet implemented")
    }

    override suspend fun write(id: String, value: String) {
        TODO("Not yet implemented")
    }
}