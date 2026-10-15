package com.ferryapps.vitals.core.domain.repository

import com.ferryapps.vitals.core.domain.model.BatterySample
import kotlinx.coroutines.flow.Flow

/** Historial local de batería. Nunca sale del dispositivo. */
interface BatteryHistoryRepository {
    suspend fun record(sample: BatterySample)
    fun observeSince(fromMs: Long): Flow<List<BatterySample>>
    suspend fun deleteOlderThan(cutoffMs: Long)
    suspend fun clear()
}
