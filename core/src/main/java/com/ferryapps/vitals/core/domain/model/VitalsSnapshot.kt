package com.ferryapps.vitals.core.domain.model

data class VitalsSnapshot(
    val cpuUsagePercent: Float,
    val ramUsedMb: Long,
    val ramTotalMb: Long,
    val threadCount: Int,
    val timestampMs: Long
)
