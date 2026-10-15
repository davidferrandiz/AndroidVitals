package com.ferryapps.vitals.core.di

import com.ferryapps.vitals.core.data.repository.EngagementRepositoryImpl
import com.ferryapps.vitals.core.data.repository.SettingsRepositoryImpl
import com.ferryapps.vitals.core.data.repository.VitalsRepositoryImpl
import com.ferryapps.vitals.core.domain.repository.EngagementRepository
import com.ferryapps.vitals.core.domain.repository.SettingsRepository
import com.ferryapps.vitals.core.domain.repository.VitalsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindVitalsRepository(impl: VitalsRepositoryImpl): VitalsRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindEngagementRepository(impl: EngagementRepositoryImpl): EngagementRepository
}
