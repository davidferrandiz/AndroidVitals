package com.ferryapps.vitals.feature.monitor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.hilt.navigation.compose.hiltViewModel
import com.ferryapps.vitals.core.domain.model.MemoryInfo
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
        item { MemoryLiveCard(uiState.memory) }
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
private fun MemoryLiveCard(memory: MemoryInfo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("RAM en vivo", style = MaterialTheme.typography.labelSmall)
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
        }
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
