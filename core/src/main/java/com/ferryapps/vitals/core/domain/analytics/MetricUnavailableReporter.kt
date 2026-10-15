package com.ferryapps.vitals.core.domain.analytics

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Envía `metric_unavailable` una sola vez por fuente y proceso. Con el modelo de móvil que
 * Firebase ya adjunta, dice en qué fabricantes falla cada lectura (térmica, corriente…).
 */
@Singleton
class MetricUnavailableReporter @Inject constructor(
    private val tracker: AnalyticsTracker
) {
    private val reported: MutableSet<String> = ConcurrentHashMap.newKeySet()

    fun report(source: String) {
        if (reported.add(source)) {
            tracker.logEvent(AnalyticsEvents.METRIC_UNAVAILABLE, mapOf(AnalyticsEvents.PARAM_SOURCE to source))
        }
    }

    companion object Sources {
        const val THERMAL_ZONES = "thermal_zones"
        const val THERMAL_HEADROOM = "thermal_headroom"
        const val BATTERY_CURRENT = "battery_current"
        const val BATTERY_CYCLES = "battery_cycles"
        const val CPU_CORE_NAMES = "cpu_core_names"
        const val SOC_NAME = "soc_name"
        const val WIDEVINE = "widevine"
    }
}
