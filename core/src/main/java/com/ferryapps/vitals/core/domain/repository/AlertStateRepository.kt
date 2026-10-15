package com.ferryapps.vitals.core.domain.repository

import com.ferryapps.vitals.core.domain.alerts.AlertState
import com.ferryapps.vitals.core.domain.alerts.AlertType

/** Estado de los avisos entre pasadas del worker: cuándo salió cada uno y la racha de batería caliente. */
interface AlertStateRepository {
    suspend fun load(): AlertState
    suspend fun markFired(type: AlertType, atMs: Long)
    suspend fun setBatteryHotStreak(count: Int)
}
