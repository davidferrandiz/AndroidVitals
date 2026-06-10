package com.ferryapps.vitals.core.domain.usecase

import com.ferryapps.vitals.core.domain.model.CpuCoreInfo
import com.ferryapps.vitals.core.domain.model.ThermalZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject

class GetCpuCoresUseCase @Inject constructor() {

    operator fun invoke(): Flow<List<CpuCoreInfo>> = flow {
        while (true) {
            emit(readCores())
            delay(1_000)
        }
    }.flowOn(Dispatchers.IO)

    fun thermalZones(): Flow<List<ThermalZone>> = flow {
        while (true) {
            emit(readThermalZones())
            delay(2_000)
        }
    }.flowOn(Dispatchers.IO)

    private fun readCores(): List<CpuCoreInfo> {
        val cores = mutableListOf<CpuCoreInfo>()
        var index = 0
        while (true) {
            val baseDir = File("/sys/devices/system/cpu/cpu$index")
            if (!baseDir.exists()) break

            val onlineFile = File(baseDir, "online")
            val isOnline = !onlineFile.exists() || // cpu0 no tiene 'online', siempre activo
                           runCatching { onlineFile.readText().trim() == "1" }.getOrDefault(true)

            val curFreqFile = File(baseDir, "cpufreq/scaling_cur_freq")
            val maxFreqFile = File(baseDir, "cpufreq/cpuinfo_max_freq")

            val curKhz = runCatching { curFreqFile.readText().trim().toLong() }.getOrDefault(0L)
            val maxKhz = runCatching { maxFreqFile.readText().trim().toLong() }.getOrDefault(0L)

            cores.add(
                CpuCoreInfo(
                    index       = index,
                    currentMhz  = (curKhz / 1000).toInt(),
                    maxMhz      = (maxKhz / 1000).toInt(),
                    isOnline    = isOnline
                )
            )
            index++
        }
        return cores
    }

    private fun readThermalZones(): List<ThermalZone> {
        val zones = mutableListOf<ThermalZone>()
        var i = 0
        while (true) {
            val dir = File("/sys/class/thermal/thermal_zone$i")
            if (!dir.exists()) break

            val type = runCatching { File(dir, "type").readText().trim() }.getOrNull()
            val temp = runCatching { File(dir, "temp").readText().trim().toLong() }.getOrNull()

            if (type != null && temp != null && temp > 0) {
                // Los valores pueden estar en milicélsius o en décimas
                val celsius = when {
                    temp > 1000 -> temp / 1000f   // milicélsius
                    temp > 100  -> temp / 10f     // décimas de grado
                    else        -> temp.toFloat()  // ya en célsius
                }
                if (celsius in 0f..150f) { // filtrar lecturas absurdas
                    zones.add(ThermalZone(name = type, temperatureCelsius = celsius))
                }
            }
            i++
        }
        // Deduplicar por nombre, quedarnos con los más relevantes
        return zones
            .distinctBy { it.name }
            .filter { it.name.contains("cpu", ignoreCase = true) ||
                      it.name.contains("soc", ignoreCase = true) ||
                      it.name.contains("skin", ignoreCase = true) ||
                      it.name.contains("battery", ignoreCase = true) }
            .take(6)
    }
}
