package com.ferryapps.vitals.feature.monitor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferryapps.vitals.core.domain.analytics.AnalyticsEvents
import com.ferryapps.vitals.core.domain.analytics.AnalyticsTracker
import com.ferryapps.vitals.core.domain.analytics.MetricUnavailableReporter
import com.ferryapps.vitals.core.domain.model.AppSettings
import com.ferryapps.vitals.core.domain.model.BatteryInfo
import com.ferryapps.vitals.core.domain.model.CpuSnapshot
import com.ferryapps.vitals.core.domain.model.MemoryInfo
import com.ferryapps.vitals.core.domain.model.NetworkInfo
import com.ferryapps.vitals.core.domain.model.NetworkSpeed
import com.ferryapps.vitals.core.domain.model.ProcessMemoryBreakdown
import com.ferryapps.vitals.core.domain.model.StorageInfo
import com.ferryapps.vitals.core.domain.model.ThermalState
import com.ferryapps.vitals.core.domain.model.ThermalStatus
import com.ferryapps.vitals.core.domain.model.ThermalZone
import com.ferryapps.vitals.core.domain.model.TopProcess
import com.ferryapps.vitals.core.domain.repository.EngagementRepository
import com.ferryapps.vitals.core.domain.repository.SettingsRepository
import com.ferryapps.vitals.core.domain.usecase.DeviceSummary
import com.ferryapps.vitals.core.domain.usecase.GetAppCpuUsageUseCase
import com.ferryapps.vitals.core.domain.usecase.GetBatteryUseCase
import com.ferryapps.vitals.core.domain.usecase.GetCpuCoresUseCase
import com.ferryapps.vitals.core.domain.usecase.GetDeviceSummaryUseCase
import com.ferryapps.vitals.core.domain.usecase.GetMemoryUseCase
import com.ferryapps.vitals.core.domain.usecase.GetNetworkInfoUseCase
import com.ferryapps.vitals.core.domain.usecase.GetNetworkSpeedUseCase
import com.ferryapps.vitals.core.domain.usecase.GetProcessMemoryBreakdownUseCase
import com.ferryapps.vitals.core.domain.usecase.GetStorageUseCase
import com.ferryapps.vitals.core.domain.usecase.GetThermalStateUseCase
import com.ferryapps.vitals.core.domain.usecase.GetThreadCountUseCase
import com.ferryapps.vitals.core.domain.usecase.GetTopProcessesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Panel principal. Todas las lecturas se hacen solo mientras la pantalla está visible:
 * la UI colecciona con `collectAsStateWithLifecycle` y aquí `WhileSubscribed(5 s)`
 * detiene los flows al ir a segundo plano.
 */
@HiltViewModel
class MonitorViewModel @Inject constructor(
    getCpuCores: GetCpuCoresUseCase,
    getAppCpuUsage: GetAppCpuUsageUseCase,
    getMemory: GetMemoryUseCase,
    getThreadCount: GetThreadCountUseCase,
    getBattery: GetBatteryUseCase,
    getNetworkSpeed: GetNetworkSpeedUseCase,
    getNetworkInfo: GetNetworkInfoUseCase,
    getStorage: GetStorageUseCase,
    getTopProcesses: GetTopProcessesUseCase,
    getProcessMemoryBreakdown: GetProcessMemoryBreakdownUseCase,
    getThermalState: GetThermalStateUseCase,
    getDeviceSummary: GetDeviceSummaryUseCase,
    settingsRepository: SettingsRepository,
    private val analytics: AnalyticsTracker,
    private val unavailableReporter: MetricUnavailableReporter,
    private val engagement: EngagementRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateInVm(AppSettings())

    val deviceSummary: StateFlow<DeviceSummary?> = flow<DeviceSummary?> { emit(getDeviceSummary()) }.stateInVm(null)

    // ── Tarjetas desplegadas ─────────────────────────────────────────────────
    /** null = el usuario aún no ha tocado nada y manda el ajuste "tarjetas abiertas al iniciar". */
    private val userExpanded = MutableStateFlow<Set<String>?>(null)

    val expandedCards: StateFlow<Set<String>> = combine(userExpanded, settings) { user, prefs ->
        user ?: if (prefs.expandCardsByDefault) ALL_CARDS else emptySet()
    }.stateInVm(emptySet())

    fun toggleCard(id: String) {
        val current = expandedCards.value
        val expanding = id !in current
        userExpanded.value = if (expanding) current + id else current - id
        if (expanding) {
            analytics.logEvent(AnalyticsEvents.CARD_EXPAND, mapOf(AnalyticsEvents.PARAM_CARD to id))
            viewModelScope.launch { engagement.markEngaged() }
        }
    }

    // ── Sistema ──────────────────────────────────────────────────────────────
    /** Una sola lectura de /sys compartida por la tarjeta y su gráfica. */
    private val cpuSnapshots: Flow<CpuSnapshot> = getCpuCores.snapshot()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    private val speeds: Flow<NetworkSpeed> = getNetworkSpeed()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    val cpu: StateFlow<CpuSnapshot?> = cpuSnapshots
        .onEach { snapshot ->
            if (snapshot.cores.isNotEmpty() && snapshot.cores.all { it.coreName == null }) {
                unavailableReporter.report(MetricUnavailableReporter.CPU_CORE_NAMES)
            }
        }
        .stateInVm(null)

    val cpuHistory: StateFlow<List<Float>> = cpuSnapshots
        .map { it.estimatedLoadPercent }
        .history()
        .stateInVm(emptyList())

    val thermalZones: StateFlow<List<ThermalZone>?> = getCpuCores.thermalZones()
        .onEach { if (it.isEmpty()) unavailableReporter.report(MetricUnavailableReporter.THERMAL_ZONES) }
        .stateInVm(null)

    val thermalState: StateFlow<ThermalState?> = getThermalState()
        .onEach {
            if (it.status != ThermalStatus.UNAVAILABLE && it.marginPercent == null) {
                unavailableReporter.report(MetricUnavailableReporter.THERMAL_HEADROOM)
            }
        }
        .stateInVm(null)

    val memory: StateFlow<MemoryInfo?> = getMemory().stateInVm(null)

    val storage: StateFlow<StorageInfo?> = getStorage().stateInVm(null)

    val battery: StateFlow<BatteryInfo?> = getBattery()
        .onEach { if (it.currentNowMa == null) unavailableReporter.report(MetricUnavailableReporter.BATTERY_CURRENT) }
        .stateInVm(null)

    val networkSpeed: StateFlow<NetworkSpeed?> = speeds.stateInVm(null)

    /** Historial de bajada y subida en bytes/s, para la gráfica de la tarjeta Red. */
    val networkHistory: StateFlow<Pair<List<Float>, List<Float>>> = speeds
        .runningFold(emptyList<Float>() to emptyList<Float>()) { (rx, tx), speed ->
            (rx + speed.rxBytesPerSec.toFloat()).takeLast(HISTORY_SIZE) to
                (tx + speed.txBytesPerSec.toFloat()).takeLast(HISTORY_SIZE)
        }
        .stateInVm(emptyList<Float>() to emptyList())

    val networkInfo: StateFlow<NetworkInfo?> = getNetworkInfo().stateInVm(null)

    // ── Consumo de Vitals (lo único de procesos que Android deja ver) ────────
    val appCpuPercent: StateFlow<Float> = getAppCpuUsage().stateInVm(0f)

    val appThreads: StateFlow<Int> = getThreadCount().stateInVm(0)

    val appMemory: StateFlow<ProcessMemoryBreakdown?> = getProcessMemoryBreakdown().stateInVm(null)

    val appProcesses: StateFlow<List<TopProcess>> = getTopProcesses().stateInVm(emptyList())

    private fun <T> Flow<T>.stateInVm(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    private fun Flow<Float>.history(): Flow<List<Float>> =
        runningFold(emptyList()) { acc, value -> (acc + value).takeLast(HISTORY_SIZE) }

    companion object {
        const val CARD_CPU = "cpu"
        const val CARD_VITALS = "vitals"
        private val ALL_CARDS = setOf(CARD_CPU, CARD_VITALS)

        /** 60 muestras a 1 por segundo = el último minuto. */
        const val HISTORY_SIZE = 60
    }
}
