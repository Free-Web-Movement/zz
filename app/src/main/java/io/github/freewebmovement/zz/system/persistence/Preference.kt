package io.github.freewebmovement.zz.system.persistence

import android.content.SharedPreferences
import io.github.freewebmovement.peer.interfaces.IPreference

class Preference(private val sharedPreference: SharedPreferences) : IPreference {
    override fun <T> save(key: String, value: T) {
        var editor = sharedPreference.edit()
        when (value) {
            is Int -> editor.putInt(key, value as Int) as T
            is String -> editor.putString(key, value as String) as T
            is Boolean -> editor.putBoolean(key, value as Boolean) as T
            else -> {
                throw IllegalArgumentException("Unsupported Type!")
            }
        }
        editor.apply()
    }
    override fun <T> read(key: String, value: T) : T {
        return when (value) {
            is Int -> sharedPreference.getInt(key, value as Int) as T
            is String -> sharedPreference.getString(key, value as String) as T
            is Boolean -> sharedPreference.getBoolean(key, value as Boolean) as T
            else -> {
                throw IllegalArgumentException("Unsupported Type!")
            }
        }
    }
}