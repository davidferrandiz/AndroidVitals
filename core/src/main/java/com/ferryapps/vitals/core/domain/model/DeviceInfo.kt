package com.ferryapps.vitals.core.domain.model

import com.ferryapps.vitals.core.domain.hardware.SocName

/** Datos fijos del dispositivo. Se leen una vez al abrir Hardware. */
data class DeviceInfo(
    /** Nombre que el usuario ve en Ajustes (ej. "Galaxy A8"), o fabricante + modelo. */
    val deviceName: String,
    val manufacturer: String,
    /** Código de modelo (ej. "SM-A530W"). */
    val model: String,
    val androidVersion: String,
    val sdkInt: Int,
    /** "YYYY-MM-DD", o null si el fabricante no lo informa. */
    val securityPatch: String?,
    val kernelVersion: String?,
    val primaryAbi: String,
    val soc: SocName,
    val cpuCoreCount: Int,
    val gpuRenderer: String?,
    /** Ej. "1.3". Null si no hay Vulkan. */
    val vulkanVersion: String?,
    val display: DisplayInfo?,
    val totalRamBytes: Long,
    val totalStorageBytes: Long,
    /** "L1", "L3"… o null si el dispositivo no tiene Widevine. */
    val widevineLevel: String?,
    val sensorCount: Int,
)

data class DisplayInfo(
    val widthPx: Int,
    val heightPx: Int,
    val densityDpi: Int,
    val diagonalInches: Float?,
    val currentRefreshHz: Float,
    val maxRefreshHz: Float,
    val hdrTypes: List<HdrType>,
)

enum class HdrType { DOLBY_VISION, HDR10, HLG, HDR10_PLUS }

/** Lectura en vivo de un sensor. `values` vacío si todavía no ha llegado ningún evento. */
data class SensorReading(
    val kind: SensorKind,
    val name: String,
    val values: List<Float>,
    /** Rango máximo del sensor. Para proximidad, por encima de la mitad = "lejos". */
    val maxRange: Float = 0f,
)

enum class SensorKind { ACCELEROMETER, GYROSCOPE, MAGNETOMETER, BAROMETER, LIGHT, PROXIMITY }
