package com.ferryapps.vitals.analytics

import android.content.Context
import android.os.Bundle
import com.ferryapps.vitals.BuildConfig
import com.ferryapps.vitals.core.domain.analytics.AnalyticsTracker
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación con Firebase. En debug no se envía nada, para no mezclar el móvil de
 * desarrollo con los usuarios reales en Analytics.
 */
@Singleton
class FirebaseAnalyticsTracker @Inject constructor(
    @ApplicationContext context: Context
) : AnalyticsTracker {

    private val analytics = FirebaseAnalytics.getInstance(context)
    private val crashlytics = FirebaseCrashlytics.getInstance()

    init {
        if (BuildConfig.DEBUG) setCollectionEnabled(analytics = false, crashReports = false)
    }

    override fun logEvent(name: String, params: Map<String, String>) {
        val bundle = Bundle().apply { params.forEach { (key, value) -> putString(key, value) } }
        analytics.logEvent(name, bundle)
    }

    override fun logScreen(screenName: String) {
        analytics.logEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
            }
        )
    }

    override fun setCollectionEnabled(analytics: Boolean, crashReports: Boolean) {
        this.analytics.setAnalyticsCollectionEnabled(analytics && !BuildConfig.DEBUG)
        crashlytics.setCrashlyticsCollectionEnabled(crashReports && !BuildConfig.DEBUG)
    }
}
