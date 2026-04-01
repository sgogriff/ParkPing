package com.gowain.parkping.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.gowain.parkping.model.TriggerSource
import com.gowain.parkping.monitor.TriggerProcessor
import com.gowain.parkping.util.ReminderIntentActions
import com.gowain.parkping.util.launchAsync
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {
    @Inject
    lateinit var triggerProcessor: TriggerProcessor

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderIntentActions.ACTION_GEOFENCE_EVENT) {
            return
        }
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            return
        }
        if (
            event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER &&
            event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_EXIT
        ) {
            return
        }
        val placeIds = event.triggeringGeofences
            ?.mapNotNull(Geofence::getRequestId)
            ?.toSet()
            .orEmpty()
        if (placeIds.isEmpty()) {
            return
        }
        launchAsync {
            triggerProcessor.handleGeofenceTransition(
                placeIds = placeIds,
                transition = event.geofenceTransition,
            )
        }
    }
}
