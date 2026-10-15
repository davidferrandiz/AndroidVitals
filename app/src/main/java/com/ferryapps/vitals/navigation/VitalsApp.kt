package com.ferryapps.vitals.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ferryapps.vitals.R
import com.ferryapps.vitals.core.domain.analytics.AnalyticsTracker
import com.ferryapps.vitals.designsystem.R as DsR
import com.ferryapps.vitals.feature.battery.BatteryScreen
import com.ferryapps.vitals.feature.hardware.HardwareScreen
import com.ferryapps.vitals.feature.monitor.ui.MonitorScreen
import com.ferryapps.vitals.feature.settings.SettingsScreen

private object Routes {
    const val PANEL = "panel"
    const val BATTERY = "battery"
    const val HARDWARE = "hardware"
    const val SETTINGS = "settings"
}

private enum class TopLevel(val route: String, @StringRes val label: Int, @DrawableRes val icon: Int) {
    PANEL(Routes.PANEL, R.string.nav_panel, DsR.drawable.ic_gauge),
    BATTERY(Routes.BATTERY, R.string.nav_battery, DsR.drawable.ic_battery),
    HARDWARE(Routes.HARDWARE, R.string.nav_hardware, DsR.drawable.ic_phone),
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun VitalsApp(analytics: AnalyticsTracker) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = currentDestination?.route != Routes.SETTINGS

    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            destination.route?.let(analytics::logScreen)
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

    Scaffold(
        // Los testTag se ven como resource-id: el generador de Baseline Profile navega con ellos.
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                    TopLevel.entries.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = selected,
                            modifier = Modifier.testTag("nav_${item.route}"),
                            onClick = { navController.navigateTopLevel(item.route) },
                            icon = { Icon(painterResource(item.icon), contentDescription = null, modifier = Modifier.size(22.dp)) },
                            label = { Text(stringResource(item.label)) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController = navController, startDestination = Routes.PANEL) {
            composable(Routes.PANEL) {
                MonitorScreen(
                    onOpenBattery = { navController.navigateTopLevel(Routes.BATTERY) },
                    onOpenHardware = { navController.navigateTopLevel(Routes.HARDWARE) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    contentPadding = innerPadding
                )
            }
            composable(Routes.BATTERY) { BatteryScreen(contentPadding = innerPadding) }
            composable(Routes.HARDWARE) { HardwareScreen(contentPadding = innerPadding) }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() }, contentPadding = innerPadding)
            }
        }
    }
}

/** Navegación entre pestañas: una sola copia de cada destino y se conserva su estado. */
private fun NavController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
