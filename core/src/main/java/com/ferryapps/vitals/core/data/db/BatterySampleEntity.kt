package com.ferryapps.vitals.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Estado y enchufe se guardan por nombre del enum: sobreviven a reordenar los enums. */
@Entity(tableName = "battery_samples", indices = [Index(value = ["timestamp_ms"])])
data class BatterySampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "timestamp_ms") val timestampMs: Long,
    @ColumnInfo(name = "level_percent") val levelPercent: Int,
    val status: String,
    @ColumnInfo(name = "plug_type") val plugType: String,
    @ColumnInfo(name = "temperature_c") val temperatureCelsius: Float,
    @ColumnInfo(name = "voltage_mv") val voltageMillivolts: Int,
    @ColumnInfo(name = "charge_counter_mah") val chargeCounterMah: Int?,
)
