package com.ferryapps.vitals

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.ferryapps.vitals.core.domain.analytics.AnalyticsTracker
import com.ferryapps.vitals.core.domain.repository.SettingsRepository
import com.ferryapps.vitals.core.notifications.NotificationHelper
import com.ferryapps.vitals.di.ApplicationScope
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class VitalsApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var analytics: AnalyticsTracker
    @Inject @field:ApplicationScope lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createChannels()
        applyAnalyticsConsent()
    }

    /** La analítica y Crashlytics siguen siempre lo que el usuario elige en Ajustes. */
    private fun applyAnalyticsConsent() {
        appScope.launch {
            settingsRepository.settings
                .map { it.analyticsEnabled to it.crashReportsEnabled }
                .distinctUntilChanged()
                .collect { (analyticsOn, crashesOn) -> analytics.setCollectionEnabled(analyticsOn, crashesOn) }
        }
    }

    override fun getWorkManagerConfiguration(): Configuration =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
