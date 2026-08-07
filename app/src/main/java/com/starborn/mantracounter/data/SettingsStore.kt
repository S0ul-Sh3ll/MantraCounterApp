package com.starborn.mantracounter.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** App-wide preferences. Per-japa settings live on the [Japa] row instead. */
class SettingsStore(private val context: Context) {

    private val listBackgroundKey = stringPreferencesKey("list_background")
    private val listBackgroundAlphaKey = floatPreferencesKey("list_background_alpha")
    private val vibrationKey = booleanPreferencesKey("vibration_enabled")
    private val soundKey = booleanPreferencesKey("bell_enabled")
    private val sortKey = stringPreferencesKey("japa_sort")
    private val timerIntervalKey = intPreferencesKey("timer_interval_seconds")
    private val timerEndMalasKey = intPreferencesKey("timer_end_malas")

    /** Absolute path to the background image behind the main japa list, or null. */
    val listBackground: Flow<String?> =
        context.dataStore.data.map { it[listBackgroundKey]?.takeIf(String::isNotBlank) }

    /** How strongly that background shows through. */
    val listBackgroundAlpha: Flow<Float> =
        context.dataStore.data.map { it[listBackgroundAlphaKey] ?: DEFAULT_ALPHA }

    /** Covers both the per-bead tick and the mala-completion pulse. */
    val vibrationEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[vibrationKey] ?: true }

    val bellEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[soundKey] ?: true }

    val sort: Flow<JapaSort> = context.dataStore.data.map { JapaSort.fromName(it[sortKey]) }

    /** Seconds between automatic counts in timer mode. */
    val timerInterval: Flow<Int> =
        context.dataStore.data.map { it[timerIntervalKey] ?: DEFAULT_INTERVAL }

    /** How many malas a timed session runs for before the gongs sound. */
    val timerEndMalas: Flow<Int> =
        context.dataStore.data.map { it[timerEndMalasKey] ?: DEFAULT_END_MALAS }

    suspend fun setTimerInterval(seconds: Int) {
        context.dataStore.edit { it[timerIntervalKey] = seconds.coerceIn(1, 600) }
    }

    suspend fun setTimerEndMalas(malas: Int) {
        context.dataStore.edit { it[timerEndMalasKey] = malas.coerceIn(1, 1000) }
    }

    suspend fun setSort(sort: JapaSort) {
        context.dataStore.edit { it[sortKey] = sort.name }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[vibrationKey] = enabled }
    }

    suspend fun setBellEnabled(enabled: Boolean) {
        context.dataStore.edit { it[soundKey] = enabled }
    }

    suspend fun setListBackground(path: String?) {
        context.dataStore.edit { prefs ->
            if (path == null) prefs.remove(listBackgroundKey) else prefs[listBackgroundKey] = path
        }
    }

    suspend fun setListBackgroundAlpha(alpha: Float) {
        context.dataStore.edit { it[listBackgroundAlphaKey] = alpha.coerceIn(0.05f, 1f) }
    }

    companion object {
        const val DEFAULT_ALPHA = 0.35f
        const val DEFAULT_INTERVAL = 3
        const val DEFAULT_END_MALAS = 1
    }
}
