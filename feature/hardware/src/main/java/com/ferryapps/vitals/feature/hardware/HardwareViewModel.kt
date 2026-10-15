package com.ferryapps.vitals.feature.hardware

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferryapps.vitals.core.domain.analytics.MetricUnavailableReporter
import com.ferryapps.vitals.core.domain.model.AppSettings
import com.ferryapps.vitals.core.domain.model.CpuSnapshot
import com.ferryapps.vitals.core.domain.model.DeviceInfo
import com.ferryapps.vitals.core.domain.model.MemoryInfo
import com.ferryapps.vitals.core.domain.model.SensorReading
import com.ferryapps.vitals.core.domain.model.ThermalState
import com.ferryapps.vitals.core.domain.model.ThermalZone
import com.ferryapps.vitals.core.domain.repository.SettingsRepository
import com.ferryapps.vitals.core.domain.usecase.GetCpuCoresUseCase
import com.ferryapps.vitals.core.domain.usecase.GetDeviceInfoUseCase
import com.ferryapps.vitals.core.domain.usecase.GetMemoryUseCase
import com.ferryapps.vitals.core.domain.usecase.GetSensorReadingsUseCase
import com.ferryapps.vitals.core.domain.usecase.GetThermalStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HardwareViewModel @Inject constructor(
    getDeviceInfo: GetDeviceInfoUseCase,
    getCpuCores: GetCpuCoresUseCase,
    getThermalState: GetThermalStateUseCase,
    getMemory: GetMemoryUseCase,
    getSensorReadings: GetSensorReadingsUseCase,
    settingsRepository: SettingsRepository,
    unavailableReporter: MetricUnavailableReporter,
) : ViewModel() {

    /** Se lee una vez por pantalla: incluye EGL (GPU) y MediaDrm (Widevine). */
    val device: StateFlow<DeviceInfo?> = flow<DeviceInfo?> { emit(getDeviceInfo()) }
        .onEach { info ->
            if (info != null) {
                if (info.soc.code == null) unavailableReporter.report(MetricUnavailableReporter.SOC_NAME)
                if (info.widevineLevel == null) unavailableReporter.report(MetricUnavailableReporter.WIDEVINE)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val cpu: StateFlow<CpuSnapshot?> = getCpuCores.snapshot().stateInVm(null)

    val thermalZones: StateFlow<List<ThermalZone>?> = getCpuCores.thermalZones().stateInVm(null)

    val thermalState: StateFlow<ThermalState?> = getThermalState().stateInVm(null)

    val memory: StateFlow<MemoryInfo?> = getMemory().stateInVm(null)

    val sensors: StateFlow<List<SensorReading>> = getSensorReadings().stateInVm(emptyList())

    /** Tiempo encendido, actualizado cada minuto. */
    val uptimeMs: StateFlow<Long> = flow {
        while (true) {
            emit(SystemClock.elapsedRealtime())
            delay(60_000)
        }
    }.stateInVm(SystemClock.elapsedRealtime())

    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateInVm(AppSettings())

    private fun <T> Flow<T>.stateInVm(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)
}
