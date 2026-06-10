package com.ferryapps.vitals.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VitalsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: VitalsEntity)

    @Query("SELECT * FROM vitals_snapshots ORDER BY timestampMs DESC")
    fun observeAll(): Flow<List<VitalsEntity>>

    @Query("DELETE FROM vitals_snapshots WHERE timestampMs < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long)
}
