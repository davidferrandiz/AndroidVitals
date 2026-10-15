package com.ferryapps.vitals.core.di

import com.ferryapps.vitals.core.data.repository.AlertStateRepositoryImpl
import com.ferryapps.vitals.core.data.repository.BatteryHistoryRepositoryImpl
import com.ferryapps.vitals.core.data.repository.EngagementRepositoryImpl
import com.ferryapps.vitals.core.data.repository.SettingsRepositoryImpl
import com.ferryapps.vitals.core.domain.repository.AlertStateRepository
import com.ferryapps.vitals.core.domain.repository.BatteryHistoryRepository
import com.ferryapps.vitals.core.domain.repository.EngagementRepository
import com.ferryapps.vitals.core.domain.repository.SettingsRepository
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
    abstract fun bindBatteryHistoryRepository(impl: BatteryHistoryRepositoryImpl): BatteryHistoryRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindEngagementRepository(impl: EngagementRepositoryImpl): EngagementRepository

    @Binds
    @Singleton
    abstract fun bindAlertStateRepository(impl: AlertStateRepositoryImpl): AlertStateRepository
}
