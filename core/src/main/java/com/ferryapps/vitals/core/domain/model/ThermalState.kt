package com.ferryapps.vitals.core.domain.model

/** Estado térmico del sistema según PowerManager (API 29+). */
data class ThermalState(
    val status: ThermalStatus,
    /**
     * Margen antes de que el sistema limite el rendimiento, 0–100. Sale de
     * `getThermalHeadroom()` (API 30+); null si el dispositivo no lo da.
     */
    val marginPercent: Int?,
)

enum class ThermalStatus {
    NONE, LIGHT, MODERATE, SEVERE, CRITICAL, EMERGENCY, SHUTDOWN,

    /** Android < 10 o el fabricante no informa. */
    UNAVAILABLE;

    val isThrottling: Boolean get() = this in setOf(MODERATE, SEVERE, CRITICAL, EMERGENCY, SHUTDOWN)
}
