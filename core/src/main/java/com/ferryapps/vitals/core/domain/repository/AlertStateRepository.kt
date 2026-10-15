package com.ferryapps.vitals.core.domain.repository

import com.ferryapps.vitals.core.domain.alerts.AlertType

/** Cuándo se mostró cada aviso por última vez, para no repetirlo. */
interface AlertStateRepository {
    suspend fun lastFired(): Map<AlertType, Long>
    suspend fun markFired(type: AlertType, atMs: Long)
}
