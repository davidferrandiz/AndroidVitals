package com.ferryapps.vitals.core.domain.model

data class MemoryInfo(
    val totalKb: Long,
    val availableKb: Long,
    val usedKb: Long,
    val usedPercent: Float,
    /** Android considera que hay poca memoria y empieza a cerrar apps en segundo plano. */
    val lowMemory: Boolean = false,
    /** Umbral de memoria disponible por debajo del cual `lowMemory` pasa a true. */
    val thresholdKb: Long = 0L,
    val swapTotalKb: Long = 0L,
    val swapFreeKb: Long = 0L,
    val cachedKb: Long = 0L,
)
