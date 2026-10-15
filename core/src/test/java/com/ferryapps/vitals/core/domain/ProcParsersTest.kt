package com.ferryapps.vitals.core.domain

import com.ferryapps.vitals.core.domain.parser.ProcParsers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProcParsersTest {

    @Test
    fun `meminfo keeps every key in kB`() {
        val text = """
            MemTotal:        1903212 kB
            MemFree:           71236 kB
            MemAvailable:     214420 kB
            Cached:           402316 kB
            SwapTotal:       1427404 kB
            SwapFree:         602112 kB
        """.trimIndent()
        val values = ProcParsers.parseMemInfo(text)
        assertEquals(1903212L, values["MemTotal"])
        assertEquals(214420L, values["MemAvailable"])
        assertEquals(1427404L, values["SwapTotal"])
        assertEquals(602112L, values["SwapFree"])
    }

    @Test
    fun `cpufreq in kHz becomes MHz and garbage becomes zero`() {
        assertEquals(2910, ProcParsers.parseFreqMhz("2910000\n"))
        assertEquals(0, ProcParsers.parseFreqMhz("<unsupported>"))
        assertEquals(0, ProcParsers.parseFreqMhz(null))
    }

    @Test
    fun `thermal zone accepts millicelsius, decicelsius and celsius`() {
        assertEquals(43.5f, ProcParsers.parseThermalCelsius("43500")!!, 0.01f)
        assertEquals(31.5f, ProcParsers.parseThermalCelsius("315")!!, 0.01f)
        assertEquals(38f, ProcParsers.parseThermalCelsius("38")!!, 0.01f)
        assertNull(ProcParsers.parseThermalCelsius("-273000"))
        assertNull(ProcParsers.parseThermalCelsius("999999"))
    }

    @Test
    fun `cpuinfo parts per processor on a MediaTek Helio G85`() {
        val text = """
            processor	: 0
            BogoMIPS	: 26.00
            CPU implementer	: 0x41
            CPU part	: 0xd05

            processor	: 6
            CPU implementer	: 0x41
            CPU part	: 0xd0a

            Hardware	: MT6769Z
        """.trimIndent()
        val parts = ProcParsers.parseCpuParts(text)
        assertEquals(0x41 to 0xd05, parts[0])
        assertEquals(0x41 to 0xd0a, parts[6])
        assertEquals("MT6769Z", ProcParsers.parseCpuInfoHardware(text))
    }

    @Test
    fun `threads line of proc self status`() {
        val text = "Name:\tcom.ferryapps.vitals\nState:\tS (sleeping)\nThreads:\t34\nSigQ:\t0/7464\n"
        assertEquals(34, ProcParsers.parseThreads(text))
        assertEquals(0, ProcParsers.parseThreads("Name:\tx\n"))
    }
}
