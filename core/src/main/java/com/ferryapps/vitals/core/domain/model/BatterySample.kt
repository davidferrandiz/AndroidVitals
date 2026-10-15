package com.ferryapps.vitals.core.domain.model

/** Lectura de batería guardada por VitalsWorker cada ~15 min. Solo vive en el dispositivo. */
data class BatterySample(
    val timestampMs: Long,
    val levelPercent: Int,
    val status: BatteryStatus,
    val plugType: PlugType,
    val temperatureCelsius: Float,
    val voltageMillivolts: Int,
    /** Contador de carga del sistema en mAh. Null si el móvil no lo expone. */
    val chargeCounterMah: Int?,
) {
    val isCharging: Boolean get() = status == BatteryStatus.CHARGING

    companion object {
        fun from(info: BatteryInfo, timestampMs: Long) = BatterySample(
            timestampMs = timestampMs,
            levelPercent = info.levelPercent,
            status = info.status,
            plugType = info.plugType,
            temperatureCelsius = info.temperatureCelsius,
            voltageMillivolts = info.voltageMillivolts,
            chargeCounterMah = info.chargeCounterMah,
        )
    }
}

/**
 * Capacidad estimada a partir de cuánto cambia el contador de carga frente al porcentaje.
 *
 * @param capacityMah mediana de las estimaciones válidas; null hasta tener [BatteryHealthEstimate.MIN_SESSIONS].
 * @param validSessions tramos de carga/descarga que han dado una estimación coherente.
 * @param candidateSessions tramos con suficiente recorrido de nivel, sean coherentes o no.
 */
data class BatteryHealthEstimate(
    val capacityMah: Int?,
    val validSessions: Int,
    val candidateSessions: Int,
) {
    /** Muchos tramos largos y ninguno coherente: el contador del móvil no sirve para estimar. */
    val counterUnreliable: Boolean
        get() = validSessions == 0 && candidateSessions >= UNRELIABLE_AFTER

    companion object {
        const val MIN_SESSIONS = 3
        const val UNRELIABLE_AFTER = 5
        val EMPTY = BatteryHealthEstimate(capacityMah = null, validSessions = 0, candidateSessions = 0)
    }
}

/** Resumen del historial para la pantalla Batería. */
data class BatteryHistoryStats(
    /** Media de % por hora perdidos descargando en las últimas 24 h. */
    val drainPercentPerHour: Float?,
    /** Veces que se ha empezado a cargar en los últimos 7 días. */
    val chargeSessions7d: Int,
    /** Temperatura máxima de la batería en las últimas 24 h. */
    val maxTemperature24hCelsius: Float?,
)

data class BatteryHistory(
    /** Muestras de las últimas 24 h, de la más antigua a la más reciente. */
    val last24h: List<BatterySample>,
    val stats: BatteryHistoryStats,
    val health: BatteryHealthEstimate,
    /** Capacidad de diseño según el perfil de energía del fabricante, si es creíble. */
    val designCapacityMah: Int?,
) {
    /** Salud estimada (capacidad estimada / diseño), limitada a 100 %. */
    val healthPercent: Int?
        get() {
            val estimated = health.capacityMah ?: return null
            val design = designCapacityMah ?: return null
            return (estimated * 100 / design).coerceAtMost(100)
        }
}
