package com.gowain.parkping.worker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.gowain.parkping.util.BusinessDayCalculator
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderWorkScheduler @Inject constructor(
    @ApplicationContext context: Context,
    private val businessDayCalculator: BusinessDayCalculator,
) {
    private val workManager = WorkManager.getInstance(context)

    fun scheduleDailyReset(now: Instant = Instant.now()) {
        val delay = businessDayCalculator.nextResetDelay(now)
        val request = OneTimeWorkRequestBuilder<DailyResetWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(DAILY_RESET_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    fun scheduleSnooze(until: Instant, now: Instant = Instant.now()) {
        val delay = Duration.between(now, until).let { if (it.isNegative) Duration.ZERO else it }
        val request = OneTimeWorkRequestBuilder<SnoozeReminderWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(SNOOZE_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelSnooze() {
        workManager.cancelUniqueWork(SNOOZE_WORK)
    }

    companion object {
        private const val DAILY_RESET_WORK = "daily-reset"
        private const val SNOOZE_WORK = "snooze-reissue"
    }
}
