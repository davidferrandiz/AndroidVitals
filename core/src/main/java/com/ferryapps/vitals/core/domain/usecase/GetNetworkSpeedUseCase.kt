package com.ferryapps.vitals.core.domain.usecase

import android.net.TrafficStats
import com.ferryapps.vitals.core.domain.model.NetworkSpeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class GetNetworkSpeedUseCase @Inject constructor() {

    operator fun invoke(): Flow<NetworkSpeed> = flow {
        var prevRx = TrafficStats.getTotalRxBytes()
        var prevTx = TrafficStats.getTotalTxBytes()
        var prevMs = System.currentTimeMillis()

        while (true) {
            delay(1_000)
            val currRx = TrafficStats.getTotalRxBytes()
            val currTx = TrafficStats.getTotalTxBytes()
            val currMs = System.currentTimeMillis()

            val elapsedSec = (currMs - prevMs) / 1000.0
            if (elapsedSec > 0 && currRx != TrafficStats.UNSUPPORTED.toLong()) {
                emit(
                    NetworkSpeed(
                        rxBytesPerSec = ((currRx - prevRx) / elapsedSec).toLong().coerceAtLeast(0),
                        txBytesPerSec = ((currTx - prevTx) / elapsedSec).toLong().coerceAtLeast(0)
                    )
                )
            }
            prevRx = currRx
            prevTx = currTx
            prevMs = currMs
        }
    }.flowOn(Dispatchers.IO)
}
