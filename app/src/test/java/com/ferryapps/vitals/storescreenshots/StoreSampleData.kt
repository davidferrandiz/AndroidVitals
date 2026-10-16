package com.ferryapps.vitals.storescreenshots

import com.ferryapps.vitals.core.domain.hardware.SocName
import com.ferryapps.vitals.core.domain.model.BatteryHealth
import com.ferryapps.vitals.core.domain.model.BatteryHealthEstimate
import com.ferryapps.vitals.core.domain.model.BatteryHistory
import com.ferryapps.vitals.core.domain.model.BatteryHistoryStats
import com.ferryapps.vitals.core.domain.model.BatteryInfo
import com.ferryapps.vitals.core.domain.model.BatterySample
import com.ferryapps.vitals.core.domain.model.BatteryStatus
import com.ferryapps.vitals.core.domain.model.CpuCluster
import com.ferryapps.vitals.core.domain.model.CpuCoreInfo
import com.ferryapps.vitals.core.domain.model.CpuSnapshot
import com.ferryapps.vitals.core.domain.model.DeviceInfo
import com.ferryapps.vitals.core.domain.model.DisplayInfo
import com.ferryapps.vitals.core.domain.model.HdrType
import com.ferryapps.vitals.core.domain.model.MemoryInfo
import com.ferryapps.vitals.core.domain.model.NetworkInfo
import com.ferryapps.vitals.core.domain.model.NetworkSpeed
import com.ferryapps.vitals.core.domain.model.NetworkTransport
import com.ferryapps.vitals.core.domain.model.PlugType
import com.ferryapps.vitals.core.domain.model.ProcessMemoryBreakdown
import com.ferryapps.vitals.core.domain.model.SensorKind
import com.ferryapps.vitals.core.domain.model.SensorReading
import com.ferryapps.vitals.core.domain.model.StorageInfo
import com.ferryapps.vitals.core.domain.model.ThermalKind
import com.ferryapps.vitals.core.domain.model.ThermalState
import com.ferryapps.vitals.core.domain.model.ThermalStatus
import com.ferryapps.vitals.core.domain.model.ThermalZone
import com.ferryapps.vitals.core.domain.model.TopProcess
import com.ferryapps.vitals.core.domain.model.WifiDetails
import com.ferryapps.vitals.core.domain.usecase.DeviceSummary
import kotlin.math.PI
import kotlin.math.sin

/**
 * Lecturas de ejemplo para las capturas de Play Store: un Galaxy A55 cargando a media tarde.
 * Tienen que ser coherentes entre sí (potencia = corriente × voltaje, RAM total = la de Hardware…)
 * porque la ficha las enseña juntas.
 */
object StoreSampleData {

    private const val GB = 1_000_000_000L
    private const val HOUR = 3_600_000L

    val soc = SocName(displayName = "Exynos 1480", code = "s5e8845", vendor = "Samsung")

    val deviceSummary = DeviceSummary(deviceName = "Galaxy A55 5G", androidVersion = "15", soc = soc)

    val battery = BatteryInfo(
        levelPercent = 78,
        isCharging = true,
        status = BatteryStatus.CHARGING,
        health = BatteryHealth.GOOD,
        temperatureCelsius = 31.4f,
        voltageMillivolts = 4_180,
        technology = "Li-ion",
        plugType = PlugType.AC,
        currentNowMa = 2_920,
        currentAverageMa = 2_870,
        chargeCounterMah = 3_900,
        chargeTimeRemainingMs = 34 * 60_000L,
        cycleCount = 214,
    )

    /** Potencia de los últimos minutos (W), estable con algo de ruido. */
    val powerHistory: List<Float> = List(150) { i -> 12.2f + 0.5f * sin(i / 6.0).toFloat() + 0.25f * sin(i / 1.7).toFloat() }

    private fun cores(): List<CpuCoreInfo> =
        List(4) { CpuCoreInfo(it, currentMhz = listOf(1_820, 1_536, 2_002, 1_248)[it], maxMhz = 2_002, isOnline = true, minMhz = 400, coreName = "Cortex-A55") } +
            List(4) { CpuCoreInfo(it + 4, currentMhz = listOf(2_400, 2_750, 1_950, 2_210)[it], maxMhz = 2_750, isOnline = true, minMhz = 600, coreName = "Cortex-A78") }

    val cpu = CpuSnapshot(
        cores = cores(),
        clusters = listOf(
            CpuCluster(coreName = "Cortex-A55", coreCount = 4, onlineCount = 4, currentMhz = 1_820, maxMhz = 2_002),
            CpuCluster(coreName = "Cortex-A78", coreCount = 4, onlineCount = 4, currentMhz = 2_400, maxMhz = 2_750),
        ),
        estimatedLoadPercent = 34f,
    )

    val cpuHistory: List<Float> = List(60) { i ->
        (30 + 9 * sin(i / 3.0) + 6 * sin(i / 1.3) + (if (i in 38..44) 18 else 0)).toFloat().coerceIn(4f, 96f)
    }

    val thermalZones = listOf(
        ThermalZone("cpu-big", 41.5f, ThermalKind.CPU),
        ThermalZone("gpu", 38.9f, ThermalKind.GPU),
        ThermalZone("battery", 31.4f, ThermalKind.BATTERY),
        ThermalZone("skin", 33.8f, ThermalKind.SKIN),
    )

    val thermalState = ThermalState(status = ThermalStatus.NONE, marginPercent = 64)

    val memory = MemoryInfo(
        totalKb = 7_650_000,
        availableKb = 3_090_000,
        usedKb = 4_560_000,
        usedPercent = 59.6f,
        thresholdKb = 226_000,
        swapTotalKb = 4_194_304,
        swapFreeKb = 2_910_000,
        cachedKb = 1_480_000,
    )

    val storage = StorageInfo(totalBytes = 128 * GB, usedBytes = 86 * GB, freeBytes = 42 * GB)

    val speed = NetworkSpeed(rxBytesPerSec = 2_340_000, txBytesPerSec = 184_000)

    val networkHistory: Pair<List<Float>, List<Float>> =
        List(40) { i -> (1.4 + 0.9 * sin(i / 2.5) + (if (i > 30) 0.8 else 0.0)).toFloat().coerceAtLeast(0.1f) } to
            List(40) { i -> (0.15 + 0.08 * sin(i / 1.7)).toFloat() }

    val network = NetworkInfo(
        transport = NetworkTransport.WIFI,
        wifi = WifiDetails(frequencyMhz = 5_180, generation = 6, rssiDbm = -54, linkSpeedMbps = 866),
    )

    val appMemory = ProcessMemoryBreakdown(
        pid = 12_345, totalPssKb = 61_200, javaHeapKb = 14_800, nativeHeapKb = 18_300,
        codeKb = 15_100, stackKb = 1_200, graphicsKb = 7_900, otherKb = 3_900,
    )

    val appProcesses = listOf(
        TopProcess(12_345, "com.ferryapps.vitals", 61_200),
        TopProcess(12_402, "com.ferryapps.vitals:widget", 18_400),
    )

    /** 24 h: carga por la noche, descarga durante el día y vuelve a cargar ahora. */
    fun history(nowMs: Long): BatteryHistory {
        val samples = (0..96).map { step ->
            val t = nowMs - 24 * HOUR + step * 15 * 60_000L
            val h = step / 4f // horas desde el inicio
            val (level, status, plug) = when {
                h < 1.5f -> Triple((44 + h / 1.5f * 56).toInt(), BatteryStatus.CHARGING, PlugType.AC)
                h < 9f -> Triple(100, BatteryStatus.FULL, PlugType.AC)
                h < 22.8f -> Triple(dischargeLevel(h - 9f), BatteryStatus.DISCHARGING, PlugType.NONE)
                else -> Triple((23 + (h - 22.8f) / 1.2f * 55).toInt(), BatteryStatus.CHARGING, PlugType.AC)
            }
            val temp = 28f + 3f * sin(h / 24.0 * 2 * PI).toFloat() + if (status == BatteryStatus.CHARGING) 2.5f else 0f
            BatterySample(t, level.coerceIn(0, 100), status, plug, temp, 4_100, 3_900 * level / 100)
        }
        return BatteryHistory(
            last24h = samples,
            stats = BatteryHistoryStats(drainPercentPerHour = 5.6f, chargeSessions7d = 9, maxTemperature24hCelsius = 36.8f),
            health = BatteryHealthEstimate(capacityMah = 4_710, validSessions = 6, candidateSessions = 7),
            designCapacityMah = 5_000,
        )
    }

    /** Descarga de un día normal: lenta por la mañana, más rápida con uso intenso, de 100 % a 23 %. */
    private fun dischargeLevel(hours: Float): Int {
        // Ritmo (%/h) por tramos de una hora durante 13,8 h.
        val rates = floatArrayOf(2.5f, 3f, 6f, 8f, 4f, 3.5f, 9f, 11f, 5f, 4f, 6.5f, 7f, 3f, 4f)
        var level = 100f
        var t = 0f
        for (rate in rates) {
            val step = minOf(1f, hours - t)
            if (step <= 0f) break
            level -= rate * step
            t += 1f
        }
        return level.toInt().coerceAtLeast(23)
    }

    val device = DeviceInfo(
        deviceName = "Galaxy A55 5G",
        manufacturer = "Samsung",
        model = "SM-A556B",
        androidVersion = "15",
        sdkInt = 35,
        securityPatch = "2026-09-01",
        kernelVersion = "5.15.153-android13-8",
        primaryAbi = "arm64-v8a",
        soc = soc,
        cpuCoreCount = 8,
        gpuRenderer = "Samsung Xclipse 530",
        vulkanVersion = "1.3.279",
        display = DisplayInfo(
            widthPx = 1_080, heightPx = 2_340, densityDpi = 450, diagonalInches = 6.6f,
            currentRefreshHz = 120f, maxRefreshHz = 120f, hdrTypes = listOf(HdrType.HDR10, HdrType.HDR10_PLUS),
        ),
        totalRamBytes = 8 * 1_073_741_824L,
        totalStorageBytes = 128 * GB,
        widevineLevel = "L1",
        sensorCount = 32,
    )

    val sensors = listOf(
        SensorReading(SensorKind.ACCELEROMETER, "LSM6DSO Accelerometer", listOf(0.12f, 9.78f, 0.31f), 78.4f),
        SensorReading(SensorKind.GYROSCOPE, "LSM6DSO Gyroscope", listOf(0.01f, -0.02f, 0.0f), 34.9f),
        SensorReading(SensorKind.MAGNETOMETER, "AK09918 Magnetometer", listOf(-12.4f, 30.1f, -41.7f), 4_912f),
        SensorReading(SensorKind.LIGHT, "TCS3701 Light", listOf(312f), 60_000f),
        SensorReading(SensorKind.PROXIMITY, "TCS3701 Proximity", listOf(5f), 5f),
    )

    const val UPTIME_MS = 3 * 24 * HOUR + 5 * HOUR + 12 * 60_000L
}
