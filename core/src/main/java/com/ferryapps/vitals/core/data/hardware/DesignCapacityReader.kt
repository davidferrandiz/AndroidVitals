package com.ferryapps.vitals.core.data.hardware

import android.annotation.SuppressLint
import android.content.Context
import com.ferryapps.vitals.core.domain.battery.BatteryHistoryMath
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Capacidad de diseño de la batería. Android no la expone en el SDK; se lee del perfil de
 * energía del fabricante (PowerProfile, API oculta pero permitida). Si falla o el valor es
 * el de por defecto de AOSP, devuelve null y la pantalla muestra solo la capacidad estimada.
 */
@Singleton
class DesignCapacityReader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val designCapacityMah: Int? by lazy { BatteryHistoryMath.plausibleDesignCapacity(readPowerProfile()) }

    @SuppressLint("PrivateApi", "DiscouragedPrivateApi")
    private fun readPowerProfile(): Double? = runCatching {
        val profileClass = Class.forName("com.android.internal.os.PowerProfile")
        val profile = profileClass.getConstructor(Context::class.java).newInstance(context)
        profileClass.getMethod("getBatteryCapacity").invoke(profile) as? Double
    }.getOrNull()
}
