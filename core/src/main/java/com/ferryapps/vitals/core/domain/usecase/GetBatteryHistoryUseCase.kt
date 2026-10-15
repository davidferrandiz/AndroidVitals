package com.ferryapps.vitals.core.domain.usecase

import com.ferryapps.vitals.core.data.hardware.DesignCapacityReader
import com.ferryapps.vitals.core.domain.battery.BatteryHistoryMath
import com.ferryapps.vitals.core.domain.model.BatteryHistory
import com.ferryapps.vitals.core.domain.repository.BatteryHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Historial de los últimos 30 días ya resumido: gráfica de 24 h, estadísticas y salud. */
class GetBatteryHistoryUseCase @Inject constructor(
    private val repository: BatteryHistoryRepository,
    private val designCapacityReader: DesignCapacityReader,
) {
    operator fun invoke(nowMs: () -> Long = System::currentTimeMillis): Flow<BatteryHistory> {
        val since = nowMs() - RETENTION_MS
        return repository.observeSince(since)
            .map { samples ->
                val now = nowMs()
                val design = designCapacityReader.designCapacityMah
                BatteryHistory(
                    last24h = samples.filter { it.timestampMs >= now - BatteryHistoryMath.DAY_MS },
                    stats = BatteryHistoryMath.stats(samples, now),
                    health = BatteryHistoryMath.estimateHealth(samples, design),
                    designCapacityMah = design,
                )
            }
            .flowOn(Dispatchers.Default)
    }

    companion object {
        /** Lo que se guarda: suficiente para estimar la salud con varias cargas. */
        const val RETENTION_MS = 30 * BatteryHistoryMath.DAY_MS
    }
}
