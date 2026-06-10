package com.ferryapps.vitals.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vitals_snapshots")
data class VitalsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cpuUsagePercent: Float,
    val ramUsedMb: Long,
    val ramTotalMb: Long,
    val threadCount: Int,
    val timestampMs: Long
)
