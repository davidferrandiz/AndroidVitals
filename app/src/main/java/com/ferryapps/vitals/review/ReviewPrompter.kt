package com.ferryapps.vitals.review

import android.app.Activity
import com.ferryapps.vitals.core.domain.analytics.AnalyticsEvents
import com.ferryapps.vitals.core.domain.analytics.AnalyticsTracker
import com.ferryapps.vitals.core.domain.repository.EngagementRepository
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import javax.inject.Inject

/**
 * Pide valoración en Google Play con In-App Review: a partir de la tercera apertura, si el
 * usuario ha desplegado alguna tarjeta y como mucho una vez cada 4 meses. Google limita
 * además cuántas veces se muestra, así que puede no aparecer aunque se pida.
 */
class ReviewPrompter @Inject constructor(
    private val engagement: EngagementRepository,
    private val analytics: AnalyticsTracker,
) {
    suspend fun onAppStarted(activity: Activity) {
        engagement.recordLaunch()
        val now = System.currentTimeMillis()
        if (!engagement.shouldAskForReview(now)) return
        val manager = ReviewManagerFactory.create(activity)
        val reviewInfo = try {
            manager.requestReview()
        } catch (e: ReviewException) {
            // Sin Play Store o sin cuota: no es un error para el usuario.
            return
        }
        engagement.markReviewAsked(now)
        analytics.logEvent(AnalyticsEvents.REVIEW_PROMPT)
        manager.launchReview(activity, reviewInfo)
    }
}
