package com.ferryapps.vitals.core.domain.usecase

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import com.ferryapps.vitals.core.domain.battery.BatteryNormalizer
import com.ferryapps.vitals.core.domain.model.BatteryHealth
import com.ferryapps.vitals.core.domain.model.BatteryInfo
import com.ferryapps.vitals.core.domain.model.BatteryStatus
import com.ferryapps.vitals.core.domain.model.PlugType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class GetBatteryUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val batteryManager: BatteryManager? = context.getSystemService(BatteryManager::class.java)

    /** 2 s por defecto: la corriente cambia rápido y se pinta en una gráfica. */
    operator fun invoke(intervalMs: Long = 2_000L): Flow<BatteryInfo> = flow {
        while (true) {
            current()?.let { emit(it) }
            delay(intervalMs)
        }
    }.flowOn(Dispatchers.IO)

    /** Una lectura suelta (VitalsWorker, widget). Lee un broadcast fijo: no registra nada. */
    fun current(): BatteryInfo? {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return null
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val statusRaw = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val healthRaw = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
        val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        val tech = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)?.takeIf { it.isNotBlank() } ?: "—"
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)

        val status = mapStatus(statusRaw)
        val isCharging = status == BatteryStatus.CHARGING || status == BatteryStatus.FULL
        val chargingForSign = when (status) {
            BatteryStatus.CHARGING -> true
            BatteryStatus.DISCHARGING, BatteryStatus.NOT_CHARGING -> false
            else -> null
        }

        val manager = batteryManager
        val currentNow = manager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            ?.let { BatteryNormalizer.currentToMa(it.toLong(), chargingForSign) }
        val currentAvg = manager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
            ?.let { BatteryNormalizer.currentToMa(it.toLong(), chargingForSign) }
        val counter = manager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            ?.let { BatteryNormalizer.chargeCounterToMah(it.toLong()) }

        val chargeTimeRemaining = if (manager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && status == BatteryStatus.CHARGING) {
            manager.computeChargeTimeRemaining().takeIf { it > 0 }
        } else null

        val cycles = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            intent.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1).takeIf { it >= 0 }
        } else null

        return BatteryInfo(
            levelPercent = if (scale > 0 && level >= 0) level * 100 / scale else 0,
            isCharging = isCharging,
            status = status,
            health = mapHealth(healthRaw),
            temperatureCelsius = temp / 10f,
            voltageMillivolts = voltage,
            technology = tech,
            plugType = mapPlug(plugged),
            currentNowMa = currentNow,
            currentAverageMa = currentAvg,
            chargeCounterMah = counter,
            chargeTimeRemainingMs = chargeTimeRemaining,
            cycleCount = cycles,
        )
    }

    private fun mapStatus(status: Int) = when (status) {
        BatteryManager.BATTERY_STATUS_CHARGING -> BatteryStatus.CHARGING
        BatteryManager.BATTERY_STATUS_DISCHARGING -> BatteryStatus.DISCHARGING
        BatteryManager.BATTERY_STATUS_FULL -> BatteryStatus.FULL
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> BatteryStatus.NOT_CHARGING
        else -> BatteryStatus.UNKNOWN
    }

    private fun mapHealth(health: Int) = when (health) {
        BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealth.GOOD
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealth.OVERHEAT
        BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealth.DEAD
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealth.OVER_VOLTAGE
        BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealth.COLD
        else -> BatteryHealth.UNKNOWN
    }

    private fun mapPlug(plugged: Int) = when {
        plugged and BatteryManager.BATTERY_PLUGGED_WIRELESS != 0 -> PlugType.WIRELESS
        plugged and BatteryManager.BATTERY_PLUGGED_AC != 0 -> PlugType.AC
        plugged and BatteryManager.BATTERY_PLUGGED_USB != 0 -> PlugType.USB
        plugged and PLUGGED_DOCK != 0 -> PlugType.DOCK
        else -> PlugType.NONE
    }

    private companion object {
        /** BatteryManager.BATTERY_PLUGGED_DOCK es API 33; el valor es estable. */
        const val PLUGGED_DOCK = 8
    }
}
