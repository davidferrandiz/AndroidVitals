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
import androidx.compose.runtime.remember
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
import com.ferryapps.vitals.core.domain.model.BatteryHealthEstimate
import com.ferryapps.vitals.core.domain.model.BatteryHistory
import com.ferryapps.vitals.core.domain.model.BatteryInfo
import com.ferryapps.vitals.core.domain.model.BatteryStatus
import com.ferryapps.vitals.core.domain.model.PlugType
import com.ferryapps.vitals.designsystem.component.CardPadding
import com.ferryapps.vitals.designsystem.component.LinearMeter
import com.ferryapps.vitals.designsystem.component.MiniStat
import com.ferryapps.vitals.designsystem.component.MetricRow
import com.ferryapps.vitals.designsystem.component.RingGauge
import com.ferryapps.vitals.designsystem.component.SectionCard
import com.ferryapps.vitals.designsystem.component.Sparkline
import com.ferryapps.vitals.designsystem.component.StatusKind
import com.ferryapps.vitals.designsystem.component.StatusPill
import com.ferryapps.vitals.designsystem.component.TimePoint
import com.ferryapps.vitals.designsystem.component.TimeSeriesChart
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
    val history by viewModel.history.collectAsStateWithLifecycle()

    BatteryContent(battery, powerHistory, history, settings, contentPadding)
}

/** Pantalla sin ViewModel: la usan [BatteryScreen] y las capturas de Play Store (tests de Roborazzi). */
@Composable
fun BatteryContent(
    battery: BatteryInfo?,
    powerHistory: List<Float>,
    history: BatteryHistory?,
    settings: AppSettings,
    contentPadding: PaddingValues = PaddingValues(),
) {
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
        item(key = "history") { HistoryCard(history, settings) }
        if (settings.batteryHistoryEnabled) {
            item(key = "health") { HealthCard(history, info) }
        }
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

/** Nivel de las últimas 24 h con lo que guarda VitalsWorker; las bandas son tramos cargando. */
@Composable
private fun HistoryCard(history: BatteryHistory?, settings: AppSettings) {
    SectionCard(title = stringResource(R.string.battery_history_title)) {
        if (!settings.batteryHistoryEnabled) {
            HistoryNote(stringResource(R.string.battery_history_off))
            return@SectionCard
        }
        val samples = history?.last24h.orEmpty()
        if (samples.size < 2) {
            HistoryNote(stringResource(R.string.battery_history_collecting))
            return@SectionCard
        }
        val accent = VitalsTheme.palette.battery
        val now = remember(samples) { maxOf(System.currentTimeMillis(), samples.last().timestampMs) }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 12.dp)) {
            TimeSeriesChart(
                points = samples.map { TimePoint(it.timestampMs, it.levelPercent.toFloat(), highlighted = it.isCharging) },
                startMs = now - DAY_MS,
                endMs = now,
                color = accent,
                highlightColor = VitalsTheme.palette.good,
                description = stringResource(R.string.battery_history_cd),
                height = 104.dp
            )
            Row {
                Text(
                    stringResource(R.string.battery_history_axis_start),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    stringResource(R.string.battery_history_axis_end),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val stats = history?.stats
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                MiniStat(
                    label = stringResource(R.string.battery_drain_rate),
                    value = stats?.drainPercentPerHour?.let { "${Formatters.decimal(it, 1)} %/h" } ?: "—",
                    modifier = Modifier.weight(1f)
                )
                MiniStat(
                    label = stringResource(R.string.battery_charges_7d),
                    value = stats?.let { Formatters.integer(it.chargeSessions7d) } ?: "—",
                    modifier = Modifier.weight(1f)
                )
                MiniStat(
                    label = stringResource(R.string.battery_max_temp),
                    value = stats?.maxTemperature24hCelsius
                        ?.let { Formatters.temperature(it, settings.temperatureUnit, decimals = 0) } ?: "—",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HistoryNote(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp)
    )
}

/**
 * Salud estimada = capacidad estimada / capacidad de diseño. Si no hay contador de carga, o
 * no se mueve, no se inventa nada: se dice por qué no se puede calcular.
 */
@Composable
private fun HealthCard(history: BatteryHistory?, info: BatteryInfo) {
    val estimate = history?.health ?: BatteryHealthEstimate.EMPTY
    SectionCard(title = stringResource(R.string.battery_health_estimated_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            when {
                info.chargeCounterMah == null || estimate.counterUnreliable -> UnavailableRow(
                    title = stringResource(R.string.battery_health_estimated_title),
                    reason = stringResource(R.string.battery_health_no_counter_reason)
                )
                estimate.capacityMah == null -> {
                    val needed = BatteryHealthEstimate.MIN_SESSIONS
                    val done = estimate.validSessions.coerceAtMost(needed)
                    Text(
                        stringResource(R.string.battery_health_calculating, done, needed),
                        style = MaterialTheme.typography.titleMedium
                    )
                    LinearMeter(progress = done / needed.toFloat(), color = VitalsTheme.palette.battery)
                    HistoryNote(stringResource(R.string.battery_health_calculating_desc))
                }
                else -> HealthResult(history ?: return@Column, estimate.capacityMah ?: 0, estimate.validSessions)
            }
        }
    }
}

@Composable
private fun HealthResult(history: BatteryHistory, capacityMah: Int, sessions: Int) {
    val percent = history.healthPercent
    val design = history.designCapacityMah
    if (percent != null && design != null) {
        val kind = when {
            percent >= 80 -> StatusKind.GOOD
            percent >= 60 -> StatusKind.WARNING
            else -> StatusKind.CRITICAL
        }
        val label = when (kind) {
            StatusKind.GOOD -> R.string.battery_wear_good
            StatusKind.WARNING -> R.string.battery_wear_worn
            else -> R.string.battery_wear_bad
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "${Formatters.integer(percent)} %",
                style = MaterialTheme.typography.displaySmall.merge(TabularNumbers),
                fontWeight = FontWeight.ExtraBold
            )
            StatusPill(text = stringResource(label), kind = kind)
        }
        Text(
            stringResource(
                R.string.battery_health_estimated_caption,
                Formatters.integer(capacityMah),
                Formatters.integer(design),
                sessions
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        Text(
            "${Formatters.integer(capacityMah)} mAh",
            style = MaterialTheme.typography.displaySmall.merge(TabularNumbers),
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            stringResource(R.string.battery_capacity_estimated_caption, sessions),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Text(
        stringResource(R.string.battery_health_estimate_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private const val DAY_MS = 24 * 60 * 60 * 1000L

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
