package com.ferryapps.vitals.core.domain

import com.ferryapps.vitals.core.domain.alerts.AlertInputs
import com.ferryapps.vitals.core.domain.alerts.AlertRules
import com.ferryapps.vitals.core.domain.alerts.AlertState
import com.ferryapps.vitals.core.domain.alerts.AlertType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertRulesTest {

    private val hour = 3_600_000L
    private val now = 100 * hour
    private val afternoon = 16
    private val night = 3

    /** Estado tras una lectura caliente previa. */
    private val hotOnce = AlertState(batteryHotStreak = 1)

    @Test
    fun `one hot reading is not enough`() {
        val decision = AlertRules.decide(AlertInputs(47f, 50f), AlertState(), now, afternoon)
        assertTrue(decision.show.isEmpty())
        assertEquals(1, decision.batteryHotStreak)
    }

    @Test
    fun `two hot readings in a row trigger the battery alert`() {
        val decision = AlertRules.decide(AlertInputs(47f, 50f), hotOnce, now, afternoon)
        assertEquals(listOf(AlertType.BATTERY_HOT), decision.show)
    }

    @Test
    fun `a normal reading resets the streak`() {
        val decision = AlertRules.decide(AlertInputs(43f, 50f), hotOnce, now, afternoon)
        assertTrue(decision.show.isEmpty())
        assertEquals(0, decision.batteryHotStreak)
    }

    @Test
    fun `missing temperature keeps the streak`() {
        assertEquals(1, AlertRules.decide(AlertInputs(null, 50f), hotOnce, now, afternoon).batteryHotStreak)
    }

    @Test
    fun `low storage triggers during the day`() {
        assertEquals(listOf(AlertType.STORAGE_LOW), AlertRules.decide(AlertInputs(30f, 5f), AlertState(), now, afternoon).show)
        assertTrue(AlertRules.decide(AlertInputs(30f, 10f), AlertState(), now, afternoon).show.isEmpty())
    }

    @Test
    fun `low storage waits until morning`() {
        assertTrue(AlertRules.decide(AlertInputs(30f, 5f), AlertState(), now, night).show.isEmpty())
        assertTrue(AlertRules.decide(AlertInputs(30f, 5f), AlertState(), now, 22).show.isEmpty())
        assertEquals(listOf(AlertType.STORAGE_LOW), AlertRules.decide(AlertInputs(30f, 5f), AlertState(), now, 9).show)
    }

    @Test
    fun `hot battery at night is shown silently`() {
        val decision = AlertRules.decide(AlertInputs(47f, 50f), hotOnce, now, night)
        assertEquals(listOf(AlertType.BATTERY_HOT), decision.show)
        assertTrue(decision.silent)
        assertFalse(AlertRules.decide(AlertInputs(47f, 50f), hotOnce, now, afternoon).silent)
    }

    @Test
    fun `missing data never triggers`() {
        assertTrue(AlertRules.decide(AlertInputs(null, null), AlertState(), now, afternoon).show.isEmpty())
    }

    @Test
    fun `cooldown avoids repeating the same alert`() {
        val state = AlertState(
            lastFiredMs = mapOf(AlertType.BATTERY_HOT to now - 6 * hour, AlertType.STORAGE_LOW to now - 3 * 24 * hour),
            batteryHotStreak = 1,
        )
        assertTrue(AlertRules.decide(AlertInputs(47f, 5f), state, now, afternoon).show.isEmpty())
    }

    @Test
    fun `alert comes back after the cooldown`() {
        val state = AlertState(
            lastFiredMs = mapOf(AlertType.BATTERY_HOT to now - 13 * hour, AlertType.STORAGE_LOW to now - 8 * 24 * hour),
            batteryHotStreak = 1,
        )
        assertEquals(
            listOf(AlertType.BATTERY_HOT, AlertType.STORAGE_LOW),
            AlertRules.decide(AlertInputs(47f, 5f), state, now, afternoon).show
        )
    }

    @Test
    fun `solved problems dismiss their notification`() {
        assertEquals(
            setOf(AlertType.BATTERY_HOT, AlertType.STORAGE_LOW),
            AlertRules.decide(AlertInputs(38f, 15f), AlertState(), now, afternoon).dismiss
        )
        // Entre 40 y 45 °C, o entre 10 y 12 % libre, no se quita: evita que parpadee.
        assertTrue(AlertRules.decide(AlertInputs(42f, 11f), AlertState(), now, afternoon).dismiss.isEmpty())
    }

    @Test
    fun `clock moved backwards does not block alerts forever`() {
        val state = AlertState(lastFiredMs = mapOf(AlertType.BATTERY_HOT to now + 50 * hour), batteryHotStreak = 1)
        assertEquals(listOf(AlertType.BATTERY_HOT), AlertRules.decide(AlertInputs(47f, 50f), state, now, afternoon).show)
    }
}
