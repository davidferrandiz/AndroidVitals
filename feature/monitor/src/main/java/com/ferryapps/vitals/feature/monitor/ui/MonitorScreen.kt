package com.ferryapps.vitals.feature.monitor.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.hilt.navigation.compose.hiltViewModel
import com.ferryapps.vitals.core.domain.model.MemoryInfo
import com.ferryapps.vitals.core.domain.model.ProcessMemoryBreakdown
import com.ferryapps.vitals.core.domain.model.VitalsSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MonitorScreen(
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: MonitorViewModel = hiltViewModel()
) {
    val snapshots by viewModel.snapshots.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val memoryExpanded by viewModel.memoryExpanded.collectAsState()
    val memoryBreakdown by viewModel.memoryBreakdown.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = max(contentPadding.calculateTopPadding(), 16.dp),
            bottom = max(contentPadding.calculateBottomPadding(), 16.dp)
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { CpuLiveCard(uiState.cpuPercent) }
        item {
            MemoryLiveCard(
                memory = uiState.memory,
                expanded = memoryExpanded,
                breakdown = memoryBreakdown,
                onToggle = viewModel::toggleMemoryExpanded
            )
        }
        item { ThreadsLiveCard(uiState.threadCount) }
        items(snapshots, key = { it.timestampMs }) { snapshot ->
            SnapshotCard(snapshot)
        }
    }
}

@Composable
private fun CpuLiveCard(cpuUsage: Float) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("CPU en vivo", style = MaterialTheme.typography.labelSmall)
            Text(
                text = "%.1f%%".format(cpuUsage),
                style = MaterialTheme.typography.headlineMedium
            )
            LinearProgressIndicator(
                progress = { (cpuUsage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MemoryLiveCard(
    memory: MemoryInfo,
    expanded: Boolean,
    breakdown: ProcessMemoryBreakdown?,
    onToggle: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .clickable(onClick = onToggle)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RAM en vivo",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (expanded) "▴" else "▾",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = "%.1f%%  ·  %d / %d MB".format(
                    memory.usedPercent,
                    memory.usedKb / 1024,
                    memory.totalKb / 1024
                ),
                style = MaterialTheme.typography.headlineMedium
            )
            LinearProgressIndicator(
                progress = { (memory.usedPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
            AnimatedVisibility(visible = expanded) {
                ProcessMemoryBreakdownView(breakdown)
            }
        }
    }
}

@Composable
private fun ProcessMemoryBreakdownView(breakdown: ProcessMemoryBreakdown?) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (breakdown == null) {
            Text(
                text = "Cargando…",
                style = MaterialTheme.typography.bodySmall
            )
            return@Column
        }
        Text(
            text = "Memoria de Vitals (PID ${breakdown.pid})",
            style = MaterialTheme.typography.labelMedium
        )
        BreakdownRow("Total PSS", breakdown.totalPssKb)
        BreakdownRow("Java heap", breakdown.javaHeapKb)
        BreakdownRow("Native heap", breakdown.nativeHeapKb)
        BreakdownRow("Code", breakdown.codeKb)
        BreakdownRow("Stack", breakdown.stackKb)
        BreakdownRow("Graphics", breakdown.graphicsKb)
        BreakdownRow("Other", breakdown.otherKb)
    }
}

@Composable
private fun BreakdownRow(label: String, valueKb: Int) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "%d MB".format(valueKb / 1024),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ThreadsLiveCard(threadCount: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Threads en vivo", style = MaterialTheme.typography.labelSmall)
            Text(
                text = "$threadCount",
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

@Composable
private fun SnapshotCard(snapshot: VitalsSnapshot) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    .format(Date(snapshot.timestampMs)),
                style = MaterialTheme.typography.labelSmall
            )
            Text(text = "CPU  ${snapshot.cpuUsagePercent}%")
            Text(text = "RAM  ${snapshot.ramUsedMb} / ${snapshot.ramTotalMb} MB")
            Text(text = "Threads  ${snapshot.threadCount}")
        }
    }
}
