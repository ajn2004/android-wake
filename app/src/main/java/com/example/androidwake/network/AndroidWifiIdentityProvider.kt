package com.example.androidwake.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.example.androidwake.domain.NetworkIdentity
import com.example.androidwake.domain.WifiIdentityProvider

class AndroidWifiIdentityProvider(context: Context) : WifiIdentityProvider {
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    override fun getCurrentIdentity(): NetworkIdentity? {
        return try {
            val activeNetwork = connectivityManager.activeNetwork ?: return null
            val caps = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return null
            if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null

            val info = wifiManager.connectionInfo ?: return null
            val ssid = info.ssid?.removePrefix("\"")?.removeSuffix("\"")
            val bssid = info.bssid
            if (ssid.isNullOrBlank() || ssid == "<unknown ssid>" || bssid.isNullOrBlank()) {
                return null
            }

            NetworkIdentity(ssid = ssid, bssid = bssid.uppercase())
        } catch (_: SecurityException) {
            null
        }
    }
}
