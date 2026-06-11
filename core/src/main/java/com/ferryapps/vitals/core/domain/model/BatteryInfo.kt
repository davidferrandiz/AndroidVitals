package com.ferryapps.vitals.core.domain.model

data class BatteryInfo(
    val levelPercent: Int,
    val isCharging: Boolean,
    val status: BatteryStatus,
    val health: BatteryHealth,
    val temperatureCelsius: Float,
    val voltageMillivolts: Int,
    val technology: String
)

enum class BatteryStatus {
    CHARGING, DISCHARGING, FULL, NOT_CHARGING, UNKNOWN
}

enum class BatteryHealth {
    GOOD, OVERHEAT, DEAD, OVER_VOLTAGE, COLD, UNKNOWN
}
