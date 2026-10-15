package com.ferryapps.vitals.core.domain

import com.ferryapps.vitals.core.domain.alerts.AlertInputs
import com.ferryapps.vitals.core.domain.alerts.AlertRules
import com.ferryapps.vitals.core.domain.alerts.AlertType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertRulesTest {

    private val hour = 3_600_000L
    private val now = 100 * hour

    @Test
    fun `hot battery and low storage both trigger`() {
        val due = AlertRules.due(AlertInputs(46f, 4f), emptyMap(), now)
        assertEquals(listOf(AlertType.BATTERY_HOT, AlertType.STORAGE_LOW), due)
    }

    @Test
    fun `normal values do not trigger`() {
        assertTrue(AlertRules.due(AlertInputs(38f, 40f), emptyMap(), now).isEmpty())
        assertTrue(AlertRules.due(AlertInputs(44.9f, 10f), emptyMap(), now).isEmpty())
    }

    @Test
    fun `missing data never triggers`() {
        assertTrue(AlertRules.due(AlertInputs(null, null), emptyMap(), now).isEmpty())
    }

    @Test
    fun `cooldown avoids repeating the same alert`() {
        val last = mapOf(AlertType.BATTERY_HOT to now - 2 * hour, AlertType.STORAGE_LOW to now - 24 * hour)
        assertTrue(AlertRules.due(AlertInputs(47f, 5f), last, now).isEmpty())
    }

    @Test
    fun `alert comes back after the cooldown`() {
        val last = mapOf(AlertType.BATTERY_HOT to now - 7 * hour, AlertType.STORAGE_LOW to now - 80 * hour)
        assertEquals(listOf(AlertType.BATTERY_HOT, AlertType.STORAGE_LOW), AlertRules.due(AlertInputs(47f, 5f), last, now))
    }

    @Test
    fun `clock moved backwards does not block alerts forever`() {
        val last = mapOf(AlertType.BATTERY_HOT to now + 50 * hour)
        assertEquals(listOf(AlertType.BATTERY_HOT), AlertRules.due(AlertInputs(47f, 50f), last, now))
    }
}
