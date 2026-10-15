package com.ferryapps.vitals.core.domain.model

data class NetworkInfo(
    val transport: NetworkTransport,
    val wifi: WifiDetails? = null,
    /** Estimación del sistema del ancho de banda de bajada, en kbps. */
    val downstreamKbps: Int? = null,
)

enum class NetworkTransport { WIFI, CELLULAR, ETHERNET, VPN, OTHER, NONE }

data class WifiDetails(
    val frequencyMhz: Int?,
    /** Estándar Wi-Fi (4, 5, 6, 7…). Solo API 30+. */
    val generation: Int?,
    val rssiDbm: Int?,
    val linkSpeedMbps: Int?,
) {
    val band: WifiBand?
        get() = when (frequencyMhz) {
            null -> null
            in 2400..2500 -> WifiBand.GHZ_2_4
            in 4900..5900 -> WifiBand.GHZ_5
            in 5925..7125 -> WifiBand.GHZ_6
            else -> null
        }

    val signalQuality: SignalQuality?
        get() = rssiDbm?.let {
            when {
                it >= -60 -> SignalQuality.GOOD
                it >= -72 -> SignalQuality.FAIR
                else -> SignalQuality.POOR
            }
        }
}

enum class WifiBand { GHZ_2_4, GHZ_5, GHZ_6 }

enum class SignalQuality { GOOD, FAIR, POOR }
