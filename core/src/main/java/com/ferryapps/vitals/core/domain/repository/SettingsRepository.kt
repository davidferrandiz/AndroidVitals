package com.ferryapps.vitals.core.domain.repository

import com.ferryapps.vitals.core.domain.model.AppSettings
import com.ferryapps.vitals.core.domain.model.TemperatureUnit
import com.ferryapps.vitals.core.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setTemperatureUnit(unit: TemperatureUnit)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setExpandCardsByDefault(enabled: Boolean)
    suspend fun setAnalyticsEnabled(enabled: Boolean)
    suspend fun setCrashReportsEnabled(enabled: Boolean)
    suspend fun setBatteryHistoryEnabled(enabled: Boolean)
}
