package com.ferryapps.vitals.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [VitalsEntity::class], version = 1, exportSchema = false)
abstract class VitalsDatabase : RoomDatabase() {
    abstract fun vitalsDao(): VitalsDao
}
