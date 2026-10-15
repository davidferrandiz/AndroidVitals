package com.ferryapps.vitals.core.domain.parser

/**
 * Parsers puros de ficheros de /proc y /sys. Sin dependencias de Android para poder
 * testearlos con ficheros de ejemplo de distintos fabricantes.
 */
object ProcParsers {

    /** `/proc/meminfo` → mapa clave → kB. Ej.: "MemTotal:  8048964 kB" → ("MemTotal", 8048964). */
    fun parseMemInfo(text: String): Map<String, Long> {
        val values = mutableMapOf<String, Long>()
        text.lineSequence().forEach { line ->
            val colon = line.indexOf(':')
            if (colon <= 0) return@forEach
            val key = line.substring(0, colon).trim()
            val number = line.substring(colon + 1).trim().substringBefore(' ').toLongOrNull()
            if (number != null) values[key] = number
        }
        return values
    }

    /** Frecuencia de cpufreq en kHz → MHz. Devuelve 0 si el texto no es un número. */
    fun parseFreqMhz(text: String?): Int =
        ((text?.trim()?.toLongOrNull() ?: 0L) / 1000L).toInt()

    /**
     * Temperatura de `/sys/class/thermal/thermal_zoneN/temp`. Puede venir en milicélsius, décimas o célsius.
     * Devuelve null si la lectura es absurda.
     */
    fun parseThermalCelsius(text: String?): Float? {
        val raw = text?.trim()?.toLongOrNull() ?: return null
        if (raw <= 0L) return null
        val celsius = when {
            raw > 1000L -> raw / 1000f
            raw > 150L -> raw / 10f
            else -> raw.toFloat()
        }
        return celsius.takeIf { it in 1f..150f }
    }

    /**
     * `/proc/cpuinfo` → por cada procesador, su (implementer, part).
     * Los kernels arm64 listan un bloque por núcleo separado por línea en blanco.
     */
    fun parseCpuParts(text: String): Map<Int, Pair<Int, Int>> {
        val result = mutableMapOf<Int, Pair<Int, Int>>()
        var processor: Int? = null
        var implementer: Int? = null
        var part: Int? = null
        fun flush() {
            val p = processor
            val i = implementer
            val pt = part
            if (p != null && i != null && pt != null) result[p] = i to pt
            processor = null
            implementer = null
            part = null
        }
        text.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) {
                flush()
                return@forEach
            }
            val key = line.substringBefore(':').trim().lowercase()
            val value = line.substringAfter(':', "").trim()
            when (key) {
                "processor" -> {
                    flush()
                    processor = value.toIntOrNull()
                }
                "cpu implementer" -> implementer = parseHex(value)
                "cpu part" -> part = parseHex(value)
            }
        }
        flush()
        return result
    }

    /** Línea "Hardware" de `/proc/cpuinfo`, presente en kernels antiguos (ej. "MT6769Z"). */
    fun parseCpuInfoHardware(text: String): String? =
        text.lineSequence()
            .firstOrNull { it.trim().lowercase().startsWith("hardware") }
            ?.substringAfter(':', "")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    /** Línea "Threads:" de `/proc/self/status`. */
    fun parseThreads(text: String): Int =
        text.lineSequence()
            .firstOrNull { it.startsWith("Threads:") }
            ?.substringAfter(':')
            ?.trim()
            ?.toIntOrNull() ?: 0

    private fun parseHex(value: String): Int? =
        value.removePrefix("0x").removePrefix("0X").toIntOrNull(16)
}
