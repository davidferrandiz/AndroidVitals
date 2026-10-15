package com.ferryapps.vitals.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BatterySampleDao {

    @Insert
    suspend fun insert(entity: BatterySampleEntity)

    @Query("SELECT * FROM battery_samples WHERE timestamp_ms >= :fromMs ORDER BY timestamp_ms ASC")
    fun observeSince(fromMs: Long): Flow<List<BatterySampleEntity>>

    @Query("DELETE FROM battery_samples WHERE timestamp_ms < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long)

    @Query("DELETE FROM battery_samples")
    suspend fun clear()
}
