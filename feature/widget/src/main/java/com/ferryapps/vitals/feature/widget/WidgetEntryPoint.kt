package com.ferryapps.vitals.feature.widget

import com.ferryapps.vitals.core.domain.repository.SettingsRepository
import com.ferryapps.vitals.core.domain.usecase.GetBatteryUseCase
import com.ferryapps.vitals.core.domain.usecase.GetMemoryUseCase
import com.ferryapps.vitals.core.domain.usecase.GetStorageUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** GlanceAppWidget no admite @AndroidEntryPoint: las dependencias se piden aquí. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun battery(): GetBatteryUseCase
    fun memory(): GetMemoryUseCase
    fun storage(): GetStorageUseCase
    fun settings(): SettingsRepository
}
