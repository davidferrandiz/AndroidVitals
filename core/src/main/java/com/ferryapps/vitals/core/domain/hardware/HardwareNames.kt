package com.ferryapps.vitals.core.domain.hardware

/** Nombre comercial de un SoC y el código que devuelve el sistema (ej. "Helio G85" / "MT6769Z"). */
data class SocName(
    val displayName: String,
    /** Código tal como lo da Android, o null si coincide con el nombre mostrado. */
    val code: String?,
    val vendor: String?,
)

/**
 * Tablas de nombres. Android devuelve códigos internos (`Build.SOC_MODEL`, `Build.HARDWARE`,
 * "CPU part" de /proc/cpuinfo) que no dicen nada al usuario. Son tablas inmutables:
 * añadir aquí los modelos que aparezcan en Analytics con `metric_unavailable`.
 */
object HardwareNames {

    private val ARM_PARTS = mapOf(
        0xd02 to "Cortex-A34", 0xd03 to "Cortex-A53", 0xd04 to "Cortex-A35", 0xd05 to "Cortex-A55",
        0xd06 to "Cortex-A65", 0xd07 to "Cortex-A57", 0xd08 to "Cortex-A72", 0xd09 to "Cortex-A73",
        0xd0a to "Cortex-A75", 0xd0b to "Cortex-A76", 0xd0d to "Cortex-A77", 0xd41 to "Cortex-A78",
        0xd44 to "Cortex-X1", 0xd46 to "Cortex-A510", 0xd47 to "Cortex-A710", 0xd48 to "Cortex-X2",
        0xd4d to "Cortex-A715", 0xd4e to "Cortex-X3", 0xd80 to "Cortex-A520", 0xd81 to "Cortex-A720",
        0xd82 to "Cortex-X4", 0xd85 to "Cortex-X925", 0xd87 to "Cortex-A725",
    )

    private val QUALCOMM_PARTS = mapOf(
        0x800 to "Kryo Gold", 0x801 to "Kryo Silver", 0x802 to "Kryo Gold", 0x803 to "Kryo Silver",
        0x804 to "Kryo Gold", 0x805 to "Kryo Silver", 0x001 to "Oryon",
    )

    private const val IMPLEMENTER_ARM = 0x41
    private const val IMPLEMENTER_QUALCOMM = 0x51
    private const val IMPLEMENTER_SAMSUNG = 0x53

    fun cpuCoreName(implementer: Int, part: Int): String? = when (implementer) {
        IMPLEMENTER_ARM -> ARM_PARTS[part]
        IMPLEMENTER_QUALCOMM -> QUALCOMM_PARTS[part]
        IMPLEMENTER_SAMSUNG -> "Exynos M"
        else -> null
    }

    /** Clave en minúsculas y sin espacios → (fabricante, nombre comercial). */
    private val SOCS: Map<String, Pair<String, String>> = buildMap {
        // MediaTek
        put("mt6761", "MediaTek" to "Helio A22")
        put("mt6762", "MediaTek" to "Helio P22")
        put("mt6765", "MediaTek" to "Helio P35")
        put("mt6765g", "MediaTek" to "Helio G35")
        put("mt6768", "MediaTek" to "Helio P65")
        put("mt6769v", "MediaTek" to "Helio G70")
        put("mt6769t", "MediaTek" to "Helio G80")
        put("mt6769z", "MediaTek" to "Helio G85")
        put("mt6769h", "MediaTek" to "Helio G88")
        put("mt6781", "MediaTek" to "Helio G96")
        put("mt6785", "MediaTek" to "Helio G90T")
        put("mt6789", "MediaTek" to "Helio G99")
        put("mt6833", "MediaTek" to "Dimensity 700")
        put("mt6853", "MediaTek" to "Dimensity 720")
        put("mt6893", "MediaTek" to "Dimensity 1200")
        put("mt6897", "MediaTek" to "Dimensity 8300")
        put("mt6983", "MediaTek" to "Dimensity 9000")
        put("mt6985", "MediaTek" to "Dimensity 9200")
        put("mt6989", "MediaTek" to "Dimensity 9300")
        // Unisoc
        put("sc9863a", "Unisoc" to "SC9863A")
        put("t606", "Unisoc" to "T606")
        put("t610", "Unisoc" to "T610")
        put("t612", "Unisoc" to "T612")
        put("t616", "Unisoc" to "T616")
        put("t618", "Unisoc" to "T618")
        put("t700", "Unisoc" to "T700")
        put("t760", "Unisoc" to "T760")
        put("t820", "Unisoc" to "T820")
        // Qualcomm
        put("sdm439", "Qualcomm" to "Snapdragon 439")
        put("sdm450", "Qualcomm" to "Snapdragon 450")
        put("sm4350", "Qualcomm" to "Snapdragon 480")
        put("sm6115", "Qualcomm" to "Snapdragon 662")
        put("sm6125", "Qualcomm" to "Snapdragon 665")
        put("sm6150", "Qualcomm" to "Snapdragon 675")
        put("sm6225", "Qualcomm" to "Snapdragon 680")
        put("sm6375", "Qualcomm" to "Snapdragon 695")
        put("sm7125", "Qualcomm" to "Snapdragon 720G")
        put("sm7150", "Qualcomm" to "Snapdragon 730")
        put("sm7325", "Qualcomm" to "Snapdragon 778G")
        put("sm7435", "Qualcomm" to "Snapdragon 7s Gen 2")
        put("sm8150", "Qualcomm" to "Snapdragon 855")
        put("sm8250", "Qualcomm" to "Snapdragon 865")
        put("sm8350", "Qualcomm" to "Snapdragon 888")
        put("sm8450", "Qualcomm" to "Snapdragon 8 Gen 1")
        put("sm8475", "Qualcomm" to "Snapdragon 8+ Gen 1")
        put("sm8550", "Qualcomm" to "Snapdragon 8 Gen 2")
        put("sm8650", "Qualcomm" to "Snapdragon 8 Gen 3")
        put("sm8750", "Qualcomm" to "Snapdragon 8 Elite")
        // Samsung
        put("exynos7885", "Samsung" to "Exynos 7885")
        put("exynos9611", "Samsung" to "Exynos 9611")
        put("s5e3830", "Samsung" to "Exynos 850")
        put("s5e8825", "Samsung" to "Exynos 1280")
        put("s5e8835", "Samsung" to "Exynos 1380")
        put("s5e8845", "Samsung" to "Exynos 1480")
        put("s5e9925", "Samsung" to "Exynos 2200")
        put("s5e9945", "Samsung" to "Exynos 2400")
    }

    /**
     * Resuelve el nombre del SoC probando, por orden, `Build.SOC_MODEL` (API 31+),
     * la línea Hardware de /proc/cpuinfo y `Build.HARDWARE`.
     */
    fun resolveSoc(socManufacturer: String?, vararg candidates: String?): SocName {
        val cleaned = candidates.mapNotNull { it?.trim()?.takeIf { c -> c.isNotEmpty() && c != "unknown" } }
        for (candidate in cleaned) {
            val key = candidate.lowercase().replace(" ", "")
            // Coincidencia exacta o, si no, el prefijo más largo ("mt6765g…" antes que "mt6765…").
            val match = SOCS[key]
                ?: SOCS.entries.filter { key.startsWith(it.key) }.maxByOrNull { it.key.length }?.value
            if (match != null) {
                return SocName(displayName = "${match.first} ${match.second}", code = candidate.uppercase(), vendor = match.first)
            }
        }
        val raw = cleaned.firstOrNull() ?: return SocName(displayName = "—", code = null, vendor = socManufacturer)
        val vendor = socManufacturer?.trim()?.takeIf { it.isNotEmpty() && !it.equals("unknown", ignoreCase = true) }
        val display = if (vendor != null && !raw.startsWith(vendor, ignoreCase = true)) "$vendor $raw" else raw
        return SocName(displayName = display, code = null, vendor = vendor)
    }
}
