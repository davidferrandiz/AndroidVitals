package com.ferryapps.vitals.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferryapps.vitals.core.domain.analytics.AnalyticsEvents
import com.ferryapps.vitals.core.domain.analytics.AnalyticsTracker
import com.ferryapps.vitals.core.domain.model.AppSettings
import com.ferryapps.vitals.core.domain.model.TemperatureUnit
import com.ferryapps.vitals.core.domain.model.ThemeMode
import com.ferryapps.vitals.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val analytics: AnalyticsTracker,
) : ViewModel() {

    val settings: StateFlow<AppSettings?> =
        repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setTemperatureUnit(unit: TemperatureUnit) = update("temperature_unit", unit.name) {
        repository.setTemperatureUnit(unit)
    }

    fun setThemeMode(mode: ThemeMode) = update("theme_mode", mode.name) { repository.setThemeMode(mode) }

    fun setDynamicColor(enabled: Boolean) = update("dynamic_color", enabled.toString()) {
        repository.setDynamicColor(enabled)
    }

    fun setExpandCardsByDefault(enabled: Boolean) = update("expand_cards", enabled.toString()) {
        repository.setExpandCardsByDefault(enabled)
    }

    /** No se registra el evento: si el usuario desactiva la analítica, no se le pregunta por qué. */
    fun setAnalyticsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setAnalyticsEnabled(enabled) }
    }

    fun setCrashReportsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setCrashReportsEnabled(enabled) }
    }

    fun setBatteryHistoryEnabled(enabled: Boolean) = update("battery_history", enabled.toString()) {
        repository.setBatteryHistoryEnabled(enabled)
    }

    private fun update(setting: String, value: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            analytics.logEvent(
                AnalyticsEvents.SETTING_CHANGED,
                mapOf(AnalyticsEvents.PARAM_SETTING to setting, AnalyticsEvents.PARAM_VALUE to value)
            )
        }
    }
}
