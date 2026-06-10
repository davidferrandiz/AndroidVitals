package com.ferryapps.vitals.core.domain.model

data class CpuCoreInfo(
    val index: Int,
    val currentMhz: Int,
    val maxMhz: Int,
    val isOnline: Boolean
)

data class ThermalZone(
    val name: String,
    val temperatureCelsius: Float
)
