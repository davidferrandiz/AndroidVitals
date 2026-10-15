package com.ferryapps.vitals.core.domain.repository

/**
 * Señales de uso locales para decidir cuándo pedir una valoración. No salen del dispositivo.
 */
interface EngagementRepository {
    suspend fun recordLaunch()

    /** El usuario ha interactuado de verdad (ha desplegado una tarjeta). */
    suspend fun markEngaged()

    suspend fun shouldAskForReview(nowMs: Long): Boolean

    suspend fun markReviewAsked(nowMs: Long)
}
