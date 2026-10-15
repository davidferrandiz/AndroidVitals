package com.ferryapps.vitals.core.domain

import com.ferryapps.vitals.core.domain.battery.BatteryHistoryMath
import com.ferryapps.vitals.core.domain.battery.BatteryHistoryMath.HOUR_MS
import com.ferryapps.vitals.core.domain.model.BatterySample
import com.ferryapps.vitals.core.domain.model.BatteryStatus
import com.ferryapps.vitals.core.domain.model.PlugType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryHistoryMathTest {

    private val step = 15 * 60_000L

    private fun sample(
        t: Long,
        level: Int,
        status: BatteryStatus,
        counter: Int?,
        temp: Float = 30f,
    ) = BatterySample(
        timestampMs = t,
        levelPercent = level,
        status = status,
        plugType = if (status == BatteryStatus.CHARGING) PlugType.AC else PlugType.NONE,
        temperatureCelsius = temp,
        voltageMillivolts = 4000,
        chargeCounterMah = counter,
    )

    /** Carga de [from] a [to] % con una batería de [capacity] mAh, una muestra cada 15 min. */
    private fun charge(start: Long, from: Int, to: Int, capacity: Int, pointsPerStep: Int = 5): List<BatterySample> =
        (from..to step pointsPerStep).mapIndexed { i, level ->
            sample(start + i * step, level, BatteryStatus.CHARGING, capacity * level / 100)
        }

    private fun discharge(start: Long, from: Int, to: Int, capacity: Int, pointsPerStep: Int = 2): List<BatterySample> =
        (from downTo to step pointsPerStep).mapIndexed { i, level ->
            sample(start + i * step, level, BatteryStatus.DISCHARGING, capacity * level / 100)
        }

    @Test
    fun `three charges give the capacity`() {
        val day = 24 * HOUR_MS
        val samples = charge(0, 30, 80, 4500) + charge(day, 20, 90, 4500) + charge(2 * day, 40, 70, 4500)
        val estimate = BatteryHistoryMath.estimateHealth(samples)
        assertEquals(3, estimate.validSessions)
        assertEquals(4500, estimate.capacityMah)
    }

    @Test
    fun `fewer than three sessions is still calculating`() {
        val estimate = BatteryHistoryMath.estimateHealth(charge(0, 30, 80, 4500) + charge(48 * HOUR_MS, 20, 60, 4500))
        assertNull(estimate.capacityMah)
        assertEquals(2, estimate.validSessions)
    }

    @Test
    fun `short charges do not count`() {
        val estimate = BatteryHistoryMath.estimateHealth(charge(0, 50, 65, 4500))
        assertEquals(0, estimate.candidateSessions)
    }

    @Test
    fun `discharges also estimate the capacity`() {
        val day = 24 * HOUR_MS
        val samples = discharge(0, 90, 60, 5000) + discharge(day, 80, 40, 5000) + discharge(2 * day, 70, 30, 5000)
        assertEquals(5000, BatteryHistoryMath.estimateHealth(samples).capacityMah)
    }

    @Test
    fun `samples at 100 percent are left out of the charge`() {
        val base = charge(0, 40, 95, 4000)
        // Al 100 % el contador sigue subiendo con el nivel fijo: no debe inflar la capacidad.
        val full = (1..4).map { sample(base.last().timestampMs + it * step, 100, BatteryStatus.CHARGING, 4000 + it * 100) }
        val day = 24 * HOUR_MS
        val samples = base + full + charge(day, 30, 90, 4000) + charge(2 * day, 20, 70, 4000)
        assertEquals(4000, BatteryHistoryMath.estimateHealth(samples).capacityMah)
    }

    @Test
    fun `counter that never moves is flagged as unreliable`() {
        val samples = (0 until 5).flatMap { day ->
            (30..90 step 5).mapIndexed { i, level ->
                sample(day * 24 * HOUR_MS + i * step, level, BatteryStatus.CHARGING, 4000)
            }
        }
        val estimate = BatteryHistoryMath.estimateHealth(samples)
        assertNull(estimate.capacityMah)
        assertTrue(estimate.counterUnreliable)
    }

    @Test
    fun `missing counter gives no candidates`() {
        val samples = (30..90 step 5).mapIndexed { i, level -> sample(i * step, level, BatteryStatus.CHARGING, null) }
        val estimate = BatteryHistoryMath.estimateHealth(samples)
        assertEquals(0, estimate.candidateSessions)
        assertFalse(estimate.counterUnreliable)
    }

    @Test
    fun `a long gap splits the charge`() {
        val first = charge(0, 30, 45, 4000)
        val second = charge(first.last().timestampMs + 3 * HOUR_MS, 50, 65, 4000)
        // Juntas serían 35 puntos, pero con 3 h de hueco no se sabe qué pasó en medio.
        assertEquals(0, BatteryHistoryMath.estimateHealth(first + second).candidateSessions)
    }

    @Test
    fun `estimates far from the design capacity are discarded`() {
        val day = 24 * HOUR_MS
        val samples = charge(0, 30, 80, 9000) + charge(day, 20, 90, 9000) + charge(2 * day, 40, 70, 9000)
        val estimate = BatteryHistoryMath.estimateHealth(samples, designCapacityMah = 4000)
        assertEquals(0, estimate.validSessions)
    }

    @Test
    fun `drain rate, charge sessions and max temperature`() {
        val now = 30 * HOUR_MS
        // 4 h descargando de 90 a 74 % (4 %/h) y una carga después.
        val drain = (0..16).map { i ->
            sample(now - 10 * HOUR_MS + i * step, 90 - i, BatteryStatus.DISCHARGING, null, temp = 31f)
        }
        val charging = (1..4).map { i ->
            sample(drain.last().timestampMs + i * step, 74 + i * 5, BatteryStatus.CHARGING, null, temp = 38.5f)
        }
        val stats = BatteryHistoryMath.stats(drain + charging, now)
        assertEquals(4f, stats.drainPercentPerHour!!, 0.01f)
        assertEquals(1, stats.chargeSessions7d)
        assertEquals(38.5f, stats.maxTemperature24hCelsius!!, 0.01f)
    }

    @Test
    fun `less than an hour of discharge has no drain rate`() {
        val samples = (0..3).map { sample(it * step, 80 - it, BatteryStatus.DISCHARGING, null) }
        assertNull(BatteryHistoryMath.stats(samples, 4 * step).drainPercentPerHour)
    }

    @Test
    fun `default AOSP design capacity is ignored`() {
        assertNull(BatteryHistoryMath.plausibleDesignCapacity(1000.0))
        assertNull(BatteryHistoryMath.plausibleDesignCapacity(null))
        assertEquals(5000, BatteryHistoryMath.plausibleDesignCapacity(5000.0))
    }
}
