package com.gowain.parkping.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gowain.parkping.monitor.TriggerProcessor
import com.gowain.parkping.util.ReminderIntentActions
import com.gowain.parkping.util.launchAsync
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {
    @Inject
    lateinit var triggerProcessor: TriggerProcessor

    override fun onReceive(context: Context, intent: Intent) {
        launchAsync {
            when (intent.action) {
                ReminderIntentActions.ACTION_MARK_DONE -> triggerProcessor.markDone()
                ReminderIntentActions.ACTION_SNOOZE -> triggerProcessor.snooze()
            }
        }
    }
}
