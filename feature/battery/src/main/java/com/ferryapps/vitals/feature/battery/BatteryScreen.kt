package com.ferryapps.vitals.feature.battery

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ferryapps.vitals.core.domain.model.AppSettings
import com.ferryapps.vitals.core.domain.model.BatteryHealth
import com.ferryapps.vitals.core.domain.model.BatteryInfo
import com.ferryapps.vitals.core.domain.model.BatteryStatus
import com.ferryapps.vitals.core.domain.model.PlugType
import com.ferryapps.vitals.designsystem.component.CardPadding
import com.ferryapps.vitals.designsystem.component.MetricRow
import com.ferryapps.vitals.designsystem.component.RingGauge
import com.ferryapps.vitals.designsystem.component.SectionCard
import com.ferryapps.vitals.designsystem.component.Sparkline
import com.ferryapps.vitals.designsystem.component.StatusKind
import com.ferryapps.vitals.designsystem.component.UnavailableRow
import com.ferryapps.vitals.designsystem.component.VitalCard
import com.ferryapps.vitals.designsystem.component.color
import com.ferryapps.vitals.designsystem.format.Formatters
import com.ferryapps.vitals.designsystem.theme.TabularNumbers
import com.ferryapps.vitals.designsystem.theme.VitalsTheme

@Composable
fun BatteryScreen(
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: BatteryViewModel = hiltViewModel(),
) {
    val battery by viewModel.battery.collectAsStateWithLifecycle()
    val powerHistory by viewModel.powerHistory.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = max(contentPadding.calculateTopPadding(), 8.dp) + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "title") {
            Text(
                stringResource(R.string.battery_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .padding(start = 4.dp, bottom = 4.dp)
                    .semantics { heading() }
            )
        }
        val info = battery
        if (info == null) return@LazyColumn
        item(key = "hero") { HeroCard(info, powerHistory) }
        item(key = "details") { DetailsGrid(info, settings) }
        item(key = "charger") { ChargerCard(info) }
    }
}

@Composable
private fun HeroCard(info: BatteryInfo, powerHistory: List<Float>) {
    val context = LocalContext.current
    val accent = VitalsTheme.palette.battery
    val power = info.powerWatts
    VitalCard(contentPadding = CardPadding.Roomy) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            RingGauge(
                progress = info.levelPercent / 100f,
                color = accent,
                centerText = "${Formatters.integer(info.levelPercent)} %"
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(info.status.labelRes()).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
                Text(
                    power?.let { Formatters.power(it) } ?: "—",
                    style = MaterialTheme.typography.displaySmall.merge(TabularNumbers)
                )
                val eta = when {
                    info.chargeTimeRemainingMs != null ->
                        stringResource(R.string.battery_full_in, Formatters.duration(context, info.chargeTimeRemainingMs ?: 0L))
                    info.estimatedRemainingMs != null ->
                        stringResource(R.string.battery_remaining, Formatters.duration(context, info.estimatedRemainingMs ?: 0L))
                    else -> null
                }
                if (eta != null) {
                    Text(eta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (power != null) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.battery_power_history),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Sparkline(
                    values = powerHistory,
                    color = accent,
                    description = stringResource(R.string.battery_power_history),
                    capacity = BatteryViewModel.HISTORY_SIZE,
                    height = 64.dp
                )
            }
        } else {
            UnavailableRow(
                title = stringResource(R.string.battery_current_title),
                reason = stringResource(R.string.battery_current_reason)
            )
        }
    }
}

@Composable
private fun DetailsGrid(info: BatteryInfo, settings: AppSettings) {
    val palette = VitalsTheme.palette
    val tempKind = when {
        info.temperatureCelsius >= 45f -> StatusKind.CRITICAL
        info.temperatureCelsius >= 40f -> StatusKind.WARNING
        else -> StatusKind.GOOD
    }
    val tempLabel = when (tempKind) {
        StatusKind.CRITICAL -> R.string.battery_temp_hot
        StatusKind.WARNING -> R.string.battery_temp_warm
        else -> R.string.battery_temp_normal
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailTile(
                label = stringResource(R.string.battery_current),
                value = info.currentNowMa?.let { Formatters.current(it) } ?: "—",
                caption = info.currentAverageMa?.let { stringResource(R.string.battery_current_avg, Formatters.current(it)) },
                modifier = Modifier.weight(1f)
            )
            DetailTile(
                label = stringResource(R.string.battery_voltage),
                value = "${Formatters.integer(info.voltageMillivolts)} mV",
                caption = info.technology,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailTile(
                label = stringResource(R.string.battery_temperature),
                value = Formatters.temperature(info.temperatureCelsius, settings.temperatureUnit),
                caption = stringResource(tempLabel),
                captionColor = tempKind.color(),
                modifier = Modifier.weight(1f)
            )
            DetailTile(
                label = stringResource(R.string.battery_charge_counter),
                value = info.chargeCounterMah?.let { "${Formatters.integer(it)} mAh" } ?: "—",
                caption = null,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailTile(
                label = stringResource(R.string.battery_cycles),
                value = info.cycleCount?.let { Formatters.integer(it) } ?: "—",
                caption = when {
                    info.cycleCount != null -> stringResource(R.string.battery_cycles_source)
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> stringResource(R.string.battery_cycles_needs_14)
                    else -> stringResource(R.string.battery_cycles_not_reported)
                },
                modifier = Modifier.weight(1f)
            )
            DetailTile(
                label = stringResource(R.string.battery_health),
                value = stringResource(info.health.labelRes()),
                caption = stringResource(R.string.battery_health_source),
                captionColor = if (info.health == BatteryHealth.GOOD) palette.good else null,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DetailTile(
    label: String,
    value: String,
    caption: String?,
    modifier: Modifier = Modifier,
    captionColor: Color? = null,
) {
    VitalCard(modifier = modifier, contentPadding = CardPadding.Compact) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge.merge(TabularNumbers), fontWeight = FontWeight.ExtraBold, maxLines = 1)
            if (caption != null) {
                Text(
                    caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = captionColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (captionColor != null) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun ChargerCard(info: BatteryInfo) {
    SectionCard(title = stringResource(R.string.battery_charger)) {
        MetricRow(
            label = stringResource(R.string.battery_charger_type),
            value = stringResource(
                when (info.plugType) {
                    PlugType.AC -> R.string.battery_plug_ac
                    PlugType.USB -> R.string.battery_plug_usb
                    PlugType.WIRELESS -> R.string.battery_plug_wireless
                    PlugType.DOCK -> R.string.battery_plug_dock
                    PlugType.NONE -> R.string.battery_plug_none
                }
            ),
            divider = true
        )
        MetricRow(label = stringResource(R.string.battery_status), value = stringResource(info.status.labelRes()))
    }
}

private fun BatteryStatus.labelRes(): Int = when (this) {
    BatteryStatus.CHARGING -> R.string.battery_status_charging
    BatteryStatus.DISCHARGING -> R.string.battery_status_discharging
    BatteryStatus.FULL -> R.string.battery_status_full
    BatteryStatus.NOT_CHARGING -> R.string.battery_status_not_charging
    BatteryStatus.UNKNOWN -> R.string.battery_status_unknown
}

private fun BatteryHealth.labelRes(): Int = when (this) {
    BatteryHealth.GOOD -> R.string.battery_health_good
    BatteryHealth.OVERHEAT -> R.string.battery_health_overheat
    BatteryHealth.DEAD -> R.string.battery_health_dead
    BatteryHealth.OVER_VOLTAGE -> R.string.battery_health_over_voltage
    BatteryHealth.COLD -> R.string.battery_health_cold
    BatteryHealth.UNKNOWN -> R.string.battery_health_unknown
}
