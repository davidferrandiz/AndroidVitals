package com.ferryapps.vitals.core.domain.usecase

import android.os.Process
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * CPU que consume **Vitals** (no el sistema), en % de la capacidad total de la CPU.
 * Android bloquea /proc/stat a las apps desde Android 8, así que el uso global no se puede
 * leer; para el sistema se usa la carga estimada de [GetCpuCoresUseCase.snapshot].
 */
class GetAppCpuUsageUseCase @Inject constructor() {

    operator fun invoke(): Flow<Float> = flow {
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        var prevCpu = Process.getElapsedCpuTime()
        var prevWall = SystemClock.elapsedRealtime()
        while (true) {
            delay(1_000)
            val cpu = Process.getElapsedCpuTime()
            val wall = SystemClock.elapsedRealtime()
            val dWall = wall - prevWall
            val percent = if (dWall > 0) (cpu - prevCpu).toFloat() / (dWall * cores) * 100f else 0f
            emit(percent.coerceIn(0f, 100f))
            prevCpu = cpu
            prevWall = wall
        }
    }.flowOn(Dispatchers.IO)
}
