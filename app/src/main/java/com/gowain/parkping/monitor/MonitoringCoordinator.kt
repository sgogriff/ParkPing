package com.gowain.parkping.monitor

import com.gowain.parkping.data.ParkPingRepository
import com.gowain.parkping.model.PlaceConfig
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import com.gowain.parkping.worker.ReminderWorkScheduler

@Singleton
class MonitoringCoordinator @Inject constructor(
    private val repository: ParkPingRepository,
    private val permissionManager: PermissionManager,
    private val geofenceManager: GeofenceManager,
    private val wifiMonitor: WifiMonitor,
    private val triggerProcessor: TriggerProcessor,
    private val workScheduler: ReminderWorkScheduler,
) {
    suspend fun syncMonitoring(now: Instant = Instant.now()) {
        val snapshot = repository.currentSnapshot()
        val permissions = permissionManager.snapshot()
        val geofenceStatus = geofenceManager.sync(snapshot.settings, permissions)
        val wifiStatus = wifiMonitor.sync(snapshot.settings, permissions)
        val currentWifiPlaceId = if (wifiStatus.registered && permissions.canReadWifiIdentity) {
            snapshot.settings.findPlaceForSsid(wifiMonitor.currentSsid())?.placeId
        } else {
            null
        }
        val errorMessages = listOfNotNull(geofenceStatus.errorMessage, wifiStatus.errorMessage)
            .joinToString(separator = "\n")
            .ifBlank { null }
        repository.updateMonitoringStatus { current ->
            val activeGeofencePlaceIds = current.activeGeofencePlaceIds.intersect(
                snapshot.settings.enabledGeofencePlaces.map(PlaceConfig::placeId).toSet(),
            )
            val activeWifiPlaceIds = currentWifiPlaceId?.let(::setOf).orEmpty()
            current.copy(
                geofenceRegistered = geofenceStatus.registered,
                wifiMonitoringRegistered = wifiStatus.registered,
                lastRegistrationError = errorMessages,
                activeGeofencePlaceIds = if (geofenceStatus.registered) activeGeofencePlaceIds else emptySet(),
                activeWifiPlaceIds = activeWifiPlaceIds,
                lastOutsideAllPlacesAt = if (
                    (!geofenceStatus.registered || activeGeofencePlaceIds.isEmpty()) &&
                    activeWifiPlaceIds.isEmpty()
                ) {
                    current.lastOutsideAllPlacesAt ?: now
                } else {
                    null
                },
            )
        }
        workScheduler.scheduleDailyReset(now)
        triggerProcessor.refreshForCurrentDay(now)
        currentWifiPlaceId?.let { triggerProcessor.handleArrivalTrigger(it, com.gowain.parkping.model.TriggerSource.WIFI, now) }
    }
}
