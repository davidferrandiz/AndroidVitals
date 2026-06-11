package com.ferryapps.vitals.core.domain.usecase

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.ferryapps.vitals.core.domain.model.BatteryHealth
import com.ferryapps.vitals.core.domain.model.BatteryInfo
import com.ferryapps.vitals.core.domain.model.BatteryStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetBatteryUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    operator fun invoke(): Flow<BatteryInfo> = flow {
        while (true) {
            emit(readBattery())
            delay(5_000) // La batería cambia lento — 5s es suficiente
        }
    }

    private fun readBattery(): BatteryInfo {
        val intent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level    = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale    = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status   = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val health   = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val temp     = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val voltage  = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val tech     = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "—"

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                         status == BatteryManager.BATTERY_STATUS_FULL

        return BatteryInfo(
            levelPercent       = if (scale > 0) (level * 100 / scale) else 0,
            isCharging         = isCharging,
            status             = mapStatus(status),
            health             = mapHealth(health),
            temperatureCelsius = temp / 10f,
            voltageMillivolts  = voltage,
            technology         = tech
        )
    }

    private fun mapStatus(status: Int) = when (status) {
        BatteryManager.BATTERY_STATUS_CHARGING     -> BatteryStatus.CHARGING
        BatteryManager.BATTERY_STATUS_DISCHARGING  -> BatteryStatus.DISCHARGING
        BatteryManager.BATTERY_STATUS_FULL         -> BatteryStatus.FULL
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> BatteryStatus.NOT_CHARGING
        else -> BatteryStatus.UNKNOWN
    }

    private fun mapHealth(health: Int) = when (health) {
        BatteryManager.BATTERY_HEALTH_GOOD         -> BatteryHealth.GOOD
        BatteryManager.BATTERY_HEALTH_OVERHEAT     -> BatteryHealth.OVERHEAT
        BatteryManager.BATTERY_HEALTH_DEAD         -> BatteryHealth.DEAD
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealth.OVER_VOLTAGE
        BatteryManager.BATTERY_HEALTH_COLD         -> BatteryHealth.COLD
        else -> BatteryHealth.UNKNOWN
    }
}
