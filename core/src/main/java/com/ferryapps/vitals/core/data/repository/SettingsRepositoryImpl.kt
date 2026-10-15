package com.ferryapps.vitals.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ferryapps.vitals.core.domain.model.AppSettings
import com.ferryapps.vitals.core.domain.model.TemperatureUnit
import com.ferryapps.vitals.core.domain.model.ThemeMode
import com.ferryapps.vitals.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private val defaults = AppSettings()

    override val settings: Flow<AppSettings> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            AppSettings(
                temperatureUnit = prefs[KEY_TEMPERATURE_UNIT]
                    ?.let { name -> TemperatureUnit.entries.firstOrNull { it.name == name } }
                    ?: defaults.temperatureUnit,
                themeMode = prefs[KEY_THEME_MODE]
                    ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                    ?: defaults.themeMode,
                dynamicColor = prefs[KEY_DYNAMIC_COLOR] ?: defaults.dynamicColor,
                expandCardsByDefault = prefs[KEY_EXPAND_CARDS] ?: defaults.expandCardsByDefault,
                analyticsEnabled = prefs[KEY_ANALYTICS] ?: defaults.analyticsEnabled,
                crashReportsEnabled = prefs[KEY_CRASH_REPORTS] ?: defaults.crashReportsEnabled,
                batteryHistoryEnabled = prefs[KEY_BATTERY_HISTORY] ?: defaults.batteryHistoryEnabled,
            )
        }
        .distinctUntilChanged()

    override suspend fun setTemperatureUnit(unit: TemperatureUnit) {
        dataStore.edit { it[KEY_TEMPERATURE_UNIT] = unit.name }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled }
    }

    override suspend fun setExpandCardsByDefault(enabled: Boolean) {
        dataStore.edit { it[KEY_EXPAND_CARDS] = enabled }
    }

    override suspend fun setAnalyticsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_ANALYTICS] = enabled }
    }

    override suspend fun setCrashReportsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_CRASH_REPORTS] = enabled }
    }

    override suspend fun setBatteryHistoryEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_BATTERY_HISTORY] = enabled }
    }

    private companion object {
        val KEY_TEMPERATURE_UNIT = stringPreferencesKey("temperature_unit")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val KEY_EXPAND_CARDS = booleanPreferencesKey("expand_cards_by_default")
        val KEY_ANALYTICS = booleanPreferencesKey("analytics_enabled")
        val KEY_CRASH_REPORTS = booleanPreferencesKey("crash_reports_enabled")
        val KEY_BATTERY_HISTORY = booleanPreferencesKey("battery_history_enabled")
    }
}
