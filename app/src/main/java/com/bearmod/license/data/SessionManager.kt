package com.bearmod.license.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bearmod.license.models.Role
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val DATASTORE_NAME = "bear_license_session"

private val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

class SessionManager(private val context: Context) {

    companion object {
        private val KEY_LAST_ROLE = stringPreferencesKey("last_role")
    }

    val lastRole: Flow<Role?> = context.dataStore.data.map { prefs: Preferences ->
        prefs[KEY_LAST_ROLE]?.let { value ->
            runCatching { Role.valueOf(value) }.getOrNull()
        }
    }

    suspend fun setLastRole(role: Role) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_ROLE] = role.name
        }
    }

    suspend fun clearLastRole() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_LAST_ROLE)
        }
    }
}
