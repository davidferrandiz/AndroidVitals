package com.ferryapps.vitals.feature.monitor.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ferryapps.vitals.feature.monitor.R
import com.ferryapps.vitals.core.domain.model.BatteryInfo
import com.ferryapps.vitals.core.domain.model.CpuCoreInfo
import com.ferryapps.vitals.core.domain.model.NetworkSpeed
import com.ferryapps.vitals.core.domain.model.ProcessMemoryBreakdown
import com.ferryapps.vitals.core.domain.model.StorageInfo
import com.ferryapps.vitals.core.domain.model.ThermalZone
import com.ferryapps.vitals.core.domain.model.TopProcess

// ── Accent colors por categoría ───────────────────────────────────────────────
private val ColorCpu       = Color(0xFF4FC3F7)
private val ColorMemory    = Color(0xFFCE93D8)
private val ColorBattery   = Color(0xFF81C784)
private val ColorNetwork   = Color(0xFFFFB74D)
private val ColorStorage   = Color(0xFF4DB6AC)
private val ColorProcesses = Color(0xFFEF9A9A)

private val LabelStyle @Composable get() = MaterialTheme.typography.labelSmall.copy(
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    letterSpacing = 0.8.sp
)

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MonitorScreen(
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: MonitorViewModel = hiltViewModel()
) {
    val uiState       by viewModel.uiState.collectAsState()
    val expandedCards by viewModel.expandedCards.collectAsState()
    val memExpanded   by viewModel.memoryExpanded.collectAsState()
    val memBreakdown  by viewModel.memoryBreakdown.collectAsState()
    val battery       by viewModel.battery.collectAsState()
    val cpuCores      by viewModel.cpuCores.collectAsState()
    val thermals      by viewModel.thermalZones.collectAsState()
    val network       by viewModel.networkSpeed.collectAsState()
    val storage       by viewModel.storage.collectAsState()
    val processes     by viewModel.topProcesses.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start  = 16.dp, end = 16.dp,
            top    = max(contentPadding.calculateTopPadding(), 16.dp),
            bottom = max(contentPadding.calculateBottomPadding(), 80.dp)
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ── CPU ──────────────────────────────────────────────────────────────
        item {
            VitalCard(
                id          = "cpu",
                accentColor = ColorCpu,
                icon        = "⚡",
                title       = stringResource(R.string.card_cpu),
                summary     = "%.1f%%".format(uiState.cpuPercent),
                progress    = uiState.cpuPercent / 100f,
                isExpanded  = "cpu" in expandedCards,
                onToggle    = { viewModel.toggleCard("cpu") }
            ) {
                CpuExpandedContent(
                    cores   = cpuCores,
                    thermals = thermals,
                    threads = uiState.threadCount
                )
            }
        }

        // ── RAM ──────────────────────────────────────────────────────────────
        item {
            VitalCard(
                id          = "ram",
                accentColor = ColorMemory,
                icon        = "🧠",
                title       = stringResource(R.string.card_ram),
                summary     = "%.1f%%  ·  %d / %d GB".format(
                    uiState.memory.usedPercent,
                    uiState.memory.usedKb / 1_048_576,
                    uiState.memory.totalKb / 1_048_576
                ),
                progress    = uiState.memory.usedPercent / 100f,
                isExpanded  = "ram" in expandedCards,
                onToggle    = { viewModel.toggleCard("ram") }
            ) {
                MemoryExpandedContent(
                    memExpanded  = memExpanded,
                    memBreakdown = memBreakdown,
                    onToggleMem  = viewModel::toggleMemoryExpanded
                )
            }
        }

        // ── Batería ───────────────────────────────────────────────────────────
        item {
            val bat = battery
            VitalCard(
                id          = "battery",
                accentColor = ColorBattery,
                icon        = if (bat?.isCharging == true) "🔋⚡" else "🔋",
                title       = stringResource(R.string.card_battery),
                summary     = if (bat != null) "${bat.levelPercent}%  ·  ${bat.status}" else "—",
                progress    = (bat?.levelPercent ?: 0) / 100f,
                isExpanded  = "battery" in expandedCards,
                onToggle    = { viewModel.toggleCard("battery") }
            ) {
                BatteryExpandedContent(bat)
            }
        }

        // ── Red ───────────────────────────────────────────────────────────────
        item {
            val net = network
            VitalCard(
                id          = "network",
                accentColor = ColorNetwork,
                icon        = "📡",
                title       = stringResource(R.string.card_network),
                summary     = if (net != null)
                    "↓ ${net.rxFormatted()}  ↑ ${net.txFormatted()}"
                else "—",
                progress    = null,
                isExpanded  = "network" in expandedCards,
                onToggle    = { viewModel.toggleCard("network") }
            ) {
                NetworkExpandedContent(net)
            }
        }

        // ── Almacenamiento ────────────────────────────────────────────────────
        item {
            val stor = storage
            VitalCard(
                id          = "storage",
                accentColor = ColorStorage,
                icon        = "💾",
                title       = stringResource(R.string.card_storage),
                summary     = if (stor != null)
                    "%.1f / %.1f GB".format(stor.usedGb(), stor.totalGb())
                else "—",
                progress    = stor?.usedPercent?.div(100f),
                isExpanded  = "storage" in expandedCards,
                onToggle    = { viewModel.toggleCard("storage") }
            ) {
                StorageExpandedContent(stor)
            }
        }

        // ── Top procesos ──────────────────────────────────────────────────────
        item {
            VitalCard(
                id          = "processes",
                accentColor = ColorProcesses,
                icon        = "📊",
                title       = stringResource(R.string.card_processes),
                summary     = if (processes.isNotEmpty())
                    stringResource(R.string.processes_count, processes.size)
                else "—",
                progress    = null,
                isExpanded  = "processes" in expandedCards,
                onToggle    = { viewModel.toggleCard("processes") }
            ) {
                ProcessesExpandedContent(processes)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// VitalCard — tarjeta reutilizable con expand/collapse animado
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VitalCard(
    id: String,
    accentColor: Color,
    icon: String,
    title: String,
    summary: String,
    progress: Float?,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val chevronRotation by animateFloatAsState(
        targetValue   = if (isExpanded) 180f else 0f,
        animationSpec = tween(250),
        label         = "chevron_$id"
    )

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.animateContentSize(tween(300))) {

            // ── Header ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(Modifier.width(10.dp))
                Text(text = icon, fontSize = 18.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text       = title,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.weight(1f)
                )
                Text(
                    text       = summary,
                    style      = MaterialTheme.typography.bodySmall,
                    color      = accentColor,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text       = "›",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Light,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier   = Modifier.rotate(chevronRotation - 90f)
                )
            }

            // Progress bar siempre visible (si aplica)
            if (progress != null) {
                LinearProgressIndicator(
                    progress  = { progress.coerceIn(0f, 1f) },
                    modifier  = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color      = accentColor,
                    trackColor = accentColor.copy(alpha = 0.15f),
                    strokeCap  = StrokeCap.Round
                )
                Spacer(Modifier.height(4.dp))
            }

            // ── Contenido expandible ─────────────────────────────────────────
            AnimatedVisibility(
                visible = isExpanded,
                enter   = fadeIn(tween(200)) + expandVertically(tween(300)),
                exit    = fadeOut(tween(150)) + shrinkVertically(tween(250))
            ) {
                Column {
                    HorizontalDivider(
                        color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 0.5.dp,
                        modifier  = Modifier.padding(horizontal = 16.dp)
                    )
                    Column(
                        modifier = Modifier.padding(
                            start = 16.dp, end = 16.dp,
                            top   = 12.dp, bottom = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Contenidos expandidos
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CpuExpandedContent(
    cores: List<CpuCoreInfo>,
    thermals: List<ThermalZone>,
    threads: Int
) {
    MetricRow(stringResource(R.string.active_threads), threads.toString())
    if (cores.isNotEmpty()) {
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.label_cores), style = LabelStyle)
        cores.forEach { core ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text  = stringResource(R.string.core_name, core.index),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (core.isOnline) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text  = if (core.isOnline) "${core.currentMhz} MHz" else stringResource(R.string.core_offline),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = if (core.isOnline) ColorCpu
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    if (thermals.isNotEmpty()) {
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.label_temperature), style = LabelStyle)
        thermals.forEach { zone ->
            MetricRow(zone.name, "%.1f °C".format(zone.temperatureCelsius))
        }
    }
}

@Composable
private fun MemoryExpandedContent(
    memExpanded: Boolean,
    memBreakdown: ProcessMemoryBreakdown?,
    onToggleMem: () -> Unit
) {
    Text(
        text     = if (memExpanded) stringResource(R.string.hide_breakdown)
                   else stringResource(R.string.show_breakdown),
        style    = MaterialTheme.typography.bodySmall,
        color    = ColorMemory,
        modifier = Modifier.clickable(onClick = onToggleMem)
    )
    AnimatedVisibility(visible = memExpanded) {
        if (memBreakdown == null) {
            Text(stringResource(R.string.loading), style = MaterialTheme.typography.bodySmall)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.vitals_pid, memBreakdown.pid), style = LabelStyle)
                MetricRow(stringResource(R.string.metric_total_pss),   "${memBreakdown.totalPssKb / 1024} MB")
                MetricRow(stringResource(R.string.metric_java_heap),   "${memBreakdown.javaHeapKb / 1024} MB")
                MetricRow(stringResource(R.string.metric_native_heap), "${memBreakdown.nativeHeapKb / 1024} MB")
                MetricRow(stringResource(R.string.metric_graphics),    "${memBreakdown.graphicsKb / 1024} MB")
                MetricRow(stringResource(R.string.metric_code),        "${memBreakdown.codeKb / 1024} MB")
                MetricRow(stringResource(R.string.metric_stack),       "${memBreakdown.stackKb / 1024} MB")
            }
        }
    }
}

@Composable
private fun BatteryExpandedContent(battery: BatteryInfo?) {
    if (battery == null) {
        Text(stringResource(R.string.no_data), style = MaterialTheme.typography.bodySmall)
        return
    }
    MetricRow(stringResource(R.string.battery_status),      battery.status)
    MetricRow(stringResource(R.string.battery_health),      battery.health)
    MetricRow(stringResource(R.string.battery_temperature), "%.1f °C".format(battery.temperatureCelsius))
    MetricRow(stringResource(R.string.battery_voltage),     "${battery.voltageMillivolts} mV")
    MetricRow(stringResource(R.string.battery_technology),  battery.technology)
}

@Composable
private fun NetworkExpandedContent(network: NetworkSpeed?) {
    if (network == null) {
        Text(stringResource(R.string.no_data), style = MaterialTheme.typography.bodySmall)
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.label_download), style = LabelStyle)
            Spacer(Modifier.height(4.dp))
            Text(
                text       = network.rxFormatted(),
                style      = MaterialTheme.typography.titleMedium,
                color      = ColorNetwork,
                fontWeight = FontWeight.Bold
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.label_upload), style = LabelStyle)
            Spacer(Modifier.height(4.dp))
            Text(
                text       = network.txFormatted(),
                style      = MaterialTheme.typography.titleMedium,
                color      = ColorNetwork.copy(alpha = 0.75f),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StorageExpandedContent(storage: StorageInfo?) {
    if (storage == null) {
        Text(stringResource(R.string.no_data), style = MaterialTheme.typography.bodySmall)
        return
    }
    MetricRow(stringResource(R.string.storage_total), "%.2f GB".format(storage.totalGb()))
    MetricRow(stringResource(R.string.storage_used),  "%.2f GB".format(storage.usedGb()))
    MetricRow(stringResource(R.string.storage_free),  "%.2f GB".format(storage.freeGb()))
    Spacer(Modifier.height(4.dp))
    LinearProgressIndicator(
        progress  = { (storage.usedPercent / 100f).coerceIn(0f, 1f) },
        modifier  = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
        color      = ColorStorage,
        trackColor = ColorStorage.copy(alpha = 0.15f),
        strokeCap  = StrokeCap.Round
    )
}

@Composable
private fun ProcessesExpandedContent(processes: List<TopProcess>) {
    if (processes.isEmpty()) {
        Text(
            stringResource(R.string.no_proc_access),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    Text(stringResource(R.string.label_top_by_ram), style = LabelStyle)
    Spacer(Modifier.height(4.dp))
    processes.forEach { proc ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text     = proc.name,
                style    = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text  = if (proc.ramKb > 1024) "${proc.ramKb / 1024} MB"
                        else "${proc.ramKb} KB",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = ColorProcesses
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Componente reutilizable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text       = value,
            style      = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            fontWeight = FontWeight.Medium
        )
    }
}
