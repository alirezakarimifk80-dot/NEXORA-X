package com.nexora.x.core.database

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nexora.x.core.model.ProfileSortOrder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.nexoraDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "nexora_preferences"
)

/**
 * Jetpack DataStore Preferences manager for NEXORA X Phase 2.
 * Persists:
 * - Active selected profile ID
 * - Theme mode preference (Dark / Light)
 * - Default Servers sort order
 */
@Singleton
class NexoraPreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val SELECTED_PROFILE_ID = stringPreferencesKey("selected_profile_id")
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val SORT_ORDER = stringPreferencesKey("profile_sort_order")
    }

    val selectedProfileId: Flow<String?> = context.nexoraDataStore.data.map { prefs ->
        prefs[Keys.SELECTED_PROFILE_ID]
    }

    val isDarkTheme: Flow<Boolean> = context.nexoraDataStore.data.map { prefs ->
        prefs[Keys.IS_DARK_THEME] ?: true
    }

    val sortOrder: Flow<ProfileSortOrder> = context.nexoraDataStore.data.map { prefs ->
        val raw = prefs[Keys.SORT_ORDER]
        runCatching {
            if (raw != null) ProfileSortOrder.valueOf(raw) else ProfileSortOrder.RECENTLY_UPDATED
        }.getOrDefault(ProfileSortOrder.RECENTLY_UPDATED)
    }

    suspend fun setSelectedProfileId(profileId: String?) {
        context.nexoraDataStore.edit { prefs ->
            if (profileId.isNullOrBlank()) {
                prefs.remove(Keys.SELECTED_PROFILE_ID)
            } else {
                prefs[Keys.SELECTED_PROFILE_ID] = profileId
            }
        }
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.nexoraDataStore.edit { prefs ->
            prefs[Keys.IS_DARK_THEME] = isDark
        }
    }

    suspend fun setSortOrder(order: ProfileSortOrder) {
        context.nexoraDataStore.edit { prefs ->
            prefs[Keys.SORT_ORDER] = order.name
        }
    }
}
