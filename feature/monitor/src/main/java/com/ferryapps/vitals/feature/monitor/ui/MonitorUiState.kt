package com.ferryapps.vitals.feature.monitor.ui

import com.ferryapps.vitals.core.domain.model.MemoryInfo

data class MonitorUiState(
    val cpuPercent: Float = 0f,
    val memory: MemoryInfo = MemoryInfo(totalKb = 0L, availableKb = 0L, usedKb = 0L, usedPercent = 0f),
    val threadCount: Int = 0
)
