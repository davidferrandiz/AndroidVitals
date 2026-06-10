package com.ferryapps.vitals.feature.monitor.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferryapps.vitals.core.domain.model.BatteryInfo
import com.ferryapps.vitals.core.domain.model.CpuCoreInfo
import com.ferryapps.vitals.core.domain.model.NetworkSpeed
import com.ferryapps.vitals.core.domain.model.ProcessMemoryBreakdown
import com.ferryapps.vitals.core.domain.model.StorageInfo
import com.ferryapps.vitals.core.domain.model.ThermalZone
import com.ferryapps.vitals.core.domain.model.TopProcess
import com.ferryapps.vitals.core.domain.model.VitalsSnapshot
import com.ferryapps.vitals.core.domain.usecase.GetBatteryUseCase
import com.ferryapps.vitals.core.domain.usecase.GetCpuCoresUseCase
import com.ferryapps.vitals.core.domain.usecase.GetNetworkSpeedUseCase
import com.ferryapps.vitals.core.domain.usecase.GetProcessMemoryBreakdownUseCase
import com.ferryapps.vitals.core.domain.usecase.GetStorageUseCase
import com.ferryapps.vitals.core.domain.usecase.GetTopProcessesUseCase
import com.ferryapps.vitals.core.domain.usecase.GetVitalsUseCase
import com.ferryapps.vitals.feature.monitor.service.MonitorServiceState
import com.ferryapps.vitals.feature.monitor.service.VitalsForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MonitorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    getVitals: GetVitalsUseCase,
    getProcessMemoryBreakdown: GetProcessMemoryBreakdownUseCase,
    getBattery: GetBatteryUseCase,
    getCpuCores: GetCpuCoresUseCase,
    getNetworkSpeed: GetNetworkSpeedUseCase,
    getStorage: GetStorageUseCase,
    getTopProcesses: GetTopProcessesUseCase,
    serviceState: MonitorServiceState
) : ViewModel() {

    init {
        context.startForegroundService(
            Intent(context, VitalsForegroundService::class.java)
        )
    }

    // ── Tarjetas expandidas ───────────────────────────────────────────────────
    private val _expandedCards = MutableStateFlow<Set<String>>(emptySet())
    val expandedCards: StateFlow<Set<String>> = _expandedCards

    fun toggleCard(id: String) {
        val current = _expandedCards.value
        _expandedCards.value = if (id in current) current - id else current + id
    }

    // ── Datos del Foreground Service (CPU, RAM, Threads) ─────────────────────
    val uiState: StateFlow<MonitorUiState> = serviceState.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonitorUiState())

    // ── Historial de snapshots (Room) ─────────────────────────────────────────
    val snapshots: StateFlow<List<VitalsSnapshot>> = getVitals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Desglose de memoria del proceso Vitals ────────────────────────────────
    private val _memoryExpanded = MutableStateFlow(false)
    val memoryExpanded: StateFlow<Boolean> = _memoryExpanded

    val memoryBreakdown: StateFlow<ProcessMemoryBreakdown?> = _memoryExpanded
        .flatMapLatest { if (it) getProcessMemoryBreakdown() else emptyFlow() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun toggleMemoryExpanded() { _memoryExpanded.value = !_memoryExpanded.value }

    // ── Batería ───────────────────────────────────────────────────────────────
    val battery: StateFlow<BatteryInfo?> = getBattery()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ── CPU por core + zonas térmicas ─────────────────────────────────────────
    val cpuCores: StateFlow<List<CpuCoreInfo>> = getCpuCores()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val thermalZones: StateFlow<List<ThermalZone>> = getCpuCores.thermalZones()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Red ───────────────────────────────────────────────────────────────────
    val networkSpeed: StateFlow<NetworkSpeed?> = getNetworkSpeed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ── Almacenamiento ────────────────────────────────────────────────────────
    val storage: StateFlow<StorageInfo?> = getStorage()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ── Top procesos por RAM ──────────────────────────────────────────────────
    val topProcesses: StateFlow<List<TopProcess>> = getTopProcesses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
