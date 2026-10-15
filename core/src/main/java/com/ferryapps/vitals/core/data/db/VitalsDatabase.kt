package com.ferryapps.vitals.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * v1 tenía la tabla vitals_snapshots, que nunca llegó a escribirse; v2 la sustituye por
 * battery_samples con migración destructiva (ver DatabaseModule). Desde v2 el esquema se
 * exporta a core/schemas y cada cambio necesita una Migration con su test.
 */
@Database(entities = [BatterySampleEntity::class], version = 2, exportSchema = true)
abstract class VitalsDatabase : RoomDatabase() {
    abstract fun batterySampleDao(): BatterySampleDao
}
