package com.ferryapps.vitals.core.domain.usecase

import android.os.Debug
import android.os.Process
import com.ferryapps.vitals.core.domain.model.ProcessMemoryBreakdown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class GetProcessMemoryBreakdownUseCase @Inject constructor() {

    operator fun invoke(): Flow<ProcessMemoryBreakdown> = flow {
        while (true) {
            emit(readBreakdown())
            delay(5_000)
        }
    }.flowOn(Dispatchers.IO)

    private fun readBreakdown(): ProcessMemoryBreakdown {
        val info = Debug.MemoryInfo()
        Debug.getMemoryInfo(info)
        val java = info.getMemoryStat(STAT_JAVA_HEAP)?.toIntOrNull() ?: 0
        val native = info.getMemoryStat(STAT_NATIVE_HEAP)?.toIntOrNull() ?: 0
        val code = info.getMemoryStat(STAT_CODE)?.toIntOrNull() ?: 0
        val stack = info.getMemoryStat(STAT_STACK)?.toIntOrNull() ?: 0
        val graphics = info.getMemoryStat(STAT_GRAPHICS)?.toIntOrNull() ?: 0
        val total = info.totalPss
        val accounted = java + native + code + stack + graphics
        return ProcessMemoryBreakdown(
            pid = Process.myPid(),
            totalPssKb = total,
            javaHeapKb = java,
            nativeHeapKb = native,
            codeKb = code,
            stackKb = stack,
            graphicsKb = graphics,
            otherKb = (total - accounted).coerceAtLeast(0)
        )
    }

    private companion object {
        const val STAT_JAVA_HEAP = "summary.java-heap"
        const val STAT_NATIVE_HEAP = "summary.native-heap"
        const val STAT_CODE = "summary.code"
        const val STAT_STACK = "summary.stack"
        const val STAT_GRAPHICS = "summary.graphics"
    }
}
