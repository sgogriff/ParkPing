package com.gowain.parkping.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gowain.parkping.monitor.MonitoringCoordinator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DailyResetWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val monitoringCoordinator: MonitoringCoordinator,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        monitoringCoordinator.syncMonitoring()
        return Result.success()
    }
}
