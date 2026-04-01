package com.gowain.parkping.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gowain.parkping.data.ParkPingRepository
import com.gowain.parkping.model.TriggerSource
import com.gowain.parkping.monitor.TriggerProcessor
import com.gowain.parkping.monitor.WifiMonitor
import com.gowain.parkping.util.ReminderIntentActions
import com.gowain.parkping.util.launchAsync
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WifiEventReceiver : BroadcastReceiver() {
    @Inject
    lateinit var repository: ParkPingRepository

    @Inject
    lateinit var wifiMonitor: WifiMonitor

    @Inject
    lateinit var triggerProcessor: TriggerProcessor

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderIntentActions.ACTION_WIFI_EVENT) {
            return
        }
        launchAsync {
            val snapshot = repository.currentSnapshot()
            val currentSsid = wifiMonitor.currentSsid()
            val matchedPlaceId = snapshot.settings.findPlaceForSsid(currentSsid)?.placeId
            triggerProcessor.handleWifiObservation(matchedPlaceId)
        }
    }
}
