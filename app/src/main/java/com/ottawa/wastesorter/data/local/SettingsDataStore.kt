package com.ottawa.wastesorter.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ottawa.wastesorter.BuildConfig
import com.ottawa.wastesorter.domain.model.AppSettings
import com.ottawa.wastesorter.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Préférences locales (DataStore). La langue est gérée par AppCompat (sélecteur de langue par application). */
class SettingsDataStore(private val context: Context) : SettingsRepository {

    private object Keys {
        val BACKEND_URL = stringPreferencesKey("backend_url")
        val SAVE_THUMBNAILS = booleanPreferencesKey("save_thumbnails")
    }

    override val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            backendUrl = prefs[Keys.BACKEND_URL] ?: BuildConfig.DEFAULT_BACKEND_URL,
            saveThumbnails = prefs[Keys.SAVE_THUMBNAILS] ?: true,
        )
    }

    override suspend fun current(): AppSettings = settings.first()

    override suspend fun setBackendUrl(url: String) {
        context.dataStore.edit { it[Keys.BACKEND_URL] = url.trim() }
    }

    override suspend fun setSaveThumbnails(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SAVE_THUMBNAILS] = enabled }
    }
}
