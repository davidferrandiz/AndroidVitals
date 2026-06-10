package com.ferryapps.vitals.core.domain.usecase

import android.os.Process
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject

class GetCpuUsageUseCase @Inject constructor() {

    // /proc/stat is blocked by SELinux on Android 9+; probe once at first collection
    private val procStatAvailable: Boolean by lazy {
        runCatching { File("/proc/stat").readText() }.isSuccess
    }

    operator fun invoke(): Flow<Float> = flow {
        var prev = readSample()
        while (true) {
            delay(500)
            val curr = readSample()
            emit(toPercent(prev, curr))
            prev = curr
            delay(500)
        }
    }.flowOn(Dispatchers.IO)

    private fun readSample(): Sample = if (procStatAvailable) {
        // System-wide CPU via /proc/stat (Android 8 and some older devices)
        val v = File("/proc/stat").useLines { it.first() }
            .trim().split("\\s+".toRegex()).drop(1).map { it.toLong() }
        Sample.System(total = v.sum(), idle = v.getOrElse(3) { 0L } + v.getOrElse(4) { 0L })
    } else {
        // Process CPU fallback for Android 9+ (SELinux blocks /proc/stat)
        Sample.Process(cpuMs = Process.getElapsedCpuTime(), wallMs = SystemClock.elapsedRealtime())
    }

    private fun toPercent(prev: Sample, curr: Sample): Float = when {
        prev is Sample.System && curr is Sample.System -> {
            val dTotal = curr.total - prev.total
            val dIdle = curr.idle - prev.idle
            if (dTotal > 0) (dTotal - dIdle).toFloat() / dTotal * 100f else 0f
        }
        prev is Sample.Process && curr is Sample.Process -> {
            val dWall = curr.wallMs - prev.wallMs
            val dCpu = curr.cpuMs - prev.cpuMs
            if (dWall > 0) (dCpu.toFloat() / dWall * 100f).coerceIn(0f, 100f) else 0f
        }
        else -> 0f
    }

    private sealed class Sample {
        data class System(val total: Long, val idle: Long) : Sample()
        data class Process(val cpuMs: Long, val wallMs: Long) : Sample()
    }
}
