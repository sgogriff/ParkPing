package com.gowain.parkping.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gowain.parkping.monitor.TriggerProcessor
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SnoozeReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val triggerProcessor: TriggerProcessor,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        triggerProcessor.reissueSnoozedReminder()
        return Result.success()
    }
}
