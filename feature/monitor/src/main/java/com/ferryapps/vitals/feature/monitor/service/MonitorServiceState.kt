package com.ferryapps.vitals.feature.monitor.service

import com.ferryapps.vitals.feature.monitor.ui.MonitorUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonitorServiceState @Inject constructor() {
    private val _state = MutableStateFlow(MonitorUiState())
    val state: StateFlow<MonitorUiState> = _state

    fun update(newState: MonitorUiState) {
        _state.value = newState
    }
}
