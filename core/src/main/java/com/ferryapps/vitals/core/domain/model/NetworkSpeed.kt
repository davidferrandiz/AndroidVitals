package com.ferryapps.vitals.core.domain.model

data class NetworkSpeed(
    val rxBytesPerSec: Long,   // descarga
    val txBytesPerSec: Long    // subida
) {
    fun rxFormatted(): String = formatSpeed(rxBytesPerSec)
    fun txFormatted(): String = formatSpeed(txBytesPerSec)

    private fun formatSpeed(bps: Long): String = when {
        bps >= 1_000_000 -> "%.1f MB/s".format(bps / 1_000_000.0)
        bps >= 1_000     -> "%.0f KB/s".format(bps / 1_000.0)
        else             -> "$bps B/s"
    }
}
