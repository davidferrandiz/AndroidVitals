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
 * Implementación con Firebase. Fuera de la build de release (debug y los build types del
 * Baseline Profile) no se envía nada, para no mezclar el móvil de desarrollo ni el emulador de
 * la CI con los usuarios reales.
 */
@Singleton
class FirebaseAnalyticsTracker @Inject constructor(
    @ApplicationContext context: Context
) : AnalyticsTracker {

    private val analytics = FirebaseAnalytics.getInstance(context)
    private val crashlytics = FirebaseCrashlytics.getInstance()

    private val isRelease = BuildConfig.BUILD_TYPE == "release"

    init {
        if (!isRelease) setCollectionEnabled(analytics = false, crashReports = false)
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
        this.analytics.setAnalyticsCollectionEnabled(analytics && isRelease)
        crashlytics.setCrashlyticsCollectionEnabled(crashReports && isRelease)
    }
}
