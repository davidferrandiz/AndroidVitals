package com.ferryapps.vitals.core.domain.model

data class AppSettings(
    val temperatureUnit: TemperatureUnit = TemperatureUnit.AUTO,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val dynamicColor: Boolean = false,
    /** Abrir núcleos, temperaturas y desgloses sin tener que tocar. */
    val expandCardsByDefault: Boolean = false,
    val analyticsEnabled: Boolean = true,
    val crashReportsEnabled: Boolean = true,
    /** VitalsWorker guarda el nivel cada 15 min. Solo en el dispositivo; desactivarlo lo borra. */
    val batteryHistoryEnabled: Boolean = true,
)

enum class TemperatureUnit { AUTO, CELSIUS, FAHRENHEIT }

enum class ThemeMode { DARK, LIGHT, SYSTEM }
