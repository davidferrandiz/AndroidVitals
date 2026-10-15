package com.ferryapps.vitals.core.domain.battery

import com.ferryapps.vitals.core.domain.model.BatteryHealthEstimate
import com.ferryapps.vitals.core.domain.model.BatteryHistoryStats
import com.ferryapps.vitals.core.domain.model.BatterySample
import com.ferryapps.vitals.core.domain.model.BatteryStatus
import com.ferryapps.vitals.core.domain.model.PlugType
import kotlin.math.abs

/**
 * Cálculos puros sobre el historial de batería (sin Android, con tests en core/src/test).
 *
 * La capacidad se estima por tramos: mientras el móvil carga (o descarga) sin interrupción,
 * capacidad ≈ Δcontador(mAh) × 100 / Δnivel(%). Se usa la mediana de varios tramos porque
 * el nivel es un entero y cada tramo tiene un error de ±1 punto.
 */
object BatteryHistoryMath {

    const val HOUR_MS = 3_600_000L
    const val DAY_MS = 24 * HOUR_MS

    /** Recorrido mínimo de nivel en un tramo. Con 20 puntos el error por redondeo es ≤ 5 %. */
    const val MIN_LEVEL_DELTA = 20

    /** Cargando, el WorkManager rara vez se retrasa; descargando, Doze puede aplazarlo horas. */
    private const val MAX_GAP_CHARGING_MS = 90 * 60_000L
    private const val MAX_GAP_DISCHARGING_MS = 3 * HOUR_MS

    private const val MIN_PLAUSIBLE_MAH = 800
    private const val MAX_PLAUSIBLE_MAH = 15_000
    private const val MAX_ESTIMATES_USED = 10

    private enum class Direction { CHARGING, DISCHARGING }

    private fun BatterySample.direction(): Direction? = when {
        status == BatteryStatus.CHARGING -> Direction.CHARGING
        status == BatteryStatus.DISCHARGING -> Direction.DISCHARGING
        status == BatteryStatus.NOT_CHARGING && plugType == PlugType.NONE -> Direction.DISCHARGING
        else -> null
    }

    /**
     * Tramos continuos en una misma dirección. Se corta si cambia el estado, si hay un hueco
     * demasiado largo o si el nivel va al revés (una carga corta que el worker no llegó a ver).
     * Al 100 % el nivel se queda fijo mientras el contador sigue subiendo, así que esas
     * muestras no entran en los tramos de carga.
     */
    private fun runs(samples: List<BatterySample>): List<Pair<Direction, List<BatterySample>>> {
        val result = mutableListOf<Pair<Direction, List<BatterySample>>>()
        var current = mutableListOf<BatterySample>()
        var currentDirection: Direction? = null

        fun close() {
            val direction = currentDirection
            if (direction != null && current.size >= 2) result += direction to current
            current = mutableListOf()
            currentDirection = null
        }

        for (sample in samples.sortedBy { it.timestampMs }) {
            val direction = sample.direction()
            val usable = direction != null && !(direction == Direction.CHARGING && sample.levelPercent >= 100)
            if (!usable) {
                close()
                continue
            }
            val last = current.lastOrNull()
            val continues = last != null && direction == currentDirection && run {
                val gap = sample.timestampMs - last.timestampMs
                val maxGap = if (direction == Direction.CHARGING) MAX_GAP_CHARGING_MS else MAX_GAP_DISCHARGING_MS
                val levelOk = if (direction == Direction.CHARGING) {
                    sample.levelPercent >= last.levelPercent
                } else {
                    sample.levelPercent <= last.levelPercent
                }
                gap in 1..maxGap && levelOk
            }
            if (!continues) close()
            currentDirection = direction
            current += sample
        }
        close()
        return result
    }

    /**
     * @param designCapacityMah si se conoce, descarta estimaciones absurdas (< 40 % o > 130 %).
     */
    fun estimateHealth(samples: List<BatterySample>, designCapacityMah: Int? = null): BatteryHealthEstimate {
        var candidates = 0
        val estimates = mutableListOf<Int>()
        for ((direction, run) in runs(samples)) {
            val withCounter = run.filter { it.chargeCounterMah != null }
            val first = withCounter.firstOrNull() ?: continue
            val last = withCounter.last()
            val levelDelta = abs(last.levelPercent - first.levelPercent)
            if (levelDelta < MIN_LEVEL_DELTA) continue
            candidates++

            val counterDelta = (last.chargeCounterMah ?: continue) - (first.chargeCounterMah ?: continue)
            val consistent = when (direction) {
                Direction.CHARGING -> counterDelta > 0
                Direction.DISCHARGING -> counterDelta < 0
            }
            if (!consistent) continue

            val estimate = abs(counterDelta) * 100 / levelDelta
            if (estimate !in MIN_PLAUSIBLE_MAH..MAX_PLAUSIBLE_MAH) continue
            if (designCapacityMah != null && estimate !in (designCapacityMah * 40 / 100)..(designCapacityMah * 130 / 100)) continue
            estimates += estimate
        }
        val recent = estimates.takeLast(MAX_ESTIMATES_USED)
        val capacity = if (recent.size >= BatteryHealthEstimate.MIN_SESSIONS) median(recent) else null
        return BatteryHealthEstimate(capacityMah = capacity, validSessions = estimates.size, candidateSessions = candidates)
    }

    fun stats(samples: List<BatterySample>, nowMs: Long): BatteryHistoryStats {
        val sorted = samples.sortedBy { it.timestampMs }
        val last24h = sorted.filter { it.timestampMs >= nowMs - DAY_MS }

        var drainedPoints = 0
        var drainMs = 0L
        for ((direction, run) in runs(last24h)) {
            if (direction != Direction.DISCHARGING) continue
            drainedPoints += run.first().levelPercent - run.last().levelPercent
            drainMs += run.last().timestampMs - run.first().timestampMs
        }
        val drainRate = if (drainMs >= HOUR_MS) drainedPoints * HOUR_MS.toFloat() / drainMs else null

        val week = sorted.filter { it.timestampMs >= nowMs - 7 * DAY_MS }
        var sessions = 0
        week.forEachIndexed { index, sample ->
            if (!sample.isCharging) return@forEachIndexed
            val previous = week.getOrNull(index - 1)
            val newSession = previous == null ||
                !previous.isCharging ||
                sample.timestampMs - previous.timestampMs > MAX_GAP_CHARGING_MS
            if (newSession) sessions++
        }

        return BatteryHistoryStats(
            drainPercentPerHour = drainRate,
            chargeSessions7d = sessions,
            maxTemperature24hCelsius = last24h.maxOfOrNull { it.temperatureCelsius },
        )
    }

    /** Capacidad de diseño creíble. Muchos MediaTek devuelven el valor por defecto de AOSP (1.000 mAh). */
    fun plausibleDesignCapacity(mah: Double?): Int? {
        val value = mah?.takeIf { !it.isNaN() }?.toInt() ?: return null
        return value.takeIf { it in 1_500..12_000 }
    }

    private fun median(values: List<Int>): Int {
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2
    }
}
