package com.ferryapps.vitals.core.domain

import com.ferryapps.vitals.core.domain.battery.BatteryNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryNormalizerTest {

    @Test
    fun `microamps as documented are converted to mA`() {
        assertEquals(2880, BatteryNormalizer.currentToMa(2_880_000, isCharging = true))
    }

    @Test
    fun `milliamps reported by some MediaTek devices are kept`() {
        assertEquals(-380, BatteryNormalizer.currentToMa(-380, isCharging = false))
    }

    @Test
    fun `inverted sign is fixed using the charging state`() {
        assertEquals(2880, BatteryNormalizer.currentToMa(-2_880_000, isCharging = true))
        assertEquals(-460, BatteryNormalizer.currentToMa(460_000, isCharging = false))
    }

    @Test
    fun `unsupported values return null`() {
        assertNull(BatteryNormalizer.currentToMa(0, isCharging = true))
        assertNull(BatteryNormalizer.currentToMa(Int.MIN_VALUE.toLong(), isCharging = true))
        assertNull(BatteryNormalizer.currentToMa(Long.MIN_VALUE, isCharging = null))
    }

    @Test
    fun `charge counter in microamp-hours or milliamp-hours`() {
        assertEquals(3210, BatteryNormalizer.chargeCounterToMah(3_210_000))
        assertEquals(3210, BatteryNormalizer.chargeCounterToMah(3210))
        assertNull(BatteryNormalizer.chargeCounterToMah(0))
    }
}
