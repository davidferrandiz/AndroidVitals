package com.ferryapps.vitals.core.domain.model

data class CpuCoreInfo(
    val index: Int,
    val currentMhz: Int,
    val maxMhz: Int,
    val isOnline: Boolean,
    val minMhz: Int = 0,
    /** Nombre de la microarquitectura (ej. "Cortex-A55"), si se reconoce. */
    val coreName: String? = null,
)

/** Núcleos que comparten microarquitectura y frecuencia máxima (eficiencia, rendimiento, prime). */
data class CpuCluster(
    val coreName: String?,
    val coreCount: Int,
    val onlineCount: Int,
    val currentMhz: Int,
    val maxMhz: Int,
)

data class CpuSnapshot(
    val cores: List<CpuCoreInfo>,
    val clusters: List<CpuCluster>,
    /**
     * Carga estimada (0–100) a partir de la frecuencia de cada núcleo. Android no deja leer
     * el uso real de CPU del sistema desde Android 8, así que es la mejor aproximación sin root.
     */
    val estimatedLoadPercent: Float,
)

data class ThermalZone(
    val name: String,
    val temperatureCelsius: Float,
    val kind: ThermalKind = ThermalKind.OTHER,
)

enum class ThermalKind { CPU, GPU, SOC, BOARD, BATTERY, SKIN, OTHER }
