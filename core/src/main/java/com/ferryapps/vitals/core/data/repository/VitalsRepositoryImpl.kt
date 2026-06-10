package com.ferryapps.vitals.core.data.repository

import com.ferryapps.vitals.core.data.db.VitalsDao
import com.ferryapps.vitals.core.data.db.VitalsEntity
import com.ferryapps.vitals.core.domain.model.VitalsSnapshot
import com.ferryapps.vitals.core.domain.repository.VitalsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class VitalsRepositoryImpl @Inject constructor(
    private val dao: VitalsDao
) : VitalsRepository {

    override suspend fun saveSnapshot(snapshot: VitalsSnapshot) {
        dao.insert(snapshot.toEntity())
    }

    override fun observeSnapshots(): Flow<List<VitalsSnapshot>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    private fun VitalsSnapshot.toEntity() = VitalsEntity(
        cpuUsagePercent = cpuUsagePercent,
        ramUsedMb = ramUsedMb,
        ramTotalMb = ramTotalMb,
        threadCount = threadCount,
        timestampMs = timestampMs
    )

    private fun VitalsEntity.toDomain() = VitalsSnapshot(
        cpuUsagePercent = cpuUsagePercent,
        ramUsedMb = ramUsedMb,
        ramTotalMb = ramTotalMb,
        threadCount = threadCount,
        timestampMs = timestampMs
    )
}
