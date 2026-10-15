package com.ferryapps.vitals.core.domain.cpu

import com.ferryapps.vitals.core.domain.model.CpuCluster
import com.ferryapps.vitals.core.domain.model.CpuCoreInfo
import com.ferryapps.vitals.core.domain.model.ThermalKind

object CpuMath {

    /**
     * Media de (actual − mínima) / (máxima − mínima) de cada núcleo. Los núcleos apagados
     * cuentan como 0. Sin mínima conocida se usa 0, lo que sobreestima un poco en reposo.
     */
    fun estimatedLoadPercent(cores: List<CpuCoreInfo>): Float {
        if (cores.isEmpty()) return 0f
        val total = cores.sumOf { core ->
            if (!core.isOnline || core.maxMhz <= 0) 0.0
            else {
                val range = (core.maxMhz - core.minMhz).coerceAtLeast(1)
                ((core.currentMhz - core.minMhz).toDouble() / range).coerceIn(0.0, 1.0)
            }
        }
        return (total / cores.size * 100.0).toFloat()
    }

    /** Agrupa núcleos por microarquitectura y frecuencia máxima. El más potente, primero. */
    fun clusters(cores: List<CpuCoreInfo>): List<CpuCluster> =
        cores.groupBy { it.coreName to it.maxMhz }
            .map { (key, group) ->
                val online = group.filter { it.isOnline }
                CpuCluster(
                    coreName = key.first,
                    coreCount = group.size,
                    onlineCount = online.size,
                    currentMhz = online.maxOfOrNull { it.currentMhz } ?: 0,
                    maxMhz = key.second,
                )
            }
            .sortedWith(compareByDescending<CpuCluster> { it.maxMhz }.thenBy { it.coreCount })

    /** Clasifica una zona térmica por su nombre (`type`). Cubre Qualcomm, MediaTek, Unisoc, Exynos y Tensor. */
    fun classifyThermalZone(type: String): ThermalKind {
        val t = type.lowercase()
        return when {
            "gpu" in t -> ThermalKind.GPU
            "batt" in t -> ThermalKind.BATTERY
            "skin" in t || "quiet" in t || "case" in t -> ThermalKind.SKIN
            "cpu" in t || t.startsWith("mtktscpu") || t.startsWith("big") || t.startsWith("little") || t.startsWith("mid") -> ThermalKind.CPU
            "soc" in t -> ThermalKind.SOC
            t.startsWith("mtktsap") || "board" in t || "pa_therm" in t || "pa-therm" in t ||
                "xo_therm" in t || "xo-therm" in t -> ThermalKind.BOARD
            else -> ThermalKind.OTHER
        }
    }
}
