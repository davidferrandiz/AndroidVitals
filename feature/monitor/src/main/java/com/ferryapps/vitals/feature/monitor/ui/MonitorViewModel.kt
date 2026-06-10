package com.ferryapps.vitals.feature.monitor.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferryapps.vitals.core.domain.model.ProcessMemoryBreakdown
import com.ferryapps.vitals.core.domain.model.VitalsSnapshot
import com.ferryapps.vitals.core.domain.usecase.GetProcessMemoryBreakdownUseCase
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
    serviceState: MonitorServiceState
) : ViewModel() {

    init {
        context.startForegroundService(
            Intent(context, VitalsForegroundService::class.java)
        )
    }

    private val _memoryExpanded = MutableStateFlow(false)
    val memoryExpanded: StateFlow<Boolean> = _memoryExpanded

    val snapshots: StateFlow<List<VitalsSnapshot>> = getVitals()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val uiState: StateFlow<MonitorUiState> = serviceState.state
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MonitorUiState()
        )

    val memoryBreakdown: StateFlow<ProcessMemoryBreakdown?> = _memoryExpanded
        .flatMapLatest { expanded ->
            if (expanded) getProcessMemoryBreakdown() else emptyFlow()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    fun toggleMemoryExpanded() {
        _memoryExpanded.value = !_memoryExpanded.value
    }
}
