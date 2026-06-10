package com.ferryapps.vitals.core.domain.usecase

import android.os.Environment
import android.os.StatFs
import com.ferryapps.vitals.core.domain.model.StorageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class GetStorageUseCase @Inject constructor() {

    operator fun invoke(): Flow<StorageInfo> = flow {
        while (true) {
            emit(readStorage())
            delay(10_000) // El almacenamiento cambia muy lento — 10s
        }
    }.flowOn(Dispatchers.IO)

    private fun readStorage(): StorageInfo {
        val stat  = StatFs(Environment.getDataDirectory().path)
        val total = stat.totalBytes
        val free  = stat.availableBytes
        val used  = total - free
        return StorageInfo(
            totalBytes = total,
            usedBytes  = used,
            freeBytes  = free
        )
    }
}
