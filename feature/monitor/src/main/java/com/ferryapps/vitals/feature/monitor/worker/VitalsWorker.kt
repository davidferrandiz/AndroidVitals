package com.ferryapps.vitals.feature.monitor.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ferryapps.vitals.core.domain.model.BatterySample
import com.ferryapps.vitals.core.domain.repository.BatteryHistoryRepository
import com.ferryapps.vitals.core.domain.repository.SettingsRepository
import com.ferryapps.vitals.core.domain.usecase.GetBatteryHistoryUseCase
import com.ferryapps.vitals.core.domain.usecase.GetBatteryUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Único trabajo en segundo plano de Vitals. Corre cada ~15 min (WorkManager decide el momento
 * exacto y lo aplaza en Doze): una lectura de batería, unos milisegundos, sin red.
 */
@HiltWorker
class VitalsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val getBattery: GetBatteryUseCase,
    private val history: BatteryHistoryRepository,
    private val settingsRepository: SettingsRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = settingsRepository.settings.first()
        val now = System.currentTimeMillis()
        val battery = getBattery.current() ?: return Result.success()

        if (settings.batteryHistoryEnabled) {
            history.record(BatterySample.from(battery, now))
            history.deleteOlderThan(now - GetBatteryHistoryUseCase.RETENTION_MS)
        }
        return Result.success()
    }
}
