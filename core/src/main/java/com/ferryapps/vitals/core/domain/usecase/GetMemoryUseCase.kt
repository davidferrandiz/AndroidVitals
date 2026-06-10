package com.ferryapps.vitals.core.domain.usecase

import com.ferryapps.vitals.core.domain.model.MemoryInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject

class GetMemoryUseCase @Inject constructor() {

    operator fun invoke(): Flow<MemoryInfo> = flow {
        while (true) {
            emit(readMemInfo())
            delay(1_000)
        }
    }.flowOn(Dispatchers.IO)

    private fun readMemInfo(): MemoryInfo {
        val values = mutableMapOf<String, Long>()
        File("/proc/meminfo").useLines { lines ->
            for (line in lines) {
                val colon = line.indexOf(':')
                if (colon == -1) continue
                val key = line.substring(0, colon).trim()
                if (key != "MemTotal" && key != "MemAvailable") continue
                // Rest of the line is like "  8048964 kB"; grab the numeric token
                values[key] = line.substring(colon + 1).trim().split(' ')[0].toLongOrNull() ?: 0L
                if (values.size == 2) break
            }
        }
        val total = values["MemTotal"] ?: 0L
        val available = values["MemAvailable"] ?: 0L
        val used = (total - available).coerceAtLeast(0L)
        return MemoryInfo(
            totalKb = total,
            availableKb = available,
            usedKb = used,
            usedPercent = if (total > 0) used.toFloat() / total * 100f else 0f
        )
    }
}
