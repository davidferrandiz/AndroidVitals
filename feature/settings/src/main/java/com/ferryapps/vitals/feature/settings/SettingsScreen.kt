package com.ferryapps.vitals.feature.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ferryapps.vitals.core.domain.model.AppSettings
import com.ferryapps.vitals.core.domain.model.TemperatureUnit
import com.ferryapps.vitals.core.domain.model.ThemeMode
import com.ferryapps.vitals.designsystem.R as DsR
import com.ferryapps.vitals.designsystem.component.MetricRow
import com.ferryapps.vitals.designsystem.component.NavigationRow
import com.ferryapps.vitals.designsystem.component.SectionCard
import com.ferryapps.vitals.designsystem.component.SegmentedSelector
import com.ferryapps.vitals.designsystem.component.SwitchRow
import com.ferryapps.vitals.designsystem.theme.VitalsTheme

private const val PRIVACY_POLICY_URL = "https://davidferrandiz.github.io/AndroidVitals/privacy-policy.html"
private const val SUPPORT_EMAIL = "ferry.applications@gmail.com"

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = max(contentPadding.calculateTopPadding(), 8.dp),
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "title") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painterResource(DsR.drawable.ic_back), contentDescription = stringResource(R.string.settings_back))
                }
                Text(
                    stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() }
                )
            }
        }
        val current = settings ?: return@LazyColumn
        item(key = "view") { ViewSection(current, viewModel) }
        item(key = "background") { BackgroundSection(current, viewModel) }
        item(key = "privacy") { PrivacySection(current, viewModel, context) }
        item(key = "about") { AboutSection(context) }
    }
}

@Composable
private fun ViewSection(settings: AppSettings, viewModel: SettingsViewModel) {
    SectionCard(title = stringResource(R.string.settings_section_view)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
            Text(stringResource(R.string.settings_temperature), style = MaterialTheme.typography.bodyLarge)
            SegmentedSelector(
                options = listOf(
                    TemperatureUnit.AUTO to stringResource(R.string.settings_auto),
                    TemperatureUnit.CELSIUS to "°C",
                    TemperatureUnit.FAHRENHEIT to "°F",
                ),
                selected = settings.temperatureUnit,
                onSelect = viewModel::setTemperatureUnit
            )
            Text(
                stringResource(R.string.settings_temperature_auto_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(color = VitalsTheme.palette.divider)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 12.dp)) {
            Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.bodyLarge)
            SegmentedSelector(
                options = listOf(
                    ThemeMode.DARK to stringResource(R.string.settings_theme_dark),
                    ThemeMode.LIGHT to stringResource(R.string.settings_theme_light),
                    ThemeMode.SYSTEM to stringResource(R.string.settings_theme_system),
                ),
                selected = settings.themeMode,
                onSelect = viewModel::setThemeMode
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            HorizontalDivider(color = VitalsTheme.palette.divider)
            SwitchRow(
                title = stringResource(R.string.settings_dynamic_color),
                description = stringResource(R.string.settings_dynamic_color_desc),
                checked = settings.dynamicColor,
                onCheckedChange = viewModel::setDynamicColor
            )
        }
        HorizontalDivider(color = VitalsTheme.palette.divider)
        SwitchRow(
            title = stringResource(R.string.settings_expand_cards),
            description = stringResource(R.string.settings_expand_cards_desc),
            checked = settings.expandCardsByDefault,
            onCheckedChange = viewModel::setExpandCardsByDefault
        )
    }
}

/** Lo que Vitals hace con la app cerrada (VitalsWorker cada 15 min). */
@Composable
private fun BackgroundSection(settings: AppSettings, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    // Se vuelve a mirar al volver de los ajustes del sistema.
    var notificationsAllowed by remember { mutableStateOf(context.notificationsAllowed()) }
    var showBlocked by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        notificationsAllowed = context.notificationsAllowed()
        if (notificationsAllowed) showBlocked = false
        onPauseOrDispose { }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsAllowed = granted && context.notificationsAllowed()
        viewModel.setAlertsEnabled(notificationsAllowed)
        // Tras dos "no", Android ya no muestra el diálogo y responde false al momento.
        showBlocked = !notificationsAllowed
    }

    fun onAlertsToggle(enable: Boolean) {
        when {
            !enable -> viewModel.setAlertsEnabled(false)
            notificationsAllowed -> viewModel.setAlertsEnabled(true)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !context.hasNotificationPermission() ->
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else -> showBlocked = true
        }
    }

    SectionCard(title = stringResource(R.string.settings_section_background)) {
        SwitchRow(
            title = stringResource(R.string.settings_battery_history),
            description = stringResource(R.string.settings_battery_history_desc),
            checked = settings.batteryHistoryEnabled,
            onCheckedChange = viewModel::setBatteryHistoryEnabled
        )
        HorizontalDivider(color = VitalsTheme.palette.divider)
        SwitchRow(
            title = stringResource(R.string.settings_alerts),
            description = stringResource(R.string.settings_alerts_desc),
            checked = settings.alertsEnabled && notificationsAllowed,
            onCheckedChange = ::onAlertsToggle
        )
        if (showBlocked || (settings.alertsEnabled && !notificationsAllowed)) {
            Text(
                stringResource(R.string.settings_alerts_blocked),
                style = MaterialTheme.typography.bodySmall,
                color = VitalsTheme.palette.warning
            )
            NavigationRow(
                title = stringResource(R.string.settings_alerts_open_system),
                onClick = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        context.openUri(Uri.parse("package:${context.packageName}"), Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    }
                }
            )
        }
    }
}

private fun Context.hasNotificationPermission(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun Context.notificationsAllowed(): Boolean =
    hasNotificationPermission() && NotificationManagerCompat.from(this).areNotificationsEnabled()

@Composable
private fun PrivacySection(settings: AppSettings, viewModel: SettingsViewModel, context: Context) {
    SectionCard(title = stringResource(R.string.settings_section_privacy)) {
        SwitchRow(
            title = stringResource(R.string.settings_analytics),
            description = stringResource(R.string.settings_analytics_desc),
            checked = settings.analyticsEnabled,
            onCheckedChange = viewModel::setAnalyticsEnabled
        )
        HorizontalDivider(color = VitalsTheme.palette.divider)
        SwitchRow(
            title = stringResource(R.string.settings_crash_reports),
            description = stringResource(R.string.settings_crash_reports_desc),
            checked = settings.crashReportsEnabled,
            onCheckedChange = viewModel::setCrashReportsEnabled
        )
        HorizontalDivider(color = VitalsTheme.palette.divider)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 12.dp)) {
            Text(stringResource(R.string.settings_what_is_sent), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.settings_what_is_sent_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        NavigationRow(
            title = stringResource(R.string.settings_privacy_policy),
            onClick = { context.openUri(Uri.parse(PRIVACY_POLICY_URL)) }
        )
    }
}

@Composable
private fun AboutSection(context: Context) {
    val version = remember(context) { context.appVersion() }
    val feedbackSubject = stringResource(R.string.settings_feedback_subject)
    SectionCard(title = stringResource(R.string.settings_section_about)) {
        NavigationRow(
            title = stringResource(R.string.settings_rate),
            onClick = {
                val market = Uri.parse("market://details?id=${context.packageName}")
                val web = Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                if (!context.openUri(market)) context.openUri(web)
            }
        )
        HorizontalDivider(color = VitalsTheme.palette.divider)
        NavigationRow(
            title = stringResource(R.string.settings_feedback),
            subtitle = stringResource(R.string.settings_feedback_desc),
            onClick = {
                val body = "\n\n—\n${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) · Vitals $version"
                val uri = Uri.parse("mailto:$SUPPORT_EMAIL?subject=${Uri.encode(feedbackSubject)}&body=${Uri.encode(body)}")
                context.openUri(uri, Intent.ACTION_SENDTO)
            }
        )
        HorizontalDivider(color = VitalsTheme.palette.divider)
        MetricRow(label = stringResource(R.string.settings_version), value = version)
    }
}

/** Abre un enlace; devuelve false si no hay ninguna app que lo gestione. */
private fun Context.openUri(uri: Uri, action: String = Intent.ACTION_VIEW): Boolean = try {
    startActivity(Intent(action, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (e: ActivityNotFoundException) {
    false
}

private fun Context.appVersion(): String = try {
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0)
    }
    info.versionName ?: "—"
} catch (e: PackageManager.NameNotFoundException) {
    "—"
}
