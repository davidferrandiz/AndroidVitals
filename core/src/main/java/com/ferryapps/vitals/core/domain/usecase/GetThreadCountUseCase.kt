package com.ferryapps.vitals.core.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject

class GetThreadCountUseCase @Inject constructor() {

    operator fun invoke(): Flow<Int> = flow {
        while (true) {
            emit(readThreadCount())
            delay(1_000)
        }
    }.flowOn(Dispatchers.IO)

    private fun readThreadCount(): Int {
        File("/proc/self/status").useLines { lines ->
            for (line in lines) {
                if (!line.startsWith("Threads:")) continue
                return line.substringAfter(':').trim().toIntOrNull() ?: 0
            }
        }
        return 0
    }
}
