package com.ferryapps.vitals.core.domain.usecase

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.view.Display
import com.ferryapps.vitals.core.data.hardware.GpuInfoReader
import com.ferryapps.vitals.core.data.hardware.WidevineReader
import com.ferryapps.vitals.core.data.sysfs.SysFs
import com.ferryapps.vitals.core.domain.hardware.HardwareNames
import com.ferryapps.vitals.core.domain.model.DeviceInfo
import com.ferryapps.vitals.core.domain.model.DisplayInfo
import com.ferryapps.vitals.core.domain.model.HdrType
import com.ferryapps.vitals.core.domain.parser.ProcParsers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import kotlin.math.sqrt

/** Datos fijos del dispositivo para la pantalla Hardware. Se leen una vez, fuera de Main. */
class GetDeviceInfoUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gpuInfoReader: GpuInfoReader,
    private val widevineReader: WidevineReader,
) {

    suspend operator fun invoke(): DeviceInfo = withContext(Dispatchers.IO) {
        val cpuInfo = SysFs.read("/proc/cpuinfo").orEmpty()
        val cpuInfoHardware = ProcParsers.parseCpuInfoHardware(cpuInfo)
        val socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL else null
        val socManufacturer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MANUFACTURER else null
        val soc = HardwareNames.resolveSoc(
            socManufacturer,
            socModel,
            cpuInfoHardware,
            cpuInfoHardware?.substringAfterLast(' '),
            Build.HARDWARE,
            Build.BOARD,
        )

        val memory = ActivityManager.MemoryInfo().also {
            context.getSystemService(ActivityManager::class.java)?.getMemoryInfo(it)
        }

        DeviceInfo(
            deviceName = deviceName(),
            manufacturer = Build.MANUFACTURER.replaceFirstChar { it.titlecase(Locale.getDefault()) },
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            securityPatch = Build.VERSION.SECURITY_PATCH.takeIf { it.isNotBlank() },
            kernelVersion = System.getProperty("os.version")?.takeIf { it.isNotBlank() },
            primaryAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "—",
            soc = soc,
            cpuCoreCount = countCores(),
            gpuRenderer = gpuInfoReader.renderer(),
            vulkanVersion = vulkanVersion(),
            display = displayInfo(),
            totalRamBytes = memory.totalMem,
            totalStorageBytes = StatFs(Environment.getDataDirectory().path).totalBytes,
            widevineLevel = widevineReader.securityLevel(),
            sensorCount = context.getSystemService(SensorManager::class.java)
                ?.getSensorList(Sensor.TYPE_ALL)?.size ?: 0,
        )
    }

    /** El nombre que el usuario ve en Ajustes ("Galaxy A8") es más claro que "SM-A530W". */
    private fun deviceName(): String {
        val fromSettings = Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
        if (!fromSettings.isNullOrBlank()) return fromSettings
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.titlecase(Locale.getDefault()) }
        return if (Build.MODEL.startsWith(Build.MANUFACTURER, ignoreCase = true)) Build.MODEL
        else "$manufacturer ${Build.MODEL}"
    }

    private fun countCores(): Int {
        var count = 0
        while (SysFs.exists("/sys/devices/system/cpu/cpu$count")) count++
        return if (count > 0) count else Runtime.getRuntime().availableProcessors()
    }

    private fun vulkanVersion(): String? {
        val feature = context.packageManager.systemAvailableFeatures
            .firstOrNull { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION }
            ?: return null
        val version = feature.version
        if (version <= 0) return null
        return "${version shr 22}.${(version shr 12) and 0x3ff}"
    }

    private fun displayInfo(): DisplayInfo? {
        val display = context.getSystemService(DisplayManager::class.java)
            ?.getDisplay(Display.DEFAULT_DISPLAY) ?: return null
        val mode = display.mode
        val width = mode.physicalWidth
        val height = mode.physicalHeight
        val metrics = context.resources.displayMetrics
        val diagonal = if (metrics.xdpi > 50f && metrics.ydpi > 50f) {
            val w = width / metrics.xdpi
            val h = height / metrics.ydpi
            sqrt(w * w + h * h)
        } else null

        @Suppress("DEPRECATION")
        val hdr = (display.hdrCapabilities?.supportedHdrTypes ?: IntArray(0)).toList().mapNotNull {
            when (it) {
                Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> HdrType.DOLBY_VISION
                Display.HdrCapabilities.HDR_TYPE_HDR10 -> HdrType.HDR10
                Display.HdrCapabilities.HDR_TYPE_HLG -> HdrType.HLG
                HDR_TYPE_HDR10_PLUS -> HdrType.HDR10_PLUS
                else -> null
            }
        }.distinct()

        return DisplayInfo(
            widthPx = minOf(width, height),
            heightPx = maxOf(width, height),
            densityDpi = metrics.densityDpi,
            diagonalInches = diagonal,
            currentRefreshHz = display.refreshRate,
            maxRefreshHz = display.supportedModes.maxOfOrNull { it.refreshRate } ?: display.refreshRate,
            hdrTypes = hdr,
        )
    }

    private companion object {
        /** Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS es API 29; el valor es estable. */
        const val HDR_TYPE_HDR10_PLUS = 4
    }
}
