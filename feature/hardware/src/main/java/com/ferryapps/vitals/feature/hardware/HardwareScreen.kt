package com.ferryapps.vitals.feature.hardware

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ferryapps.vitals.core.domain.model.AppSettings
import com.ferryapps.vitals.core.domain.model.CpuSnapshot
import com.ferryapps.vitals.core.domain.model.DeviceInfo
import com.ferryapps.vitals.core.domain.model.HdrType
import com.ferryapps.vitals.core.domain.model.MemoryInfo
import com.ferryapps.vitals.core.domain.model.SensorKind
import com.ferryapps.vitals.core.domain.model.SensorReading
import com.ferryapps.vitals.core.domain.model.ThermalState
import com.ferryapps.vitals.core.domain.model.ThermalStatus
import com.ferryapps.vitals.designsystem.R as DsR
import com.ferryapps.vitals.designsystem.component.CardPadding
import com.ferryapps.vitals.designsystem.component.ChipRow
import com.ferryapps.vitals.designsystem.component.InfoChip
import com.ferryapps.vitals.designsystem.component.LinearMeter
import com.ferryapps.vitals.designsystem.component.MetricRow
import com.ferryapps.vitals.designsystem.component.SectionCard
import com.ferryapps.vitals.designsystem.component.StatusKind
import com.ferryapps.vitals.designsystem.component.StatusPill
import com.ferryapps.vitals.designsystem.component.UnavailableRow
import com.ferryapps.vitals.designsystem.component.VitalCard
import com.ferryapps.vitals.designsystem.component.color
import com.ferryapps.vitals.designsystem.format.Formatters
import com.ferryapps.vitals.designsystem.theme.VitalsTheme
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HardwareScreen(
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: HardwareViewModel = hiltViewModel(),
) {
    val device by viewModel.device.collectAsStateWithLifecycle()
    val cpu by viewModel.cpu.collectAsStateWithLifecycle()
    val thermalState by viewModel.thermalState.collectAsStateWithLifecycle()
    val memory by viewModel.memory.collectAsStateWithLifecycle()
    val sensors by viewModel.sensors.collectAsStateWithLifecycle()
    val uptime by viewModel.uptimeMs.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = max(contentPadding.calculateTopPadding(), 8.dp) + 4.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "title") {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
                Text(
                    stringResource(R.string.hw_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() }
                )
                val info = device
                if (info != null) {
                    val shareTitle = stringResource(R.string.hw_share)
                    IconButton(onClick = { shareReport(context, info, cpu, memory, shareTitle) }) {
                        Icon(painterResource(DsR.drawable.ic_share), contentDescription = shareTitle)
                    }
                }
            }
        }
        val info = device ?: return@LazyColumn
        item(key = "device") { DeviceCard(info, uptime) }
        item(key = "thermal") { ThermalCard(thermalState) }
        item(key = "cpu") { ProcessorCard(info, cpu) }
        info.display?.let { display ->
            item(key = "display") {
                SectionCard(title = stringResource(R.string.hw_display)) {
                    val size = display.diagonalInches?.let { "${Formatters.decimal(it, 1)}\" · " }.orEmpty()
                    MetricRow(stringResource(R.string.hw_display_size), "$size${display.widthPx} × ${display.heightPx}", divider = true)
                    MetricRow(
                        stringResource(R.string.hw_display_refresh),
                        if (display.maxRefreshHz - display.currentRefreshHz > 1f) {
                            stringResource(
                                R.string.hw_display_refresh_value,
                                Formatters.decimal(display.currentRefreshHz, 0),
                                Formatters.decimal(display.maxRefreshHz, 0)
                            )
                        } else "${Formatters.decimal(display.currentRefreshHz, 0)} Hz",
                        divider = true
                    )
                    MetricRow(stringResource(R.string.hw_display_density), stringResource(R.string.hw_dpi, display.densityDpi), divider = true)
                    MetricRow(
                        stringResource(R.string.hw_display_hdr),
                        if (display.hdrTypes.isEmpty()) stringResource(R.string.hw_not_supported)
                        else display.hdrTypes.joinToString(", ") { it.label() }
                    )
                }
            }
        }
        item(key = "memory") { MemoryCard(info, memory) }
        item(key = "sensors") { SensorsCard(info, sensors) }
        item(key = "system") {
            SectionCard(title = stringResource(R.string.hw_system)) {
                MetricRow(stringResource(R.string.hw_android), "${info.androidVersion} (API ${info.sdkInt})", divider = true)
                MetricRow(stringResource(R.string.hw_kernel), info.kernelVersion ?: "—", divider = true)
                MetricRow(stringResource(R.string.hw_abi), info.primaryAbi, divider = true)
                MetricRow(stringResource(R.string.hw_model_code), info.model)
            }
        }
    }
}

@Composable
private fun DeviceCard(info: DeviceInfo, uptimeMs: Long) {
    val context = LocalContext.current
    VitalCard(contentPadding = CardPadding.Roomy) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(info.deviceName, style = MaterialTheme.typography.titleLarge)
            val patch = info.securityPatch?.let(::formatPatchDate)
            Text(
                if (patch != null) stringResource(R.string.hw_android_patch, info.androidVersion, patch)
                else stringResource(R.string.hw_android_only, info.androidVersion),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        ChipRow {
            val widevine = info.widevineLevel
            when (widevine) {
                "L1" -> InfoChip(stringResource(R.string.hw_widevine_l1), accent = VitalsTheme.palette.good)
                null -> Unit
                else -> InfoChip(stringResource(R.string.hw_widevine_other, widevine), accent = VitalsTheme.palette.warning)
            }
            InfoChip(info.primaryAbi)
            InfoChip(stringResource(R.string.hw_uptime, Formatters.uptime(context, uptimeMs)))
        }
    }
}

@Composable
private fun ThermalCard(state: ThermalState?) {
    val title = stringResource(R.string.hw_thermal)
    if (state == null) return
    if (state.status == ThermalStatus.UNAVAILABLE) {
        SectionCard(title = title) {
            UnavailableRow(
                title = stringResource(R.string.hw_thermal_status),
                reason = stringResource(R.string.hw_thermal_unavailable_reason)
            )
        }
        return
    }
    val kind = when (state.status) {
        ThermalStatus.NONE, ThermalStatus.LIGHT -> StatusKind.GOOD
        ThermalStatus.MODERATE -> StatusKind.WARNING
        else -> StatusKind.CRITICAL
    }
    val label = when (kind) {
        StatusKind.GOOD -> R.string.hw_thermal_ok
        StatusKind.WARNING -> R.string.hw_thermal_moderate
        else -> R.string.hw_thermal_severe
    }
    SectionCard(title = title, trailing = { StatusPill(stringResource(label), kind) }) {
        val margin = state.marginPercent
        if (margin != null) {
            MetricRow(stringResource(R.string.hw_thermal_margin), "$margin %")
            LinearMeter(progress = margin / 100f, color = kind.color(), height = 8.dp)
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text(stringResource(R.string.hw_thermal_cool), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.hw_thermal_limit), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            stringResource(if (kind == StatusKind.GOOD) R.string.hw_thermal_ok_desc else R.string.hw_thermal_throttling_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun ProcessorCard(info: DeviceInfo, cpu: CpuSnapshot?) {
    SectionCard(title = stringResource(R.string.hw_processor)) {
        MetricRow(stringResource(R.string.hw_soc), info.soc.displayName, divider = info.soc.code == null)
        info.soc.code?.let { code ->
            Text(
                stringResource(R.string.hw_soc_code, code),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        val clusters = cpu?.clusters.orEmpty()
        val split = clusters.joinToString(" + ") { it.coreCount.toString() }
        MetricRow(
            stringResource(R.string.hw_cores),
            if (clusters.size > 1) "${info.cpuCoreCount} ($split)" else info.cpuCoreCount.toString(),
            divider = true
        )
        clusters.forEach { cluster ->
            MetricRow(
                label = "${cluster.coreCount}× ${cluster.coreName ?: stringResource(R.string.hw_core_generic)}",
                value = Formatters.frequency(cluster.maxMhz),
                divider = true
            )
        }
        MetricRow(stringResource(R.string.hw_gpu), info.gpuRenderer ?: "—", divider = true)
        MetricRow(stringResource(R.string.hw_vulkan), info.vulkanVersion ?: stringResource(R.string.hw_not_supported))
    }
}

@Composable
private fun MemoryCard(info: DeviceInfo, memory: MemoryInfo?) {
    val context = LocalContext.current
    SectionCard(title = stringResource(R.string.hw_memory)) {
        MetricRow(stringResource(R.string.hw_ram), Formatters.bytes(context, info.totalRamBytes), divider = true)
        val swap = memory?.swapTotalKb ?: 0L
        if (swap > 0) {
            MetricRow(stringResource(R.string.hw_zram), Formatters.bytes(context, swap * 1024), divider = true)
        }
        MetricRow(stringResource(R.string.hw_storage), Formatters.bytes(context, info.totalStorageBytes))
    }
}

@Composable
private fun SensorsCard(info: DeviceInfo, sensors: List<SensorReading>) {
    SectionCard(
        title = stringResource(R.string.hw_sensors),
        trailing = {
            Text(
                stringResource(R.string.hw_sensors_count, info.sensorCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    ) {
        sensors.forEachIndexed { index, reading ->
            MetricRow(
                label = stringResource(reading.kind.labelRes()),
                value = reading.formatted(),
                divider = index < sensors.lastIndex
            )
        }
    }
}

private fun SensorKind.labelRes(): Int = when (this) {
    SensorKind.ACCELEROMETER -> R.string.hw_sensor_accelerometer
    SensorKind.GYROSCOPE -> R.string.hw_sensor_gyroscope
    SensorKind.MAGNETOMETER -> R.string.hw_sensor_magnetometer
    SensorKind.BAROMETER -> R.string.hw_sensor_barometer
    SensorKind.LIGHT -> R.string.hw_sensor_light
    SensorKind.PROXIMITY -> R.string.hw_sensor_proximity
}

@Composable
private fun SensorReading.formatted(): String {
    if (values.isEmpty()) return "…"
    fun xyz(decimals: Int, unit: String) =
        values.take(3).joinToString(" · ") { Formatters.decimal(it, decimals) } + " $unit"
    return when (kind) {
        SensorKind.ACCELEROMETER -> xyz(2, "m/s²")
        SensorKind.GYROSCOPE -> xyz(2, "rad/s")
        SensorKind.MAGNETOMETER -> xyz(0, "µT")
        SensorKind.BAROMETER -> "${Formatters.integer(values[0].toInt())} hPa"
        SensorKind.LIGHT -> "${Formatters.integer(values[0].toInt())} lx"
        SensorKind.PROXIMITY -> stringResource(
            if (maxRange > 0f && values[0] >= maxRange / 2f) R.string.hw_proximity_far else R.string.hw_proximity_near
        )
    }
}

private fun HdrType.label(): String = when (this) {
    HdrType.DOLBY_VISION -> "Dolby Vision"
    HdrType.HDR10 -> "HDR10"
    HdrType.HDR10_PLUS -> "HDR10+"
    HdrType.HLG -> "HLG"
}

/** "2026-09-05" → fecha en el formato del idioma del usuario. */
private fun formatPatchDate(raw: String): String = try {
    val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse(raw)
    if (parsed != null) DateFormat.getDateInstance(DateFormat.MEDIUM).format(parsed) else raw
} catch (e: java.text.ParseException) {
    raw
}

/** Informe en texto plano para compartir (soporte técnico, compraventa de segunda mano…). */
private fun shareReport(context: Context, info: DeviceInfo, cpu: CpuSnapshot?, memory: MemoryInfo?, title: String) {
    val clusters = cpu?.clusters.orEmpty().joinToString(", ") { "${it.coreCount}× ${it.coreName ?: "core"} ${Formatters.frequency(it.maxMhz)}" }
    val text = buildString {
        appendLine("${info.deviceName} (${info.model})")
        appendLine("Android ${info.androidVersion} · API ${info.sdkInt}" + (info.securityPatch?.let { " · patch $it" } ?: ""))
        appendLine("SoC: ${info.soc.displayName}" + (info.soc.code?.let { " ($it)" } ?: ""))
        if (clusters.isNotEmpty()) appendLine("CPU: $clusters")
        info.gpuRenderer?.let { appendLine("GPU: $it" + (info.vulkanVersion?.let { v -> " · Vulkan $v" } ?: "")) }
        appendLine("RAM: ${Formatters.bytes(context, info.totalRamBytes)}" + (memory?.swapTotalKb?.takeIf { it > 0 }?.let { " · zRAM ${Formatters.bytes(context, it * 1024)}" } ?: ""))
        appendLine("Storage: ${Formatters.bytes(context, info.totalStorageBytes)}")
        info.display?.let { appendLine("Display: ${it.widthPx}×${it.heightPx} · ${Formatters.decimal(it.maxRefreshHz, 0)} Hz") }
        info.widevineLevel?.let { appendLine("Widevine: $it") }
        append("— Vitals")
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, title))
}
