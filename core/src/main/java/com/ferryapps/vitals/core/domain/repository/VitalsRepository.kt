package com.ferryapps.vitals.core.domain.repository

import com.ferryapps.vitals.core.domain.model.VitalsSnapshot
import kotlinx.coroutines.flow.Flow

interface VitalsRepository {
    suspend fun saveSnapshot(snapshot: VitalsSnapshot)
    fun observeSnapshots(): Flow<List<VitalsSnapshot>>
}
