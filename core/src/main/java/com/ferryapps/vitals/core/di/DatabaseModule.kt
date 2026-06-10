package com.ferryapps.vitals.core.di

import android.content.Context
import androidx.room.Room
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
    fun provideDatabase(@ApplicationContext context: Context): VitalsDatabase =
        Room.databaseBuilder(context, VitalsDatabase::class.java, "vitals.db").build()

    @Provides
    fun provideVitalsDao(db: VitalsDatabase) = db.vitalsDao()
}
