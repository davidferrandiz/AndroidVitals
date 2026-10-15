package com.ferryapps.vitals.core.domain

import com.ferryapps.vitals.core.domain.cpu.CpuMath
import com.ferryapps.vitals.core.domain.model.CpuCoreInfo
import com.ferryapps.vitals.core.domain.model.ThermalKind
import org.junit.Assert.assertEquals
import org.junit.Test

class CpuMathTest {

    private fun core(i: Int, cur: Int, min: Int, max: Int, name: String?, online: Boolean = true) =
        CpuCoreInfo(index = i, currentMhz = cur, maxMhz = max, isOnline = online, minMhz = min, coreName = name)

    @Test
    fun `estimated load uses min to max range and counts offline cores as zero`() {
        val cores = listOf(
            core(0, 500, 500, 2000, "Cortex-A55"),   // 0 %
            core(1, 2000, 500, 2000, "Cortex-A55"),  // 100 %
            core(2, 1250, 500, 2000, "Cortex-A55"),  // 50 %
            core(3, 0, 500, 2000, "Cortex-A55", online = false), // 0 %
        )
        assertEquals(37.5f, CpuMath.estimatedLoadPercent(cores), 0.01f)
    }

    @Test
    fun `clusters group by core name and max frequency, strongest first`() {
        val cores = (0..5).map { core(it, 614, 614, 1600, "Cortex-A55") } +
            (6..7).map { core(it, 1200, 614, 2000, "Cortex-A75") }
        val clusters = CpuMath.clusters(cores)
        assertEquals(2, clusters.size)
        assertEquals("Cortex-A75", clusters[0].coreName)
        assertEquals(2, clusters[0].coreCount)
        assertEquals(6, clusters[1].coreCount)
    }

    @Test
    fun `thermal zones from different vendors are classified`() {
        assertEquals(ThermalKind.CPU, CpuMath.classifyThermalZone("mtktscpu"))
        assertEquals(ThermalKind.BOARD, CpuMath.classifyThermalZone("mtktsAP"))
        assertEquals(ThermalKind.BATTERY, CpuMath.classifyThermalZone("mtktsbattery"))
        assertEquals(ThermalKind.CPU, CpuMath.classifyThermalZone("cputop0-thmzone"))
        assertEquals(ThermalKind.SOC, CpuMath.classifyThermalZone("soc-thmzone"))
        assertEquals(ThermalKind.GPU, CpuMath.classifyThermalZone("gpuss-0"))
        assertEquals(ThermalKind.SKIN, CpuMath.classifyThermalZone("skin-therm"))
        assertEquals(ThermalKind.OTHER, CpuMath.classifyThermalZone("charger"))
    }
}
