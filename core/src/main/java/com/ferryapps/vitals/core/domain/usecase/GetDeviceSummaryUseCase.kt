package com.ferryapps.vitals.core.domain.usecase

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.ferryapps.vitals.core.data.sysfs.SysFs
import com.ferryapps.vitals.core.domain.hardware.HardwareNames
import com.ferryapps.vitals.core.domain.hardware.SocName
import com.ferryapps.vitals.core.domain.parser.ProcParsers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

data class DeviceSummary(
    val deviceName: String,
    val androidVersion: String,
    val soc: SocName,
)

/** Nombre del móvil, versión de Android y SoC. Barato: sin EGL ni MediaDrm. */
class GetDeviceSummaryUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(): DeviceSummary = withContext(Dispatchers.IO) {
        val hardware = SysFs.read("/proc/cpuinfo")?.let(ProcParsers::parseCpuInfoHardware)
        val socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL else null
        val socManufacturer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MANUFACTURER else null
        DeviceSummary(
            deviceName = deviceName(),
            androidVersion = Build.VERSION.RELEASE,
            soc = HardwareNames.resolveSoc(
                socManufacturer, socModel, hardware, hardware?.substringAfterLast(' '), Build.HARDWARE, Build.BOARD
            ),
        )
    }

    private fun deviceName(): String {
        val fromSettings = Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
        if (!fromSettings.isNullOrBlank()) return fromSettings
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.titlecase(Locale.getDefault()) }
        return if (Build.MODEL.startsWith(Build.MANUFACTURER, ignoreCase = true)) Build.MODEL
        else "$manufacturer ${Build.MODEL}"
    }
}
