package com.ferryapps.vitals.feature.widget

import com.ferryapps.vitals.core.domain.model.BatteryInfo
import com.ferryapps.vitals.core.domain.model.MemoryInfo
import com.ferryapps.vitals.core.domain.model.StorageInfo
import com.ferryapps.vitals.core.domain.model.TemperatureUnit

/** Lo que pinta el widget, leído una vez por actualización. */
data class WidgetSnapshot(
    val battery: BatteryInfo?,
    val memory: MemoryInfo?,
    val storage: StorageInfo?,
    val temperatureUnit: TemperatureUnit,
    val updatedAtMs: Long,
)
