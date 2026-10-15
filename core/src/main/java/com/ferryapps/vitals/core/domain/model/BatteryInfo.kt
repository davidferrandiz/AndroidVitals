package com.ferryapps.vitals.core.domain.model

data class BatteryInfo(
    val levelPercent: Int,
    val isCharging: Boolean,
    val status: BatteryStatus,
    val health: BatteryHealth,
    val temperatureCelsius: Float,
    val voltageMillivolts: Int,
    val technology: String,
    val plugType: PlugType = PlugType.NONE,
    /** Corriente instantánea en mA, positiva al cargar. Null si el dispositivo no la expone. */
    val currentNowMa: Int? = null,
    /** Corriente media en mA, positiva al cargar. */
    val currentAverageMa: Int? = null,
    /** Carga restante en mAh según el contador del sistema. */
    val chargeCounterMah: Int? = null,
    /** Tiempo hasta carga completa. Solo cargando y en API 28+. */
    val chargeTimeRemainingMs: Long? = null,
    /** Ciclos de carga. Solo en API 34+ y si el fabricante lo expone. */
    val cycleCount: Int? = null,
) {
    /** Potencia en vatios, positiva al cargar y negativa al descargar. */
    val powerWatts: Float?
        get() = currentNowMa?.let { (it / 1000f) * (voltageMillivolts / 1000f) }

    /** Autonomía estimada con el consumo actual. Solo descargando. */
    val estimatedRemainingMs: Long?
        get() {
            val counter = chargeCounterMah ?: return null
            val current = currentNowMa ?: return null
            if (isCharging || current >= 0 || counter <= 0) return null
            val hours = counter.toFloat() / -current
            return (hours * 3_600_000L).toLong()
        }
}

enum class BatteryStatus {
    CHARGING, DISCHARGING, FULL, NOT_CHARGING, UNKNOWN
}

enum class BatteryHealth {
    GOOD, OVERHEAT, DEAD, OVER_VOLTAGE, COLD, UNKNOWN
}

enum class PlugType {
    NONE, AC, USB, WIRELESS, DOCK
}
