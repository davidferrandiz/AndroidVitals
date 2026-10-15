package com.ferryapps.vitals.core.domain.usecase

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import com.ferryapps.vitals.core.domain.model.SensorKind
import com.ferryapps.vitals.core.domain.model.SensorReading
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/**
 * Lecturas en vivo de los sensores principales. Los listeners solo existen mientras alguien
 * colecciona el Flow (pantalla Hardware visible), así que no gasta batería en segundo plano.
 */
class GetSensorReadingsUseCase @Inject constructor(
    @ApplicationContext context: Context
) {
    private val sensorManager: SensorManager? = context.getSystemService(SensorManager::class.java)

    operator fun invoke(): Flow<List<SensorReading>> = callbackFlow {
        val manager = sensorManager
        if (manager == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val available: List<Pair<SensorKind, Sensor>> = TYPES.mapNotNull { (kind, type) ->
            manager.getDefaultSensor(type)?.let { kind to it }
        }
        val latest = LinkedHashMap<SensorKind, SensorReading>()
        available.forEach { (kind, sensor) ->
            latest[kind] = SensorReading(kind, sensor.name, emptyList(), sensor.maximumRange)
        }
        trySend(latest.values.toList())

        var lastEmit = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val kind = available.firstOrNull { it.second == event.sensor }?.first ?: return
                latest[kind] = SensorReading(kind, event.sensor.name, event.values.toList(), event.sensor.maximumRange)
                val now = SystemClock.elapsedRealtime()
                if (now - lastEmit >= EMIT_INTERVAL_MS) {
                    lastEmit = now
                    trySend(latest.values.toList())
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }
        available.forEach { (_, sensor) ->
            manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        awaitClose { manager.unregisterListener(listener) }
    }

    private companion object {
        const val EMIT_INTERVAL_MS = 500L
        val TYPES = listOf(
            SensorKind.ACCELEROMETER to Sensor.TYPE_ACCELEROMETER,
            SensorKind.GYROSCOPE to Sensor.TYPE_GYROSCOPE,
            SensorKind.MAGNETOMETER to Sensor.TYPE_MAGNETIC_FIELD,
            SensorKind.BAROMETER to Sensor.TYPE_PRESSURE,
            SensorKind.LIGHT to Sensor.TYPE_LIGHT,
            SensorKind.PROXIMITY to Sensor.TYPE_PROXIMITY,
        )
    }
}
