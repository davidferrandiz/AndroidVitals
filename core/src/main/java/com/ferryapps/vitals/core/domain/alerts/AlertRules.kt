package com.ferryapps.vitals.core.domain.alerts

/**
 * Avisos que VitalsWorker puede mostrar, pensados para molestar lo mínimo:
 * - solo problemas que el usuario puede resolver (no hay aviso de memoria baja: en móviles de
 *   2–3 GB saltaría casi a diario y Android ya cierra apps solo);
 * - la batería caliente tiene que verse en dos lecturas seguidas (~15 min), para no avisar por un
 *   pico de unos minutos jugando;
 * - cada aviso tiene un tiempo mínimo entre repeticiones ([cooldownMs]);
 * - por la noche no suena nada y el de espacio espera a la mañana;
 * - cuando el problema se resuelve, la notificación se quita sola.
 */
enum class AlertType(val cooldownMs: Long) {
    BATTERY_HOT(12 * HOUR_MS),
    STORAGE_LOW(7 * 24 * HOUR_MS),
}

private const val HOUR_MS = 3_600_000L

/** Lo que mira el worker en cada pasada. Null = el dato no está disponible. */
data class AlertInputs(
    val batteryTemperatureCelsius: Float?,
    val storageFreePercent: Float?,
)

/** Lo que se guarda entre pasadas del worker. */
data class AlertState(
    val lastFiredMs: Map<AlertType, Long> = emptyMap(),
    /** Lecturas seguidas con la batería por encima de [AlertRules.HOT_BATTERY_CELSIUS]. */
    val batteryHotStreak: Int = 0,
)

data class AlertDecision(
    /** Avisos que hay que mostrar ahora. */
    val show: List<AlertType>,
    /** Problemas resueltos: si su notificación sigue en la barra, se quita. */
    val dismiss: Set<AlertType>,
    /** Mostrar sin sonido ni vibración (horas de descanso). */
    val silent: Boolean,
    val batteryHotStreak: Int,
)

object AlertRules {

    /** A partir de 45 °C la batería se degrada deprisa y Android empieza a limitar la carga. */
    const val HOT_BATTERY_CELSIUS = 45f

    /** Por debajo se da por enfriada. El hueco con 45 °C evita quitar y poner el aviso a 44,9/45,1 °C. */
    const val COOL_BATTERY_CELSIUS = 40f

    const val HOT_READINGS_REQUIRED = 2

    /** Mismo umbral que el aviso del Panel. */
    const val LOW_STORAGE_PERCENT = 10f

    const val STORAGE_RECOVERED_PERCENT = 12f

    /** Horas de descanso, en hora local: desde las 22:00 hasta las 9:00. */
    const val QUIET_FROM_HOUR = 22
    const val QUIET_UNTIL_HOUR = 9

    fun isQuietHour(hour: Int): Boolean = hour >= QUIET_FROM_HOUR || hour < QUIET_UNTIL_HOUR

    fun decide(inputs: AlertInputs, state: AlertState, nowMs: Long, localHour: Int): AlertDecision {
        val temperature = inputs.batteryTemperatureCelsius
        val free = inputs.storageFreePercent
        val quiet = isQuietHour(localHour)

        val streak = when {
            temperature == null -> state.batteryHotStreak
            temperature >= HOT_BATTERY_CELSIUS -> (state.batteryHotStreak + 1).coerceAtMost(HOT_READINGS_REQUIRED)
            else -> 0
        }

        val wanted = buildList {
            if (streak >= HOT_READINGS_REQUIRED) add(AlertType.BATTERY_HOT)
            // El espacio no corre prisa: de noche espera a la siguiente pasada de día.
            if (free != null && free < LOW_STORAGE_PERCENT && !quiet) add(AlertType.STORAGE_LOW)
        }
        val show = wanted.filter { cooledDown(it, state.lastFiredMs[it], nowMs) }

        val dismiss = buildSet {
            if (temperature != null && temperature < COOL_BATTERY_CELSIUS) add(AlertType.BATTERY_HOT)
            if (free != null && free >= STORAGE_RECOVERED_PERCENT) add(AlertType.STORAGE_LOW)
        }

        return AlertDecision(show = show, dismiss = dismiss, silent = quiet, batteryHotStreak = streak)
    }

    private fun cooledDown(type: AlertType, lastMs: Long?, nowMs: Long): Boolean =
        lastMs == null || nowMs - lastMs >= type.cooldownMs || nowMs < lastMs // reloj cambiado hacia atrás
}
