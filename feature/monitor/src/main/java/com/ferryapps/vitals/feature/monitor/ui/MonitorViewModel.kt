package com.ferryapps.vitals.feature.monitor.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ferryapps.vitals.core.domain.model.VitalsSnapshot
import com.ferryapps.vitals.core.domain.usecase.GetVitalsUseCase
import com.ferryapps.vitals.feature.monitor.service.MonitorServiceState
import com.ferryapps.vitals.feature.monitor.service.VitalsForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MonitorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    getVitals: GetVitalsUseCase,
    serviceState: MonitorServiceState
) : ViewModel() {

    init {
        context.startForegroundService(
            Intent(context, VitalsForegroundService::class.java)
        )
    }

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
}
