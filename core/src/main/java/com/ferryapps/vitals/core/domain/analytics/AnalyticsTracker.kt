package com.ferryapps.vitals.core.domain.analytics

/**
 * Contrato de analítica. La implementación con Firebase vive en `:app`, así que `:core` y
 * las features no dependen de Firebase. Nunca se envían valores de métricas del usuario,
 * solo qué se usa y qué falla.
 */
interface AnalyticsTracker {
    fun logEvent(name: String, params: Map<String, String> = emptyMap())
    fun logScreen(screenName: String)
    fun setCollectionEnabled(analytics: Boolean, crashReports: Boolean)
}

object AnalyticsEvents {
    /** Tarjeta abierta. Param `card`: cpu, vitals… */
    const val CARD_EXPAND = "card_expand"

    /** Una fuente de datos no está disponible en este dispositivo. Param `source`. */
    const val METRIC_UNAVAILABLE = "metric_unavailable"

    /** Cambio de ajuste. Params `setting` y `value`. */
    const val SETTING_CHANGED = "setting_changed"

    /** Se ha mostrado la valoración de Play (In-App Review). */
    const val REVIEW_PROMPT = "review_prompt"

    const val PARAM_CARD = "card"
    const val PARAM_SOURCE = "source"
    const val PARAM_SETTING = "setting"
    const val PARAM_VALUE = "value"
}
