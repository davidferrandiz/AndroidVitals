package com.ferryapps.vitals.core.domain.usecase

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import com.ferryapps.vitals.core.domain.model.NetworkInfo
import com.ferryapps.vitals.core.domain.model.NetworkTransport
import com.ferryapps.vitals.core.domain.model.WifiDetails
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * Tipo de conexión y detalles de Wi-Fi (banda, estándar, señal, velocidad de enlace).
 * No pide ubicación: el SSID queda oculto, pero la señal y la banda no lo necesitan.
 */
class GetNetworkInfoUseCase @Inject constructor(
    @ApplicationContext context: Context
) {
    private val connectivityManager: ConnectivityManager? = context.getSystemService(ConnectivityManager::class.java)
    private val wifiManager: WifiManager? = context.applicationContext.getSystemService(WifiManager::class.java)

    operator fun invoke(): Flow<NetworkInfo> = flow {
        while (true) {
            emit(read())
            delay(3_000)
        }
    }.flowOn(Dispatchers.IO)

    private fun read(): NetworkInfo {
        val manager = connectivityManager ?: return NetworkInfo(NetworkTransport.NONE)
        val network = manager.activeNetwork ?: return NetworkInfo(NetworkTransport.NONE)
        val caps = manager.getNetworkCapabilities(network) ?: return NetworkInfo(NetworkTransport.NONE)
        val transport = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkTransport.VPN
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkTransport.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkTransport.CELLULAR
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkTransport.ETHERNET
            else -> NetworkTransport.OTHER
        }
        return NetworkInfo(
            transport = transport,
            wifi = if (transport == NetworkTransport.WIFI) readWifi(caps) else null,
            downstreamKbps = caps.linkDownstreamBandwidthKbps.takeIf { it > 0 },
        )
    }

    private fun readWifi(caps: NetworkCapabilities): WifiDetails? {
        val info: WifiInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            caps.transportInfo as? WifiInfo
        } else {
            @Suppress("DEPRECATION")
            wifiManager?.connectionInfo
        }
        if (info == null) return null

        val generation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            when (info.wifiStandard) {
                ScanResult.WIFI_STANDARD_11N -> 4
                ScanResult.WIFI_STANDARD_11AC -> 5
                ScanResult.WIFI_STANDARD_11AX -> 6
                WIFI_STANDARD_11BE -> 7
                else -> null
            }
        } else null

        return WifiDetails(
            frequencyMhz = info.frequency.takeIf { it > 0 },
            generation = generation,
            rssiDbm = info.rssi.takeIf { it in -126..-1 },
            linkSpeedMbps = info.linkSpeed.takeIf { it > 0 },
        )
    }

    private companion object {
        /** ScanResult.WIFI_STANDARD_11BE (Wi-Fi 7) es API 33; el valor es estable. */
        const val WIFI_STANDARD_11BE = 8
    }
}
