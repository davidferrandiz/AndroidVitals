package com.ferryapps.vitals.feature.battery

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferryapps.vitals.core.domain.analytics.MetricUnavailableReporter
import com.ferryapps.vitals.core.domain.model.AppSettings
import com.ferryapps.vitals.core.domain.model.BatteryHistory
import com.ferryapps.vitals.core.domain.model.BatteryInfo
import com.ferryapps.vitals.core.domain.repository.SettingsRepository
import com.ferryapps.vitals.core.domain.usecase.GetBatteryHistoryUseCase
import com.ferryapps.vitals.core.domain.usecase.GetBatteryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class BatteryViewModel @Inject constructor(
    getBattery: GetBatteryUseCase,
    getBatteryHistory: GetBatteryHistoryUseCase,
    settingsRepository: SettingsRepository,
    unavailableReporter: MetricUnavailableReporter,
) : ViewModel() {

    private val readings: Flow<BatteryInfo> = getBattery(intervalMs = SAMPLE_INTERVAL_MS)
        .onEach { info ->
            if (info.currentNowMa == null) unavailableReporter.report(MetricUnavailableReporter.BATTERY_CURRENT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && info.cycleCount == null) {
                unavailableReporter.report(MetricUnavailableReporter.BATTERY_CYCLES)
            }
        }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    val battery: StateFlow<BatteryInfo?> =
        readings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Potencia en W (valor absoluto) de los últimos 5 minutos. */
    val powerHistory: StateFlow<List<Float>> = readings
        .mapNotNull { info -> info.powerWatts?.let { abs(it) } }
        .runningFold(emptyList<Float>()) { acc, watts -> (acc + watts).takeLast(HISTORY_SIZE) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Lo escribe VitalsWorker; Room emite de nuevo con cada muestra. */
    val history: StateFlow<BatteryHistory?> =
        getBatteryHistory().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val settings: StateFlow<AppSettings> =
        settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    companion object {
        private const val SAMPLE_INTERVAL_MS = 2_000L

        /** 5 minutos a una muestra cada 2 s. */
        const val HISTORY_SIZE = 150
    }
}
