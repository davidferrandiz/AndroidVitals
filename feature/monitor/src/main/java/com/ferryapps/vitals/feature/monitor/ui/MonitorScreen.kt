package com.ferryapps.vitals.feature.monitor.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ferryapps.vitals.core.domain.model.AppSettings
import com.ferryapps.vitals.core.domain.model.BatteryInfo
import com.ferryapps.vitals.core.domain.model.BatteryStatus
import com.ferryapps.vitals.core.domain.model.CpuSnapshot
import com.ferryapps.vitals.core.domain.model.MemoryInfo
import com.ferryapps.vitals.core.domain.model.NetworkInfo
import com.ferryapps.vitals.core.domain.model.NetworkSpeed
import com.ferryapps.vitals.core.domain.model.NetworkTransport
import com.ferryapps.vitals.core.domain.model.ProcessMemoryBreakdown
import com.ferryapps.vitals.core.domain.model.SignalQuality
import com.ferryapps.vitals.core.domain.model.StorageInfo
import com.ferryapps.vitals.core.domain.model.ThermalKind
import com.ferryapps.vitals.core.domain.model.ThermalState
import com.ferryapps.vitals.core.domain.model.ThermalStatus
import com.ferryapps.vitals.core.domain.model.ThermalZone
import com.ferryapps.vitals.core.domain.model.TopProcess
import com.ferryapps.vitals.core.domain.model.WifiBand
import com.ferryapps.vitals.core.domain.usecase.DeviceSummary
import com.ferryapps.vitals.designsystem.R as DsR
import com.ferryapps.vitals.designsystem.component.CardHeader
import com.ferryapps.vitals.designsystem.component.ChipRow
import com.ferryapps.vitals.designsystem.component.ExpandToggle
import com.ferryapps.vitals.designsystem.component.ExpandableContent
import com.ferryapps.vitals.designsystem.component.InfoChip
import com.ferryapps.vitals.designsystem.component.LinearMeter
import com.ferryapps.vitals.designsystem.component.MetricRow
import com.ferryapps.vitals.designsystem.component.MiniStat
import com.ferryapps.vitals.designsystem.component.NavigationRow
import com.ferryapps.vitals.designsystem.component.SectionLabel
import com.ferryapps.vitals.designsystem.component.Sparkline
import com.ferryapps.vitals.designsystem.component.StatusBanner
import com.ferryapps.vitals.designsystem.component.StatusKind
import com.ferryapps.vitals.designsystem.component.StatusPill
import com.ferryapps.vitals.designsystem.component.SummaryTile
import com.ferryapps.vitals.designsystem.component.UnavailableRow
import com.ferryapps.vitals.designsystem.component.VitalCard
import com.ferryapps.vitals.designsystem.format.Formatters
import com.ferryapps.vitals.designsystem.theme.TabularNumbers
import com.ferryapps.vitals.designsystem.theme.VitalsTheme
import com.ferryapps.vitals.feature.monitor.R

// ─────────────────────────────────────────────────────────────────────────────
// Pantalla
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MonitorScreen(
    onOpenBattery: () -> Unit,
    onOpenHardware: () -> Unit,
    onOpenSettings: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: MonitorViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val device by viewModel.deviceSummary.collectAsStateWithLifecycle()
    val expanded by viewModel.expandedCards.collectAsStateWithLifecycle()
    val cpu by viewModel.cpu.collectAsStateWithLifecycle()
    val cpuHistory by viewModel.cpuHistory.collectAsStateWithLifecycle()
    val thermalZones by viewModel.thermalZones.collectAsStateWithLifecycle()
    val thermalState by viewModel.thermalState.collectAsStateWithLifecycle()
    val memory by viewModel.memory.collectAsStateWithLifecycle()
    val storage by viewModel.storage.collectAsStateWithLifecycle()
    val battery by viewModel.battery.collectAsStateWithLifecycle()
    val speed by viewModel.networkSpeed.collectAsStateWithLifecycle()
    val networkHistory by viewModel.networkHistory.collectAsStateWithLifecycle()
    val network by viewModel.networkInfo.collectAsStateWithLifecycle()
    val appCpu by viewModel.appCpuPercent.collectAsStateWithLifecycle()
    val appThreads by viewModel.appThreads.collectAsStateWithLifecycle()
    val appMemory by viewModel.appMemory.collectAsStateWithLifecycle()
    val appProcesses by viewModel.appProcesses.collectAsStateWithLifecycle()

    MonitorContent(
        settings = settings,
        device = device,
        expanded = expanded,
        cpu = cpu,
        cpuHistory = cpuHistory,
        thermalZones = thermalZones,
        thermalState = thermalState,
        memory = memory,
        storage = storage,
        battery = battery,
        speed = speed,
        networkHistory = networkHistory,
        network = network,
        appCpu = appCpu,
        appThreads = appThreads,
        appMemory = appMemory,
        appProcesses = appProcesses,
        onToggleCard = viewModel::toggleCard,
        onOpenBattery = onOpenBattery,
        onOpenHardware = onOpenHardware,
        onOpenSettings = onOpenSettings,
        contentPadding = contentPadding,
    )
}

/** Pantalla sin ViewModel: la usan [MonitorScreen] y las capturas de Play Store (tests de Roborazzi). */
@Composable
fun MonitorContent(
    settings: AppSettings,
    device: DeviceSummary?,
    expanded: Set<String>,
    cpu: CpuSnapshot?,
    cpuHistory: List<Float>,
    thermalZones: List<ThermalZone>?,
    thermalState: ThermalState?,
    memory: MemoryInfo?,
    storage: StorageInfo?,
    battery: BatteryInfo?,
    speed: NetworkSpeed?,
    networkHistory: Pair<List<Float>, List<Float>>,
    network: NetworkInfo?,
    appCpu: Float,
    appThreads: Int,
    appMemory: ProcessMemoryBreakdown?,
    appProcesses: List<TopProcess>,
    onToggleCard: (String) -> Unit,
    onOpenBattery: () -> Unit,
    onOpenHardware: () -> Unit,
    onOpenSettings: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = max(contentPadding.calculateTopPadding(), 8.dp),
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") { PanelHeader(device, onOpenSettings) }

        item(key = "alerts") { AlertsCard(memory, storage) }

        item(key = "summary") { SummaryGrid(battery, memory, storage, settings) }

        item(key = "thermal") { ThermalBanner(thermalState) }

        item(key = "cpu") {
            CpuCard(
                cpu = cpu,
                history = cpuHistory,
                socName = device?.soc?.displayName,
                thermalZones = thermalZones,
                settings = settings,
                expanded = MonitorViewModel.CARD_CPU in expanded,
                onToggle = { onToggleCard(MonitorViewModel.CARD_CPU) },
                onOpenHardware = onOpenHardware
            )
        }

        item(key = "network") { NetworkCard(speed, networkHistory, network) }

        item(key = "battery") { BatteryRow(battery, onOpenBattery) }

        item(key = "vitals") {
            VitalsUsageCard(
                cpuPercent = appCpu,
                threads = appThreads,
                memory = appMemory,
                processes = appProcesses,
                expanded = MonitorViewModel.CARD_VITALS in expanded,
                onToggle = { onToggleCard(MonitorViewModel.CARD_VITALS) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Cabecera y avisos
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PanelHeader(device: DeviceSummary?, onOpenSettings: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.panel_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() }
            )
            if (device != null) {
                Text(
                    stringResource(R.string.panel_device_subtitle, device.deviceName, device.androidVersion),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onOpenSettings) {
            Icon(painterResource(DsR.drawable.ic_tune), contentDescription = stringResource(R.string.panel_settings_cd))
        }
    }
}

/** Memoria baja y espacio por debajo del 10 %: lo que más preocupa en la gama baja. */
@Composable
private fun AlertsCard(memory: MemoryInfo?, storage: StorageInfo?) {
    val context = LocalContext.current
    val lowMemory = memory?.lowMemory == true
    val lowStorage = storage != null && storage.totalBytes > 0 && storage.freeBytes.toFloat() / storage.totalBytes < 0.10f
    if (!lowMemory && !lowStorage) return

    val warning = VitalsTheme.palette.warning
    VitalCard(borderColor = warning.copy(alpha = 0.45f)) {
        if (lowMemory) {
            AlertItem(
                title = stringResource(R.string.panel_alert_low_memory_title),
                message = stringResource(R.string.panel_alert_low_memory_msg),
                icon = DsR.drawable.ic_warning,
                color = warning
            )
        }
        if (lowMemory && lowStorage) HorizontalDivider(color = VitalsTheme.palette.divider)
        if (lowStorage && storage != null) {
            AlertItem(
                title = stringResource(R.string.panel_alert_storage_title),
                message = stringResource(
                    R.string.panel_alert_storage_msg,
                    Formatters.bytes(context, storage.freeBytes),
                    Formatters.bytes(context, storage.totalBytes)
                ),
                icon = DsR.drawable.ic_storage,
                color = warning
            )
            Text(
                stringResource(R.string.panel_alert_storage_action),
                style = MaterialTheme.typography.labelLarge,
                color = warning,
                modifier = Modifier
                    .padding(start = 34.dp)
                    .heightIn(min = 44.dp)
                    .clickable(role = Role.Button) {
                        context.startActivity(
                            Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                    .padding(vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun AlertItem(title: String, message: String, icon: Int, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(painterResource(icon), contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Resumen 2×2
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SummaryGrid(battery: BatteryInfo?, memory: MemoryInfo?, storage: StorageInfo?, settings: AppSettings) {
    val context = LocalContext.current
    val palette = VitalsTheme.palette
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryTile(
                label = stringResource(R.string.card_battery),
                icon = if (battery?.isCharging == true) DsR.drawable.ic_battery_charging else DsR.drawable.ic_battery,
                accent = palette.battery,
                value = battery?.let { Formatters.integer(it.levelPercent) } ?: "—",
                unit = "%",
                progress = battery?.levelPercent?.div(100f),
                caption = battery?.let { batteryCaption(it) } ?: "",
                modifier = Modifier.weight(1f)
            )
            val ramHigh = memory != null && (memory.lowMemory || memory.usedPercent >= 85f)
            SummaryTile(
                label = stringResource(R.string.card_ram),
                icon = DsR.drawable.ic_memory,
                accent = palette.memory,
                value = memory?.let { Formatters.decimal(it.usedPercent, 0) } ?: "—",
                unit = "%",
                progress = memory?.usedPercent?.div(100f),
                caption = memory?.let {
                    stringResource(
                        R.string.tile_ram_caption,
                        Formatters.bytes(context, it.usedKb * 1024),
                        Formatters.bytes(context, it.totalKb * 1024),
                        stringResource(if (ramHigh) R.string.state_high else R.string.state_normal)
                    )
                } ?: "",
                warning = ramHigh,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val temp = battery?.temperatureCelsius
            val tempValue = temp?.let { Formatters.temperatureValue(it, settings.temperatureUnit) }
            val tempState = when {
                temp == null -> R.string.state_normal
                temp >= 45f -> R.string.state_hot
                temp >= 40f -> R.string.state_warm
                else -> R.string.state_normal
            }
            SummaryTile(
                label = stringResource(R.string.tile_temperature),
                icon = DsR.drawable.ic_thermometer,
                accent = palette.network,
                value = tempValue?.first ?: "—",
                unit = tempValue?.second,
                // 0 °C → 60 °C como escala visual.
                progress = temp?.div(60f),
                caption = stringResource(R.string.tile_temperature_caption, stringResource(tempState)),
                warning = temp != null && temp >= 40f,
                modifier = Modifier.weight(1f)
            )
            val lowStorage = storage != null && storage.totalBytes > 0 && storage.freeBytes.toFloat() / storage.totalBytes < 0.10f
            SummaryTile(
                label = stringResource(R.string.card_storage),
                icon = DsR.drawable.ic_storage,
                accent = palette.storage,
                value = storage?.let { Formatters.decimal(it.usedPercent, 0) } ?: "—",
                unit = "%",
                progress = storage?.usedPercent?.div(100f),
                caption = storage?.let {
                    stringResource(
                        R.string.tile_storage_caption,
                        Formatters.bytes(context, it.usedBytes),
                        Formatters.bytes(context, it.totalBytes),
                        Formatters.bytes(context, it.freeBytes)
                    )
                } ?: "",
                warning = lowStorage,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun batteryCaption(battery: BatteryInfo): String {
    val context = LocalContext.current
    return when (battery.status) {
        BatteryStatus.CHARGING -> battery.chargeTimeRemainingMs
            ?.let { stringResource(R.string.tile_battery_charging_eta, Formatters.duration(context, it)) }
            ?: stringResource(R.string.battery_status_charging)
        BatteryStatus.DISCHARGING -> battery.powerWatts
            ?.let { stringResource(R.string.tile_battery_discharging_power, Formatters.power(it)) }
            ?: stringResource(R.string.battery_status_discharging)
        BatteryStatus.FULL -> stringResource(R.string.battery_status_full)
        BatteryStatus.NOT_CHARGING -> stringResource(R.string.battery_status_not_charging)
        BatteryStatus.UNKNOWN -> stringResource(R.string.battery_status_unknown)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Detector de throttling
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ThermalBanner(state: ThermalState?) {
    if (state == null || state.status == ThermalStatus.UNAVAILABLE) return
    val margin = state.marginPercent?.let { stringResource(R.string.thermal_margin, it) }
    when (state.status) {
        ThermalStatus.NONE, ThermalStatus.LIGHT -> StatusBanner(
            kind = StatusKind.GOOD,
            icon = DsR.drawable.ic_shield_check,
            title = stringResource(R.string.thermal_ok_title),
            message = margin
        )
        ThermalStatus.MODERATE -> StatusBanner(
            kind = StatusKind.WARNING,
            icon = DsR.drawable.ic_thermometer,
            title = stringResource(R.string.thermal_moderate_title),
            message = stringResource(R.string.thermal_throttling_msg)
        )
        else -> StatusBanner(
            kind = StatusKind.CRITICAL,
            icon = DsR.drawable.ic_thermometer,
            title = stringResource(R.string.thermal_severe_title),
            message = stringResource(R.string.thermal_throttling_msg)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CPU
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CpuCard(
    cpu: CpuSnapshot?,
    history: List<Float>,
    socName: String?,
    thermalZones: List<ThermalZone>?,
    settings: AppSettings,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenHardware: () -> Unit,
) {
    val accent = VitalsTheme.palette.cpu
    val coreCount = cpu?.cores?.size ?: 0
    VitalCard {
        // La cabecera lleva a Hardware: ahí está el detalle del SoC, la GPU y el estado térmico.
        CardHeader(
            icon = DsR.drawable.ic_cpu,
            accent = accent,
            title = stringResource(R.string.card_cpu),
            subtitle = if (socName != null && coreCount > 0) stringResource(R.string.cpu_subtitle, socName, coreCount) else socName,
            value = cpu?.let { Formatters.percent(it.estimatedLoadPercent) } ?: "—",
            valueLabel = stringResource(R.string.cpu_estimated_load),
            modifier = Modifier.clickable(
                onClickLabel = stringResource(R.string.cpu_open_hardware),
                role = Role.Button,
                onClick = onOpenHardware
            ),
            trailing = {
                Spacer(Modifier.width(6.dp))
                Icon(
                    painterResource(DsR.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        )
        Sparkline(
            values = history,
            color = accent,
            description = stringResource(R.string.cpu_chart_cd),
            maxValue = 100f,
            capacity = MonitorViewModel.HISTORY_SIZE
        )
        cpu?.clusters?.forEach { cluster ->
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row {
                    Text(
                        cluster.coreName?.let { stringResource(R.string.cpu_cluster_named, cluster.coreCount, it) }
                            ?: stringResource(R.string.cpu_cluster_unnamed, cluster.coreCount),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        stringResource(
                            R.string.cpu_freq_of,
                            Formatters.frequency(cluster.currentMhz),
                            Formatters.frequency(cluster.maxMhz)
                        ),
                        style = MaterialTheme.typography.bodyMedium.merge(TabularNumbers),
                        color = accent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                LinearMeter(
                    progress = if (cluster.maxMhz > 0) cluster.currentMhz.toFloat() / cluster.maxMhz else 0f,
                    color = accent,
                    height = 6.dp
                )
            }
        }
        if (coreCount > 0) {
            ExpandToggle(
                label = if (expanded) stringResource(R.string.cpu_hide_details) else stringResource(R.string.cpu_show_details, coreCount),
                expanded = expanded,
                accent = accent,
                onToggle = onToggle
            )
        }
        ExpandableContent(visible = expanded) {
            SectionLabel(stringResource(R.string.cpu_section_cores))
            cpu?.cores?.forEach { core ->
                MetricRow(
                    label = core.coreName?.let { stringResource(R.string.cpu_core_named, core.index, it) }
                        ?: stringResource(R.string.core_name, core.index),
                    value = if (core.isOnline) Formatters.frequency(core.currentMhz) else stringResource(R.string.core_offline),
                    valueColor = if (core.isOnline) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    divider = true
                )
            }
            Spacer(Modifier.width(1.dp).heightIn(min = 8.dp))
            SectionLabel(stringResource(R.string.cpu_section_temps))
            when {
                thermalZones == null -> Unit
                thermalZones.isEmpty() -> UnavailableRow(
                    title = stringResource(R.string.temps_unavailable_title),
                    reason = stringResource(R.string.temps_unavailable_reason)
                )
                else -> thermalZones.forEach { zone ->
                    MetricRow(
                        label = stringResource(R.string.thermal_zone_label, stringResource(zone.kind.labelRes()), zone.name),
                        value = Formatters.temperature(zone.temperatureCelsius, settings.temperatureUnit),
                        divider = true
                    )
                }
            }
        }
    }
}

private fun ThermalKind.labelRes(): Int = when (this) {
    ThermalKind.CPU -> R.string.thermal_kind_cpu
    ThermalKind.GPU -> R.string.thermal_kind_gpu
    ThermalKind.SOC -> R.string.thermal_kind_soc
    ThermalKind.BOARD -> R.string.thermal_kind_board
    ThermalKind.BATTERY -> R.string.thermal_kind_battery
    ThermalKind.SKIN -> R.string.thermal_kind_skin
    ThermalKind.OTHER -> R.string.thermal_kind_other
}

// ─────────────────────────────────────────────────────────────────────────────
// Red
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NetworkCard(speed: NetworkSpeed?, history: Pair<List<Float>, List<Float>>, network: NetworkInfo?) {
    val accent = VitalsTheme.palette.network
    val icon = when (network?.transport) {
        NetworkTransport.CELLULAR -> DsR.drawable.ic_cellular
        NetworkTransport.NONE -> DsR.drawable.ic_no_network
        else -> DsR.drawable.ic_wifi
    }
    VitalCard {
        CardHeader(
            icon = icon,
            accent = accent,
            title = stringResource(R.string.card_network),
            trailing = {
                if (speed != null) {
                    Text(
                        "↓ ${speed.rxFormatted()}",
                        style = MaterialTheme.typography.titleSmall.merge(TabularNumbers),
                        color = accent,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "↑ ${speed.txFormatted()}",
                        style = MaterialTheme.typography.titleSmall.merge(TabularNumbers),
                        color = accent.copy(alpha = 0.75f)
                    )
                }
            }
        )
        Sparkline(
            values = history.first,
            secondaryValues = history.second,
            color = accent,
            description = stringResource(R.string.network_chart_cd),
            capacity = MonitorViewModel.HISTORY_SIZE,
            height = 44.dp,
            fill = false
        )
        if (network != null) {
            ChipRow {
                networkChips(network).forEach { InfoChip(it) }
            }
        }
    }
}

@Composable
private fun networkChips(network: NetworkInfo): List<String> {
    val wifi = network.wifi
    return when (network.transport) {
        NetworkTransport.WIFI -> buildList {
            add(wifi?.generation?.let { stringResource(R.string.net_wifi_gen, it) } ?: stringResource(R.string.net_wifi))
            when (wifi?.band) {
                WifiBand.GHZ_2_4 -> add(stringResource(R.string.net_band_2_4))
                WifiBand.GHZ_5 -> add(stringResource(R.string.net_band_5))
                WifiBand.GHZ_6 -> add(stringResource(R.string.net_band_6))
                null -> Unit
            }
            val rssi = wifi?.rssiDbm
            val quality = wifi?.signalQuality
            if (rssi != null && quality != null) {
                val qualityText = stringResource(
                    when (quality) {
                        SignalQuality.GOOD -> R.string.signal_good
                        SignalQuality.FAIR -> R.string.signal_fair
                        SignalQuality.POOR -> R.string.signal_poor
                    }
                )
                add(stringResource(R.string.net_signal, rssi, qualityText))
            }
            wifi?.linkSpeedMbps?.let { add(stringResource(R.string.net_link, it)) }
        }
        NetworkTransport.CELLULAR -> listOf(stringResource(R.string.net_cellular))
        NetworkTransport.ETHERNET -> listOf(stringResource(R.string.net_ethernet))
        NetworkTransport.VPN -> listOf(stringResource(R.string.net_vpn))
        NetworkTransport.OTHER -> listOf(stringResource(R.string.net_other))
        NetworkTransport.NONE -> listOf(stringResource(R.string.net_none))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Batería (acceso a la pantalla de detalle)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BatteryRow(battery: BatteryInfo?, onOpenBattery: () -> Unit) {
    val context = LocalContext.current
    val subtitle = battery?.let { b ->
        val status = stringResource(b.status.labelRes())
        val current = b.currentNowMa
        val power = b.powerWatts
        val remaining = b.estimatedRemainingMs
        when {
            remaining != null && current != null ->
                stringResource(R.string.battery_row_eta, Formatters.current(current), Formatters.duration(context, remaining))
            current != null && power != null ->
                stringResource(R.string.battery_row_detail, status, Formatters.current(current), Formatters.power(power))
            else -> status
        }
    }
    VitalCard {
        NavigationRow(
            title = stringResource(R.string.card_battery),
            icon = DsR.drawable.ic_battery,
            accent = VitalsTheme.palette.battery,
            subtitle = subtitle,
            onClick = onOpenBattery
        )
    }
}

private fun BatteryStatus.labelRes(): Int = when (this) {
    BatteryStatus.CHARGING -> R.string.battery_status_charging
    BatteryStatus.DISCHARGING -> R.string.battery_status_discharging
    BatteryStatus.FULL -> R.string.battery_status_full
    BatteryStatus.NOT_CHARGING -> R.string.battery_status_not_charging
    BatteryStatus.UNKNOWN -> R.string.battery_status_unknown
}

// ─────────────────────────────────────────────────────────────────────────────
// Consumo de Vitals
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VitalsUsageCard(
    cpuPercent: Float,
    threads: Int,
    memory: ProcessMemoryBreakdown?,
    processes: List<TopProcess>,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val accent = VitalsTheme.palette.memory
    val pssMb = memory?.totalPssKb?.div(1024)
    val isLight = cpuPercent < 5f && (pssMb == null || pssMb < 150)
    VitalCard {
        CardHeader(
            icon = DsR.drawable.ic_pulse,
            accent = accent,
            title = stringResource(R.string.vitals_title),
            trailing = { if (isLight) StatusPill(stringResource(R.string.vitals_light), StatusKind.GOOD) }
        )
        Text(
            stringResource(R.string.vitals_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            val tile = Modifier.weight(1f).fillMaxHeight()
            MiniStat(stringResource(R.string.vitals_cpu), Formatters.percent(cpuPercent, 1), tile)
            MiniStat(stringResource(R.string.active_threads), Formatters.integer(threads), tile)
            MiniStat(stringResource(R.string.vitals_pss), pssMb?.let { "${Formatters.integer(it)} MB" } ?: "—", tile)
        }
        ExpandToggle(
            label = stringResource(if (expanded) R.string.vitals_hide else R.string.vitals_show),
            expanded = expanded,
            accent = accent,
            onToggle = onToggle
        )
        ExpandableContent(visible = expanded) {
            if (memory != null) {
                SectionLabel(stringResource(R.string.vitals_memory_section, memory.pid))
                MemoryRow(R.string.metric_total_pss, memory.totalPssKb)
                MemoryRow(R.string.metric_java_heap, memory.javaHeapKb)
                MemoryRow(R.string.metric_native_heap, memory.nativeHeapKb)
                MemoryRow(R.string.metric_graphics, memory.graphicsKb)
                MemoryRow(R.string.metric_code, memory.codeKb)
                MemoryRow(R.string.metric_stack, memory.stackKb)
                MemoryRow(R.string.metric_other, memory.otherKb)
            }
            if (processes.isNotEmpty()) {
                Spacer(Modifier.heightIn(min = 8.dp))
                SectionLabel(stringResource(R.string.vitals_processes_section))
                processes.forEach { process ->
                    MetricRow(
                        label = process.name,
                        value = if (process.ramKb > 1024) "${Formatters.integer(process.ramKb / 1024)} MB"
                        else "${Formatters.integer(process.ramKb)} KB",
                        valueColor = VitalsTheme.palette.apps,
                        divider = true
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoryRow(labelRes: Int, kb: Int) {
    MetricRow(
        label = stringResource(labelRes),
        value = "${Formatters.integer(kb / 1024)} MB",
        divider = true
    )
}
