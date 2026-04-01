package com.gowain.parkping.model

import java.time.Instant

data class MonitoringStatus(
    val geofenceRegistered: Boolean = false,
    val wifiMonitoringRegistered: Boolean = false,
    val lastRegistrationError: String? = null,
    val lastGeofenceEventAt: Instant? = null,
    val lastWifiEventAt: Instant? = null,
    val lastNotificationAt: Instant? = null,
    val activeGeofencePlaceIds: Set<String> = emptySet(),
    val activeWifiPlaceIds: Set<String> = emptySet(),
    val lastOutsideAllPlacesAt: Instant? = null,
)
