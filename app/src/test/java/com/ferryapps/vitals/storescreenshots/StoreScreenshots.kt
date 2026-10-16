package com.ferryapps.vitals.storescreenshots

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ferryapps.vitals.core.domain.model.AppSettings
import com.ferryapps.vitals.designsystem.R as DsR
import com.ferryapps.vitals.designsystem.theme.VitalsTheme
import com.ferryapps.vitals.feature.battery.BatteryContent
import com.ferryapps.vitals.feature.hardware.HardwareContent
import com.ferryapps.vitals.feature.monitor.ui.MonitorContent
import com.ferryapps.vitals.navigation.Routes
import com.ferryapps.vitals.navigation.VitalsBottomBar
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import com.ferryapps.vitals.storescreenshots.StoreSampleData as Data

/**
 * Capturas para la ficha de Google Play, con las pantallas reales y [StoreSampleData].
 * No corren en la CI normal: `./gradlew :app:testDebugUnitTest -PstoreScreenshots`
 * (o el workflow "Store screenshots"), que las deja en docs/store-assets/screenshots/raw/<idioma>/.
 * Luego docs/store-assets/frame_screenshots.py les pone el titular y el marco.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class StoreScreenshots(private val locale: String) {

    @get:Rule
    val compose = createComposeRule()

    private fun capture(name: String, darkTheme: Boolean = true, route: String, content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit) {
        compose.setContent {
            VitalsTheme(darkTheme = darkTheme) {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                    topBar = { FakeStatusBar() },
                    bottomBar = { VitalsBottomBar(isSelected = { it == route }, onNavigate = {}) },
                ) { padding -> Box(Modifier.fillMaxSize()) { content(padding) } }
            }
        }
        compose.waitForIdle()
        val dir = System.getProperty("storeScreenshots.outDir") ?: "build/store-screenshots"
        compose.onRoot().captureRoboImage("$dir/$locale/$name.png")
    }

    @Test
    fun panel() = capture("01_panel", route = Routes.PANEL) { padding -> Panel(padding) }

    @Test
    fun battery() = capture("02_battery", route = Routes.BATTERY) { padding ->
        BatteryContent(
            battery = Data.battery,
            powerHistory = Data.powerHistory,
            history = Data.history(System.currentTimeMillis()),
            settings = AppSettings(batteryHistoryEnabled = true),
            contentPadding = padding,
        )
    }

    @Test
    fun hardware() = capture("03_hardware", route = Routes.HARDWARE) { padding ->
        HardwareContent(
            device = Data.device,
            cpu = Data.cpu,
            thermalState = Data.thermalState,
            memory = Data.memory,
            sensors = Data.sensors,
            uptime = Data.UPTIME_MS,
            contentPadding = padding,
        )
    }

    @Test
    fun panelLight() = capture("04_panel_light", darkTheme = false, route = Routes.PANEL) { padding -> Panel(padding) }

    @Composable
    private fun Panel(padding: androidx.compose.foundation.layout.PaddingValues) {
        MonitorContent(
            settings = AppSettings(),
            device = Data.deviceSummary,
            expanded = emptySet(),
            cpu = Data.cpu,
            cpuHistory = Data.cpuHistory,
            thermalZones = Data.thermalZones,
            thermalState = Data.thermalState,
            memory = Data.memory,
            storage = Data.storage,
            battery = Data.battery,
            speed = Data.speed,
            networkHistory = Data.networkHistory,
            network = Data.network,
            appCpu = 1.2f,
            appThreads = 38,
            appMemory = Data.appMemory,
            appProcesses = Data.appProcesses,
            onToggleCard = {},
            onOpenBattery = {},
            onOpenHardware = {},
            onOpenSettings = {},
            contentPadding = padding,
        )
    }

    /** Barra de estado de Android: Robolectric no la dibuja. */
    @Composable
    private fun FakeStatusBar() {
        val color = MaterialTheme.colorScheme.onBackground
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(STATUS_BAR)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 22.dp)
        ) {
            Text("16:42", color = color, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Icon(painterResource(DsR.drawable.ic_wifi), null, tint = color, modifier = Modifier.size(16.dp))
            Icon(painterResource(DsR.drawable.ic_battery_charging), null, tint = color, modifier = Modifier.size(16.dp))
            Text("78%", color = color, fontSize = 13.sp)
            Spacer(Modifier.width(2.dp))
        }
    }

    private companion object {
        val STATUS_BAR = 32.dp
    }
}

private const val SCREEN = "w412dp-h892dp-port-xxhdpi"

@Config(sdk = [35], application = Application::class, qualifiers = "en-rUS-$SCREEN")
class StoreScreenshotsEn : StoreScreenshots("en-US")

@Config(sdk = [35], application = Application::class, qualifiers = "es-rES-$SCREEN")
class StoreScreenshotsEs : StoreScreenshots("es-ES")

@Config(sdk = [35], application = Application::class, qualifiers = "de-rDE-$SCREEN")
class StoreScreenshotsDe : StoreScreenshots("de-DE")

@Config(sdk = [35], application = Application::class, qualifiers = "it-rIT-$SCREEN")
class StoreScreenshotsIt : StoreScreenshots("it-IT")

@Config(sdk = [35], application = Application::class, qualifiers = "fr-rCA-$SCREEN")
class StoreScreenshotsFr : StoreScreenshots("fr-CA")

@Config(sdk = [35], application = Application::class, qualifiers = "hi-rIN-$SCREEN")
class StoreScreenshotsHi : StoreScreenshots("hi-IN")

@Config(sdk = [35], application = Application::class, qualifiers = "fa-rIR-ldrtl-$SCREEN")
class StoreScreenshotsFa : StoreScreenshots("fa-IR")
