package com.gowain.parkping.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gowain.parkping.monitor.MonitoringCoordinator
import com.gowain.parkping.util.launchAsync
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {
    @Inject
    lateinit var monitoringCoordinator: MonitoringCoordinator

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }
        launchAsync {
            monitoringCoordinator.syncMonitoring()
        }
    }
}
