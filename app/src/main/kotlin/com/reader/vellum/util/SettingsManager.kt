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
    private val EPUB_FONT_SIZE = androidx.datastore.preferences.core.floatPreferencesKey("epub_font_size")
    private val EPUB_FONT_FAMILY = androidx.datastore.preferences.core.stringPreferencesKey("epub_font_family")
    private val EPUB_LINE_HEIGHT = androidx.datastore.preferences.core.floatPreferencesKey("epub_line_height")
    private val EPUB_THEME = androidx.datastore.preferences.core.stringPreferencesKey("epub_theme")
    private val EPUB_MARGIN = androidx.datastore.preferences.core.intPreferencesKey("epub_margin")
    private val EPUB_LOCATOR_PREFIX = "epub_locator_"

    val mangaMode: Flow<Boolean> = context.dataStore.data.map { it[MANGA_MODE] ?: false }
    val tapToTurn: Flow<Boolean> = context.dataStore.data.map { it[TAP_TO_TURN] ?: true }
    val volumeKeys: Flow<Boolean> = context.dataStore.data.map { it[VOLUME_KEYS] ?: false }
    val hideCompleted: Flow<Boolean> = context.dataStore.data.map { it[HIDE_COMPLETED] ?: true }
    val adaptiveChroma: Flow<Boolean> = context.dataStore.data.map { it[ADAPTIVE_CHROMA] ?: true }
    val epubFontSize: Flow<Float> = context.dataStore.data.map { it[EPUB_FONT_SIZE] ?: 18f }
    val epubFontFamily: Flow<String> = context.dataStore.data.map { it[EPUB_FONT_FAMILY] ?: "serif" }
    val epubLineHeight: Flow<Float> = context.dataStore.data.map { it[EPUB_LINE_HEIGHT] ?: 1.5f }
    val epubTheme: Flow<String> = context.dataStore.data.map { it[EPUB_THEME] ?: "dark" }
    val epubMargin: Flow<Int> = context.dataStore.data.map { it[EPUB_MARGIN] ?: 24 }

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

    suspend fun setEpubFontSize(size: Float) {
        context.dataStore.edit { it[EPUB_FONT_SIZE] = size }
    }

    suspend fun setEpubFontFamily(family: String) {
        context.dataStore.edit { it[EPUB_FONT_FAMILY] = family }
    }

    suspend fun setEpubLineHeight(height: Float) {
        context.dataStore.edit { it[EPUB_LINE_HEIGHT] = height }
    }

    suspend fun setEpubTheme(theme: String) {
        context.dataStore.edit { it[EPUB_THEME] = theme }
    }

    suspend fun setEpubMargin(margin: Int) {
        context.dataStore.edit { it[EPUB_MARGIN] = margin }
    }

    fun getEpubLocator(bookId: String): Flow<String?> {
        val key = androidx.datastore.preferences.core.stringPreferencesKey(locatorKey(bookId))
        return context.dataStore.data.map { it[key] }
    }

    suspend fun setEpubLocator(bookId: String, locator: String) {
        val key = androidx.datastore.preferences.core.stringPreferencesKey(locatorKey(bookId))
        context.dataStore.edit { it[key] = locator }
    }

    private fun locatorKey(bookId: String): String = EPUB_LOCATOR_PREFIX + bookId
}
