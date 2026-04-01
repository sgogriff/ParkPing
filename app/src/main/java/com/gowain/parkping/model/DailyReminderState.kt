package com.gowain.parkping.model

import java.time.Instant
import java.time.LocalDate

data class DailyReminderState(
    val businessDate: LocalDate? = null,
    val reminderStatus: ReminderStatus = ReminderStatus.IDLE,
    val completedAt: Instant? = null,
    val snoozedUntil: Instant? = null,
    val lastTriggerSource: TriggerSource? = null,
    val lastTriggerAt: Instant? = null,
    val lastTriggerPlaceId: String? = null,
    val activeNotification: Boolean = false,
) {
    companion object {
        fun fresh(businessDate: LocalDate) = DailyReminderState(businessDate = businessDate)
    }
}
