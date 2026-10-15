package com.ferryapps.vitals.core.domain.usecase

import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.annotation.RequiresApi
import com.ferryapps.vitals.core.domain.model.ThermalState
import com.ferryapps.vitals.core.domain.model.ThermalStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Detector de throttling: estado térmico (API 29+) y margen antes de limitar (API 30+).
 * `getThermalHeadroom` devuelve NaN si se llama más de una vez por segundo, por eso 5 s.
 */
class GetThermalStateUseCase @Inject constructor(
    @ApplicationContext context: Context
) {
    private val powerManager: PowerManager? = context.getSystemService(PowerManager::class.java)

    operator fun invoke(): Flow<ThermalState> = flow {
        val manager = powerManager
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || manager == null) {
            emit(ThermalState(ThermalStatus.UNAVAILABLE, marginPercent = null))
            return@flow
        }
        while (true) {
            emit(read(manager))
            delay(5_000)
        }
    }.flowOn(Dispatchers.Default)

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun read(manager: PowerManager): ThermalState {
        val status = when (manager.currentThermalStatus) {
            PowerManager.THERMAL_STATUS_NONE -> ThermalStatus.NONE
            PowerManager.THERMAL_STATUS_LIGHT -> ThermalStatus.LIGHT
            PowerManager.THERMAL_STATUS_MODERATE -> ThermalStatus.MODERATE
            PowerManager.THERMAL_STATUS_SEVERE -> ThermalStatus.SEVERE
            PowerManager.THERMAL_STATUS_CRITICAL -> ThermalStatus.CRITICAL
            PowerManager.THERMAL_STATUS_EMERGENCY -> ThermalStatus.EMERGENCY
            PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalStatus.SHUTDOWN
            else -> ThermalStatus.UNAVAILABLE
        }
        val margin = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 1.0 = umbral de limitación severa. Margen = lo que falta hasta ahí.
            val headroom = manager.getThermalHeadroom(FORECAST_SECONDS)
            if (headroom.isNaN() || headroom < 0f) null
            else ((1f - headroom) * 100f).roundToInt().coerceIn(0, 100)
        } else null
        return ThermalState(status, margin)
    }

    private companion object {
        const val FORECAST_SECONDS = 10
    }
}
