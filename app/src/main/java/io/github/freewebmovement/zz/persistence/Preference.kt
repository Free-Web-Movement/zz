package io.github.freewebmovement.zz.persistence

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private const val PREFERENCES_NAME = "ZZ"
private val Context.dataStore by preferencesDataStore(PREFERENCES_NAME)

class Preference(context: Context) {
    private val dataStore = context.dataStore
    suspend fun <T> read(key:  Preferences.Key<T>): T? {
        val preferences = dataStore.data.first()
        return preferences[key] ?: null
    }
    suspend fun <T> write(key:  Preferences.Key<T>, value: T) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }
}