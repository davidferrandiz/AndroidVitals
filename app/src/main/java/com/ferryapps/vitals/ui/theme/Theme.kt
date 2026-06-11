package com.ferryapps.vitals.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VitalsDarkColorScheme = darkColorScheme(
    primary        = Color(0xFF4FC3F7),
    onPrimary      = Color(0xFF003549),
    secondary      = Color(0xFFCE93D8),
    onSecondary    = Color(0xFF3B1046),
    tertiary       = Color(0xFF81C784),
    background     = Color(0xFF0F0F0F),
    onBackground   = Color(0xFFE0E0E0),
    surface        = Color(0xFF1A1A1A),
    onSurface      = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF1E1E1E),
    outline        = Color(0xFF2A2A2A),
)

@Composable
fun VitalsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VitalsDarkColorScheme,
        typography  = Typography,
        content     = content
    )
}