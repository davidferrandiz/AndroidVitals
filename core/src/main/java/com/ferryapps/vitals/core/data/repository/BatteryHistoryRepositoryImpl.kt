package com.ferryapps.vitals.core.data.repository

import com.ferryapps.vitals.core.data.db.BatterySampleDao
import com.ferryapps.vitals.core.data.db.BatterySampleEntity
import com.ferryapps.vitals.core.domain.model.BatterySample
import com.ferryapps.vitals.core.domain.model.BatteryStatus
import com.ferryapps.vitals.core.domain.model.PlugType
import com.ferryapps.vitals.core.domain.repository.BatteryHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BatteryHistoryRepositoryImpl @Inject constructor(
    private val dao: BatterySampleDao
) : BatteryHistoryRepository {

    override suspend fun record(sample: BatterySample) = dao.insert(sample.toEntity())

    override fun observeSince(fromMs: Long): Flow<List<BatterySample>> =
        dao.observeSince(fromMs).map { rows -> rows.map { it.toDomain() } }

    override suspend fun deleteOlderThan(cutoffMs: Long) = dao.deleteOlderThan(cutoffMs)

    override suspend fun clear() = dao.clear()

    private fun BatterySample.toEntity() = BatterySampleEntity(
        timestampMs = timestampMs,
        levelPercent = levelPercent,
        status = status.name,
        plugType = plugType.name,
        temperatureCelsius = temperatureCelsius,
        voltageMillivolts = voltageMillivolts,
        chargeCounterMah = chargeCounterMah,
    )

    private fun BatterySampleEntity.toDomain() = BatterySample(
        timestampMs = timestampMs,
        levelPercent = levelPercent,
        status = BatteryStatus.entries.firstOrNull { it.name == status } ?: BatteryStatus.UNKNOWN,
        plugType = PlugType.entries.firstOrNull { it.name == plugType } ?: PlugType.NONE,
        temperatureCelsius = temperatureCelsius,
        voltageMillivolts = voltageMillivolts,
        chargeCounterMah = chargeCounterMah,
    )
}
