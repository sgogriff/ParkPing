package com.gowain.parkping.model

data class PermissionSnapshot(
    val notificationsGranted: Boolean = true,
    val fineLocationGranted: Boolean = false,
    val backgroundLocationGranted: Boolean = false,
) {
    val canRegisterGeofence: Boolean
        get() = fineLocationGranted && backgroundLocationGranted

    val canReadWifiIdentity: Boolean
        get() = fineLocationGranted
}
