package com.ferryapps.vitals.core.domain.alerts

/**
 * Avisos que VitalsWorker puede mostrar. Solo los que el usuario puede resolver: no hay aviso
 * de memoria baja porque en móviles de 2–3 GB salta casi a diario y Android ya cierra apps solo.
 *
 * @param cooldownMs tiempo mínimo entre dos avisos del mismo tipo.
 */
enum class AlertType(val cooldownMs: Long) {
    BATTERY_HOT(6 * HOUR_MS),
    STORAGE_LOW(3 * 24 * HOUR_MS),
}

private const val HOUR_MS = 3_600_000L

/** Lo que mira el worker en cada pasada. Null = el dato no está disponible. */
data class AlertInputs(
    val batteryTemperatureCelsius: Float?,
    val storageFreePercent: Float?,
)

object AlertRules {

    /** A partir de 45 °C la batería se degrada deprisa y Android empieza a limitar la carga. */
    const val HOT_BATTERY_CELSIUS = 45f

    /** Mismo umbral que el aviso del Panel. */
    const val LOW_STORAGE_PERCENT = 10f

    fun triggered(inputs: AlertInputs): Set<AlertType> = buildSet {
        val temperature = inputs.batteryTemperatureCelsius
        if (temperature != null && temperature >= HOT_BATTERY_CELSIUS) add(AlertType.BATTERY_HOT)
        val free = inputs.storageFreePercent
        if (free != null && free < LOW_STORAGE_PERCENT) add(AlertType.STORAGE_LOW)
    }

    /** Los avisos que tocan ahora: se cumplen y su último aviso es más antiguo que el cooldown. */
    fun due(inputs: AlertInputs, lastFiredMs: Map<AlertType, Long>, nowMs: Long): List<AlertType> =
        triggered(inputs).filter { type ->
            val last = lastFiredMs[type] ?: return@filter true
            nowMs - last >= type.cooldownMs || nowMs < last // reloj cambiado hacia atrás
        }.sortedBy { it.ordinal }
}
