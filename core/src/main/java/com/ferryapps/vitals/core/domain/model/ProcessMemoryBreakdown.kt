package com.ferryapps.vitals.core.domain.model

data class ProcessMemoryBreakdown(
    val pid: Int,
    val totalPssKb: Int,
    val javaHeapKb: Int,
    val nativeHeapKb: Int,
    val codeKb: Int,
    val stackKb: Int,
    val graphicsKb: Int,
    val otherKb: Int
)
