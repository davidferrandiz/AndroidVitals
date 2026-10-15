package com.ferryapps.vitals.core.domain.battery

import kotlin.math.abs

/**
 * BatteryManager documenta la corriente en µA y el contador de carga en µAh, pero varios
 * fabricantes (muchos MediaTek, algunos Samsung y Motorola) devuelven mA/mAh y con el
 * signo invertido. Estas funciones lo normalizan a mA/mAh con el signo de la documentación:
 * positivo cargando, negativo descargando.
 */
object BatteryNormalizer {

    /** Por encima de este valor absoluto, la lectura está en micro-unidades. 20 A no existen en un móvil. */
    private const val MICRO_THRESHOLD = 20_000L

    /** Para el contador: un 0,5 % de una batería de 4.000 mAh ya son 20.000 µAh. */
    private const val COUNTER_MICRO_THRESHOLD = 30_000L

    fun currentToMa(raw: Long, isCharging: Boolean?): Int? {
        if (raw == Long.MIN_VALUE || raw == Int.MIN_VALUE.toLong() || raw == 0L) return null
        val ma = if (abs(raw) >= MICRO_THRESHOLD) raw / 1000 else raw
        if (ma == 0L) return null
        val signed = when (isCharging) {
            true -> abs(ma)
            false -> -abs(ma)
            null -> ma
        }
        return signed.toInt()
    }

    fun chargeCounterToMah(raw: Long): Int? {
        if (raw == Long.MIN_VALUE || raw == Int.MIN_VALUE.toLong() || raw <= 0L) return null
        val mah = if (raw >= COUNTER_MICRO_THRESHOLD) raw / 1000 else raw
        return mah.toInt().takeIf { it > 0 }
    }
}
