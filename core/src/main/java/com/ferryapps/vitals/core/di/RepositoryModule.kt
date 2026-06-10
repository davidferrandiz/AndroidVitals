package com.ferryapps.vitals.core.di

import com.ferryapps.vitals.core.data.repository.VitalsRepositoryImpl
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
}
