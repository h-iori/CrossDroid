package com.ioristudios.crossdroid.backend.database

import android.content.Context
import android.os.Environment
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val AUTO_ACCEPT = booleanPreferencesKey("auto_accept_trusted")
    private val DOWNLOADS_DIR = stringPreferencesKey("downloads_directory")

    val autoAcceptTrusted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_ACCEPT] ?: true
    }

    val downloadsDirectory: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[DOWNLOADS_DIR] ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
    }

    suspend fun setAutoAcceptTrusted(value: Boolean) {
        context.dataStore.edit { it[AUTO_ACCEPT] = value }
    }

    suspend fun setDownloadsDirectory(path: String) {
        context.dataStore.edit { it[DOWNLOADS_DIR] = path }
    }
}
