package com.ferryapps.vitals.core.di

import android.content.Context
import androidx.room.Room
import com.ferryapps.vitals.core.data.db.BatterySampleDao
import com.ferryapps.vitals.core.data.db.VitalsDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    @Suppress("DEPRECATION") // Room 2.7 prefiere la firma con dropAllTables; esta funciona igual
    fun provideDatabase(@ApplicationContext context: Context): VitalsDatabase =
        Room.databaseBuilder(context, VitalsDatabase::class.java, "vitals.db")
            // v1 no tenía datos reales: se puede tirar sin perder nada del usuario.
            .fallbackToDestructiveMigrationFrom(1)
            .build()

    @Provides
    fun provideBatterySampleDao(db: VitalsDatabase): BatterySampleDao = db.batterySampleDao()
}
