package com.ferryapps.vitals

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ferryapps.vitals.core.domain.analytics.AnalyticsTracker
import com.ferryapps.vitals.core.domain.model.AppSettings
import com.ferryapps.vitals.core.domain.model.ThemeMode
import com.ferryapps.vitals.core.domain.repository.SettingsRepository
import com.ferryapps.vitals.designsystem.theme.VitalsTheme
import com.ferryapps.vitals.navigation.VitalsApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var analytics: AnalyticsTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            val darkTheme = when (settings.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            // Iconos de la barra de estado acordes al tema de la app, no al del sistema.
            DisposableEffect(darkTheme) {
                val style = if (darkTheme) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            VitalsTheme(darkTheme = darkTheme, dynamicColor = settings.dynamicColor) {
                VitalsApp(analytics = analytics)
            }
        }
    }
}
