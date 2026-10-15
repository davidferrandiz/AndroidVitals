package com.ferryapps.vitals.core.domain.usecase

import android.app.ActivityManager
import android.content.Context
import com.ferryapps.vitals.core.data.sysfs.SysFs
import com.ferryapps.vitals.core.domain.model.MemoryInfo
import com.ferryapps.vitals.core.domain.parser.ProcParsers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class GetMemoryUseCase @Inject constructor(
    @ApplicationContext context: Context
) {
    private val activityManager: ActivityManager? = context.getSystemService(ActivityManager::class.java)

    operator fun invoke(): Flow<MemoryInfo> = flow {
        while (true) {
            emit(current())
            delay(1_000)
        }
    }.flowOn(Dispatchers.IO)

    /** Una lectura suelta (widget). */
    fun current(): MemoryInfo {
        val values = SysFs.read("/proc/meminfo")?.let(ProcParsers::parseMemInfo).orEmpty()
        val system = ActivityManager.MemoryInfo().also { activityManager?.getMemoryInfo(it) }

        val total = values["MemTotal"] ?: (system.totalMem / 1024)
        val available = values["MemAvailable"] ?: (system.availMem / 1024)
        val used = (total - available).coerceAtLeast(0L)
        return MemoryInfo(
            totalKb = total,
            availableKb = available,
            usedKb = used,
            usedPercent = if (total > 0) used.toFloat() / total * 100f else 0f,
            lowMemory = system.lowMemory,
            thresholdKb = system.threshold / 1024,
            swapTotalKb = values["SwapTotal"] ?: 0L,
            swapFreeKb = values["SwapFree"] ?: 0L,
            cachedKb = values["Cached"] ?: 0L,
        )
    }
}
