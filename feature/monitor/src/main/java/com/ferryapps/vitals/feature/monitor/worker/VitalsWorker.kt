package com.ferryapps.vitals.feature.monitor.worker

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ferryapps.vitals.core.domain.alerts.AlertInputs
import com.ferryapps.vitals.core.domain.alerts.AlertRules
import com.ferryapps.vitals.core.domain.alerts.AlertType
import com.ferryapps.vitals.core.domain.model.AppSettings
import com.ferryapps.vitals.core.domain.model.BatteryInfo
import com.ferryapps.vitals.core.domain.model.BatterySample
import com.ferryapps.vitals.core.domain.model.StorageInfo
import com.ferryapps.vitals.core.domain.repository.AlertStateRepository
import com.ferryapps.vitals.core.domain.repository.BatteryHistoryRepository
import com.ferryapps.vitals.core.domain.repository.SettingsRepository
import com.ferryapps.vitals.core.domain.usecase.GetBatteryHistoryUseCase
import com.ferryapps.vitals.core.domain.usecase.GetBatteryUseCase
import com.ferryapps.vitals.core.domain.usecase.GetStorageUseCase
import com.ferryapps.vitals.core.notifications.NotificationHelper
import com.ferryapps.vitals.designsystem.format.Formatters
import com.ferryapps.vitals.feature.monitor.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Único trabajo en segundo plano de Vitals. Corre cada ~15 min (WorkManager decide el momento
 * exacto y lo aplaza en Doze): una lectura de batería y de almacenamiento, sin red.
 */
@HiltWorker
class VitalsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val getBattery: GetBatteryUseCase,
    private val getStorage: GetStorageUseCase,
    private val history: BatteryHistoryRepository,
    private val alertState: AlertStateRepository,
    private val notifications: NotificationHelper,
    private val settingsRepository: SettingsRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = settingsRepository.settings.first()
        val now = System.currentTimeMillis()
        val battery = getBattery.current()

        if (settings.batteryHistoryEnabled && battery != null) {
            history.record(BatterySample.from(battery, now))
            history.deleteOlderThan(now - GetBatteryHistoryUseCase.RETENTION_MS)
        }
        if (settings.alertsEnabled) checkAlerts(settings, battery, now)
        return Result.success()
    }

    private suspend fun checkAlerts(settings: AppSettings, battery: BatteryInfo?, now: Long) {
        val storage = runCatching { getStorage.current() }.getOrNull()
        val inputs = AlertInputs(
            batteryTemperatureCelsius = battery?.temperatureCelsius,
            storageFreePercent = storage?.takeIf { it.totalBytes > 0 }?.let { it.freeBytes * 100f / it.totalBytes },
        )
        for (type in AlertRules.due(inputs, alertState.lastFired(), now)) {
            val shown = when (type) {
                AlertType.BATTERY_HOT -> showBatteryHot(battery ?: continue, settings)
                AlertType.STORAGE_LOW -> showStorageLow(storage ?: continue)
            }
            // Si no se pudo mostrar (permiso retirado), no se marca: saldrá cuando vuelva a poder.
            if (shown) alertState.markFired(type, now)
        }
    }

    private fun showBatteryHot(battery: BatteryInfo, settings: AppSettings): Boolean {
        val context = applicationContext
        val temperature = Formatters.temperature(battery.temperatureCelsius, settings.temperatureUnit, decimals = 0)
        return notifications.showAlert(
            id = NotificationHelper.ID_BATTERY_HOT,
            title = context.getString(R.string.alert_battery_hot_title, temperature),
            text = context.getString(
                if (battery.isCharging) R.string.alert_battery_hot_charging_msg else R.string.alert_battery_hot_msg
            ),
        )
    }

    private fun showStorageLow(storage: StorageInfo): Boolean {
        val context = applicationContext
        return notifications.showAlert(
            id = NotificationHelper.ID_STORAGE_LOW,
            title = context.getString(R.string.panel_alert_storage_title),
            text = context.getString(
                R.string.panel_alert_storage_msg,
                Formatters.bytes(context, storage.freeBytes),
                Formatters.bytes(context, storage.totalBytes)
            ),
            contentIntent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
        )
    }
}
