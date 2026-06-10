package com.ferryapps.vitals.core.domain.model

data class BatteryInfo(
    val levelPercent: Int,
    val isCharging: Boolean,
    val status: String,          // "Cargando", "Descargando", "Llena", etc.
    val health: String,          // "Buena", "Sobrecalentada", etc.
    val temperatureCelsius: Float,
    val voltageMillivolts: Int,
    val technology: String       // "Li-ion", etc.
)
