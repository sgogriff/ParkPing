package com.gowain.parkping.monitor

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.gowain.parkping.model.ParkPingSettings
import com.gowain.parkping.model.PermissionSnapshot
import com.gowain.parkping.receiver.GeofenceBroadcastReceiver
import com.gowain.parkping.util.ReminderIntentActions
import com.gowain.parkping.util.awaitResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeofenceManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    suspend fun sync(settings: ParkPingSettings, permissions: PermissionSnapshot): MonitoringSyncResult {
        val places = settings.enabledGeofencePlaces
        if (places.isEmpty()) {
            removeGeofenceSilently()
            return MonitoringSyncResult(registered = false)
        }
        if (!permissions.canRegisterGeofence) {
            removeGeofenceSilently()
            return MonitoringSyncResult(
                registered = false,
                errorMessage = "Background location permission is required before geofencing can be armed.",
            )
        }
        return registerGeofences(places)
    }

    @SuppressLint("MissingPermission")
    private suspend fun registerGeofences(
        places: List<com.gowain.parkping.model.PlaceConfig>,
    ): MonitoringSyncResult {
        return try {
            val geofences = places.map { place ->
                Geofence.Builder()
                    .setRequestId(place.placeId)
                    .setCircularRegion(
                        place.latitude ?: return MonitoringSyncResult(false),
                        place.longitude ?: return MonitoringSyncResult(false),
                        place.radiusMeters,
                    )
                    .setTransitionTypes(
                        Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT,
                    )
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .build()
            }
            val request = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofences)
                .build()
            geofencingClient.removeGeofences(geofencePendingIntent).awaitResult()
            geofencingClient.addGeofences(request, geofencePendingIntent).awaitResult()
            MonitoringSyncResult(registered = true)
        } catch (throwable: Throwable) {
            MonitoringSyncResult(
                registered = false,
                errorMessage = throwable.message ?: "Geofence registration failed.",
            )
        }
    }

    private suspend fun removeGeofenceSilently() {
        try {
            geofencingClient.removeGeofences(geofencePendingIntent).awaitResult()
        } catch (_: Throwable) {
            // Best effort cleanup only.
        }
    }

    private val geofencePendingIntent: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            context,
            GEOFENCE_REQUEST_CODE,
            Intent(context, GeofenceBroadcastReceiver::class.java).setAction(ReminderIntentActions.ACTION_GEOFENCE_EVENT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    companion object {
        private const val GEOFENCE_REQUEST_CODE = 1001
    }
}
