package com.ferryapps.vitals.designsystem.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Colores por categoría y de estado. Cada métrica tiene el suyo en toda la app. */
@Immutable
data class VitalsPalette(
    val cpu: Color,
    val memory: Color,
    val battery: Color,
    val network: Color,
    val storage: Color,
    val apps: Color,
    val good: Color,
    val warning: Color,
    val critical: Color,
    /** Fondo de elementos dentro de una tarjeta (mini-métricas, chips). */
    val raised: Color,
    /** Separadores dentro de tarjetas. */
    val divider: Color,
)

private val DarkPalette = VitalsPalette(
    cpu = Color(0xFF4FC3F7),
    memory = Color(0xFFCE93D8),
    battery = Color(0xFF81C784),
    network = Color(0xFFFFB74D),
    storage = Color(0xFF4DB6AC),
    apps = Color(0xFFEF9A9A),
    good = Color(0xFF81C784),
    warning = Color(0xFFFFB74D),
    critical = Color(0xFFEF9A9A),
    raised = Color(0xFF242424),
    divider = Color(0xFF2A2A2A),
)

/** En claro, los mismos tonos más oscuros para mantener el contraste AA sobre blanco. */
private val LightPalette = VitalsPalette(
    cpu = Color(0xFF0277BD),
    memory = Color(0xFF8E24AA),
    battery = Color(0xFF2E7D32),
    network = Color(0xFFB45309),
    storage = Color(0xFF00796B),
    apps = Color(0xFFC62828),
    good = Color(0xFF2E7D32),
    warning = Color(0xFFB45309),
    critical = Color(0xFFC62828),
    raised = Color(0xFFEDEFF2),
    divider = Color(0xFFE2E4E8),
)

private val VitalsDarkColorScheme = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF003549),
    secondary = Color(0xFFCE93D8),
    onSecondary = Color(0xFF3B1046),
    tertiary = Color(0xFF81C784),
    background = Color(0xFF0F0F0F),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF0F0F0F),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFB0B0B0),
    surfaceContainer = Color(0xFF1A1A1A),
    surfaceContainerHigh = Color(0xFF222222),
    surfaceContainerLow = Color(0xFF161616),
    outline = Color(0xFF3A3A3A),
    outlineVariant = Color(0xFF2A2A2A),
)

private val VitalsLightColorScheme = lightColorScheme(
    primary = Color(0xFF0277BD),
    onPrimary = Color.White,
    secondary = Color(0xFF8E24AA),
    tertiary = Color(0xFF2E7D32),
    background = Color(0xFFF6F7F9),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFF6F7F9),
    onSurface = Color(0xFF1A1C1E),
    onSurfaceVariant = Color(0xFF55595F),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFEDEFF2),
    surfaceContainerLow = Color(0xFFFFFFFF),
    outline = Color(0xFFC4C7CC),
    outlineVariant = Color(0xFFE2E4E8),
)

private val LocalVitalsPalette = staticCompositionLocalOf { DarkPalette }

/** Acceso a la paleta de Vitals: `VitalsTheme.palette.cpu`. */
object VitalsTheme {
    val palette: VitalsPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalVitalsPalette.current
}

/**
 * Cifras tabulares: los números no "bailan" al cambiar cada segundo, sin el aspecto de
 * terminal de una fuente monoespaciada.
 */
val TabularNumbers = TextStyle(fontFeatureSettings = "tnum")

private val VitalsTypography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum"),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum"),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum"),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
        labelSmall = base.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
    )
}

@Composable
fun VitalsTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> VitalsDarkColorScheme
        else -> VitalsLightColorScheme
    }
    CompositionLocalProvider(LocalVitalsPalette provides if (darkTheme) DarkPalette else LightPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = VitalsTypography,
            content = content
        )
    }
}
