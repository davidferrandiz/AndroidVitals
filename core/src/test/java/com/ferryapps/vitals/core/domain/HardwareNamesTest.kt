package com.ferryapps.vitals.core.domain

import com.ferryapps.vitals.core.domain.hardware.HardwareNames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HardwareNamesTest {

    @Test
    fun `MediaTek code is translated to its commercial name`() {
        val soc = HardwareNames.resolveSoc("Mediatek", "MT6769Z")
        assertEquals("MediaTek Helio G85", soc.displayName)
        assertEquals("MT6769Z", soc.code)
    }

    @Test
    fun `longest prefix wins`() {
        assertEquals("MediaTek Helio G35", HardwareNames.resolveSoc(null, "MT6765G").displayName)
        assertEquals("MediaTek Helio P35", HardwareNames.resolveSoc(null, "MT6765V/CB").displayName)
    }

    @Test
    fun `Unisoc and Qualcomm codes`() {
        assertEquals("Unisoc T606", HardwareNames.resolveSoc("Spreadtrum", "T606").displayName)
        assertEquals("Qualcomm Snapdragon 8 Gen 2", HardwareNames.resolveSoc("QTI", "SM8550").displayName)
    }

    @Test
    fun `unknown SoC falls back to vendor plus raw model`() {
        val soc = HardwareNames.resolveSoc("Google", "Tensor G3")
        assertEquals("Google Tensor G3", soc.displayName)
        assertNull(soc.code)
    }

    @Test
    fun `later candidates are tried when the first is missing`() {
        assertEquals("Samsung Exynos 7885", HardwareNames.resolveSoc(null, null, "", "exynos7885").displayName)
    }

    @Test
    fun `ARM core part numbers`() {
        assertEquals("Cortex-A55", HardwareNames.cpuCoreName(0x41, 0xd05))
        assertEquals("Cortex-X3", HardwareNames.cpuCoreName(0x41, 0xd4e))
        assertNull(HardwareNames.cpuCoreName(0x41, 0xfff))
    }
}
