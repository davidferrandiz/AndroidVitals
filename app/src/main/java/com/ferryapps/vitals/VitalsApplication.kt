package com.ferryapps.vitals

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.ferryapps.vitals.core.domain.analytics.AnalyticsTracker
import com.ferryapps.vitals.core.domain.repository.SettingsRepository
import com.ferryapps.vitals.core.notifications.NotificationHelper
import com.ferryapps.vitals.di.ApplicationScope
import com.ferryapps.vitals.feature.monitor.worker.VitalsWorkScheduler
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
    @Inject lateinit var workScheduler: VitalsWorkScheduler
    @Inject @field:ApplicationScope lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createChannels()
        applyAnalyticsConsent()
        scheduleBackgroundWork()
    }

    /** VitalsWorker existe solo si algún ajuste lo necesita (historial de batería o avisos). */
    private fun scheduleBackgroundWork() {
        appScope.launch {
            settingsRepository.settings
                .distinctUntilChanged { old, new ->
                    VitalsWorkScheduler.needsWorker(old) == VitalsWorkScheduler.needsWorker(new) &&
                        old.batteryHistoryEnabled == new.batteryHistoryEnabled
                }
                .collect { workScheduler.apply(it) }
        }
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

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
