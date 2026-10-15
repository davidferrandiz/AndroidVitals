package com.ferryapps.vitals.designsystem.format

import android.content.Context
import android.text.format.Formatter
import com.ferryapps.vitals.core.domain.model.TemperatureUnit
import com.ferryapps.vitals.designsystem.R
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Formateo común. Todo usa el locale del sistema, así que en persa salen dígitos persas
 * y en alemán coma decimal sin hacer nada más.
 */
object Formatters {

    /** Países que usan Fahrenheit. */
    private val FAHRENHEIT_COUNTRIES = setOf("US", "LR", "MM", "BS", "BZ", "KY", "PW")

    fun resolveUnit(unit: TemperatureUnit, locale: Locale = Locale.getDefault()): TemperatureUnit = when (unit) {
        TemperatureUnit.AUTO ->
            if (locale.country.uppercase(Locale.ROOT) in FAHRENHEIT_COUNTRIES) TemperatureUnit.FAHRENHEIT
            else TemperatureUnit.CELSIUS
        else -> unit
    }

    /** "31,5 °C" o "88,7 °F". */
    fun temperature(celsius: Float, unit: TemperatureUnit, decimals: Int = 1): String {
        val resolved = resolveUnit(unit)
        val value = if (resolved == TemperatureUnit.FAHRENHEIT) celsius * 9f / 5f + 32f else celsius
        val symbol = if (resolved == TemperatureUnit.FAHRENHEIT) "°F" else "°C"
        return "${decimal(value, decimals)} $symbol"
    }

    /** Solo el número, para mostrar la unidad aparte con otro estilo. */
    fun temperatureValue(celsius: Float, unit: TemperatureUnit, decimals: Int = 1): Pair<String, String> {
        val resolved = resolveUnit(unit)
        val value = if (resolved == TemperatureUnit.FAHRENHEIT) celsius * 9f / 5f + 32f else celsius
        return decimal(value, decimals) to if (resolved == TemperatureUnit.FAHRENHEIT) "°F" else "°C"
    }

    fun decimal(value: Float, decimals: Int = 1): String =
        String.format(Locale.getDefault(), "%.${decimals}f", value)

    fun integer(value: Number): String = String.format(Locale.getDefault(), "%,d", value.toLong())

    fun percent(value: Float, decimals: Int = 0): String = "${decimal(value, decimals)} %"

    /** Bytes con el mismo criterio que Ajustes de Android (1 GB = 10⁹ bytes). */
    fun bytes(context: Context, bytes: Long): String = Formatter.formatShortFileSize(context, bytes)

    /** "2,91 GHz" o "980 MHz". */
    fun frequency(mhz: Int): String =
        if (mhz >= 1000) "${decimal(mhz / 1000f, 2)} GHz" else "${integer(mhz)} MHz"

    /** "+2.880 mA". */
    fun current(ma: Int): String = (if (ma > 0) "+" else if (ma < 0) "−" else "") + "${integer(abs(ma))} mA"

    /** "12,4 W". El signo se omite: el contexto (cargando/descargando) ya lo dice. */
    fun power(watts: Float): String = "${decimal(abs(watts), 1)} W"

    /** "41 min", "6 h 20 min". */
    fun duration(context: Context, millis: Long): String {
        val totalMinutes = (millis / 60_000.0).roundToInt().coerceAtLeast(1)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours == 0 -> context.getString(R.string.ds_duration_minutes, minutes)
            minutes == 0 -> context.getString(R.string.ds_duration_hours, hours)
            else -> context.getString(R.string.ds_duration_hours_minutes, hours, minutes)
        }
    }

    /** Tiempo encendido: "3 d 4 h" o "5 h 12 min". */
    fun uptime(context: Context, millis: Long): String {
        val totalMinutes = millis / 60_000
        val days = totalMinutes / (60 * 24)
        val hours = (totalMinutes / 60) % 24
        val minutes = totalMinutes % 60
        return if (days > 0) context.getString(R.string.ds_uptime_days_hours, days.toInt(), hours.toInt())
        else context.getString(R.string.ds_duration_hours_minutes, hours.toInt(), minutes.toInt())
    }
}
