package com.ferryapps.vitals.core.domain.usecase

import com.ferryapps.vitals.core.data.sysfs.SysFs
import com.ferryapps.vitals.core.domain.parser.ProcParsers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/** Hilos activos del proceso de Vitals. */
class GetThreadCountUseCase @Inject constructor() {

    operator fun invoke(): Flow<Int> = flow {
        while (true) {
            emit(SysFs.read("/proc/self/status")?.let(ProcParsers::parseThreads) ?: 0)
            delay(1_000)
        }
    }.flowOn(Dispatchers.IO)
}
