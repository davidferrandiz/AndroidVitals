package com.ferryapps.vitals.core.domain.model

data class MemoryInfo(
    val totalKb: Long,
    val availableKb: Long,
    val usedKb: Long,
    val usedPercent: Float
)
