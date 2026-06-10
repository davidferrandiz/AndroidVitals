package com.ferryapps.vitals.feature.monitor.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ferryapps.vitals.core.domain.repository.VitalsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class VitalsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: VitalsRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // TODO: read CPU/RAM/thread stats, build VitalsSnapshot, call repository.saveSnapshot()
        return Result.success()
    }
}
