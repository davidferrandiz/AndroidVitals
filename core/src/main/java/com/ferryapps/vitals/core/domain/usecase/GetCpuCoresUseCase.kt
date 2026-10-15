package com.ferryapps.vitals.core.domain.usecase

import com.ferryapps.vitals.core.data.sysfs.SysFs
import com.ferryapps.vitals.core.domain.cpu.CpuMath
import com.ferryapps.vitals.core.domain.hardware.HardwareNames
import com.ferryapps.vitals.core.domain.model.CpuCoreInfo
import com.ferryapps.vitals.core.domain.model.CpuSnapshot
import com.ferryapps.vitals.core.domain.model.ThermalKind
import com.ferryapps.vitals.core.domain.model.ThermalZone
import com.ferryapps.vitals.core.domain.parser.ProcParsers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class GetCpuCoresUseCase @Inject constructor() {

    /** Nombre de microarquitectura por núcleo, leído una vez de /proc/cpuinfo. */
    private val coreNames: Map<Int, String> by lazy {
        val text = SysFs.read("/proc/cpuinfo") ?: return@lazy emptyMap()
        ProcParsers.parseCpuParts(text).mapNotNull { (cpu, ids) ->
            HardwareNames.cpuCoreName(ids.first, ids.second)?.let { cpu to it }
        }.toMap()
    }

    /** Último min/max conocido por núcleo: al apagarse un núcleo su cpufreq puede desaparecer. */
    private val knownLimits = ConcurrentHashMap<Int, Pair<Int, Int>>()

    operator fun invoke(): Flow<List<CpuCoreInfo>> = flow {
        while (true) {
            emit(readCores())
            delay(1_000)
        }
    }.flowOn(Dispatchers.IO)

    fun snapshot(): Flow<CpuSnapshot> = invoke().map { cores ->
        CpuSnapshot(
            cores = cores,
            clusters = CpuMath.clusters(cores.filter { it.maxMhz > 0 }),
            estimatedLoadPercent = CpuMath.estimatedLoadPercent(cores),
        )
    }

    fun thermalZones(): Flow<List<ThermalZone>> = flow {
        while (true) {
            emit(readThermalZones())
            delay(2_000)
        }
    }.flowOn(Dispatchers.IO)

    private fun readCores(): List<CpuCoreInfo> {
        val cores = mutableListOf<CpuCoreInfo>()
        var index = 0
        while (SysFs.exists("/sys/devices/system/cpu/cpu$index")) {
            val base = "/sys/devices/system/cpu/cpu$index"
            // cpu0 no tiene fichero 'online': siempre está encendido.
            val isOnline = SysFs.read("$base/online")?.trim()?.let { it == "1" } ?: true
            val current = ProcParsers.parseFreqMhz(SysFs.read("$base/cpufreq/scaling_cur_freq"))
            var max = ProcParsers.parseFreqMhz(SysFs.read("$base/cpufreq/cpuinfo_max_freq"))
            var min = ProcParsers.parseFreqMhz(SysFs.read("$base/cpufreq/cpuinfo_min_freq"))
            if (max > 0) {
                knownLimits[index] = min to max
            } else {
                knownLimits[index]?.let { (knownMin, knownMax) ->
                    min = knownMin
                    max = knownMax
                }
            }
            cores.add(
                CpuCoreInfo(
                    index = index,
                    currentMhz = if (isOnline) current else 0,
                    maxMhz = max,
                    isOnline = isOnline,
                    minMhz = min,
                    coreName = coreNames[index],
                )
            )
            index++
        }
        return cores
    }

    private fun readThermalZones(): List<ThermalZone> {
        val zones = mutableListOf<ThermalZone>()
        var i = 0
        while (SysFs.exists("/sys/class/thermal/thermal_zone$i")) {
            val dir = "/sys/class/thermal/thermal_zone$i"
            val type = SysFs.read("$dir/type")?.trim()
            val celsius = ProcParsers.parseThermalCelsius(SysFs.read("$dir/temp"))
            if (!type.isNullOrEmpty() && celsius != null) {
                val kind = CpuMath.classifyThermalZone(type)
                if (kind != ThermalKind.OTHER) zones.add(ThermalZone(type, celsius, kind))
            }
            i++
        }
        return zones
            .distinctBy { it.name }
            .sortedBy { it.kind.ordinal }
            .take(MAX_ZONES)
    }

    private companion object {
        const val MAX_ZONES = 8
    }
}
