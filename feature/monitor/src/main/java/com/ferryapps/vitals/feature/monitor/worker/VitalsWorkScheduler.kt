package com.ferryapps.vitals.feature.monitor.worker

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ferryapps.vitals.core.domain.model.AppSettings
import com.ferryapps.vitals.core.domain.repository.BatteryHistoryRepository
import dagger.Lazy
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Programa o cancela VitalsWorker según los ajustes. Lo llama VitalsApplication al cambiar.
 * WorkManager va en Lazy: se inyecta en Application.onCreate, antes de que exista la
 * configuración con HiltWorkerFactory, y crearlo ahí lo inicializaría sin ella.
 */
@Singleton
class VitalsWorkScheduler @Inject constructor(
    private val workManager: Lazy<WorkManager>,
    private val history: BatteryHistoryRepository,
) {

    suspend fun apply(settings: AppSettings) {
        if (needsWorker(settings)) {
            val request = PeriodicWorkRequestBuilder<VitalsWorker>(INTERVAL_MINUTES, TimeUnit.MINUTES).build()
            workManager.get().enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        } else {
            workManager.get().cancelUniqueWork(WORK_NAME)
        }
        // Desactivar el historial lo borra: es lo que promete el texto del ajuste.
        if (!settings.batteryHistoryEnabled) history.clear()
    }

    companion object {
        const val WORK_NAME = "vitals_periodic"

        /** El mínimo que permite WorkManager. */
        const val INTERVAL_MINUTES = 15L

        /** Lo que decide si hace falta el worker; VitalsApplication solo reacciona a sus cambios. */
        fun needsWorker(settings: AppSettings): Boolean = settings.batteryHistoryEnabled
    }
}
