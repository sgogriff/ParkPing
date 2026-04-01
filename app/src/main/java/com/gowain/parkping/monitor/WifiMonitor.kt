package com.gowain.parkping.monitor

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import com.gowain.parkping.model.ParkPingSettings
import com.gowain.parkping.model.PermissionSnapshot
import com.gowain.parkping.receiver.WifiEventReceiver
import com.gowain.parkping.util.ReminderIntentActions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiMonitor @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun sync(settings: ParkPingSettings, permissions: PermissionSnapshot): MonitoringSyncResult {
        if (settings.enabledWifiPlaces.isEmpty()) {
            unregisterSilently()
            return MonitoringSyncResult(registered = false)
        }
        if (!permissions.canReadWifiIdentity) {
            unregisterSilently()
            return MonitoringSyncResult(
                registered = false,
                errorMessage = "Foreground location permission is required for SSID matching.",
            )
        }
        return try {
            unregisterSilently()
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            connectivityManager.registerNetworkCallback(request, wifiPendingIntent)
            MonitoringSyncResult(registered = true)
        } catch (throwable: Throwable) {
            MonitoringSyncResult(
                registered = false,
                errorMessage = throwable.message ?: "Wi-Fi monitoring registration failed.",
            )
        }
    }

    @Suppress("DEPRECATION")
    fun currentSsid(): String? {
        val activeNetwork = connectivityManager.activeNetwork ?: return null
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val transportInfo = capabilities?.transportInfo as? WifiInfo
        val rawSsid = transportInfo?.ssid ?: wifiManager.connectionInfo?.ssid
        return normalizeSsid(rawSsid)
    }

    fun normalizeSsid(ssid: String?): String? {
        val normalized = ssid
            ?.removePrefix("\"")
            ?.removeSuffix("\"")
            ?.trim()
            .orEmpty()
        return normalized.takeUnless { it.isBlank() || it == "<unknown ssid>" }
    }

    private fun unregisterSilently() {
        try {
            connectivityManager.unregisterNetworkCallback(wifiPendingIntent)
        } catch (_: Throwable) {
            // No-op.
        }
    }

    private val wifiPendingIntent: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            context,
            WIFI_REQUEST_CODE,
            Intent(context, WifiEventReceiver::class.java).setAction(ReminderIntentActions.ACTION_WIFI_EVENT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    companion object {
        private const val WIFI_REQUEST_CODE = 1002
    }
}
