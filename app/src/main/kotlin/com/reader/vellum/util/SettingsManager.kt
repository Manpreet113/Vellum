package com.reader.vellum.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val MANGA_MODE = booleanPreferencesKey("manga_mode")
    private val TAP_TO_TURN = booleanPreferencesKey("tap_to_turn")
    private val VOLUME_KEYS = booleanPreferencesKey("volume_keys")
    private val HIDE_COMPLETED = booleanPreferencesKey("hide_completed")
    private val ADAPTIVE_CHROMA = booleanPreferencesKey("adaptive_chroma")

    val mangaMode: Flow<Boolean> = context.dataStore.data.map { it[MANGA_MODE] ?: false }
    val tapToTurn: Flow<Boolean> = context.dataStore.data.map { it[TAP_TO_TURN] ?: true }
    val volumeKeys: Flow<Boolean> = context.dataStore.data.map { it[VOLUME_KEYS] ?: false }
    val hideCompleted: Flow<Boolean> = context.dataStore.data.map { it[HIDE_COMPLETED] ?: true }
    val adaptiveChroma: Flow<Boolean> = context.dataStore.data.map { it[ADAPTIVE_CHROMA] ?: true }

    suspend fun setMangaMode(enabled: Boolean) {
        context.dataStore.edit { it[MANGA_MODE] = enabled }
    }

    suspend fun setTapToTurn(enabled: Boolean) {
        context.dataStore.edit { it[TAP_TO_TURN] = enabled }
    }

    suspend fun setVolumeKeys(enabled: Boolean) {
        context.dataStore.edit { it[VOLUME_KEYS] = enabled }
    }

    suspend fun setHideCompleted(enabled: Boolean) {
        context.dataStore.edit { it[HIDE_COMPLETED] = enabled }
    }

    suspend fun setAdaptiveChroma(enabled: Boolean) {
        context.dataStore.edit { it[ADAPTIVE_CHROMA] = enabled }
    }
}
