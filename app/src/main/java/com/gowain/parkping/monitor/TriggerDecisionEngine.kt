package com.gowain.parkping.monitor

import com.gowain.parkping.model.DailyReminderState
import com.gowain.parkping.model.MonitoringStatus
import com.gowain.parkping.model.PlaceConfig
import com.gowain.parkping.model.ReminderStatus
import com.gowain.parkping.model.TriggerSource
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

sealed interface TriggerDecision {
    data object Activate : TriggerDecision

    data class Ignore(val reason: String) : TriggerDecision
}

@Singleton
class TriggerDecisionEngine @Inject constructor() {
    private val debounceWindow = Duration.ofMinutes(15)

    fun evaluate(
        place: PlaceConfig,
        dailyReminderState: DailyReminderState,
        monitoringStatus: MonitoringStatus,
        source: TriggerSource,
        now: Instant,
    ): TriggerDecision {
        if (!place.enabled) {
            return TriggerDecision.Ignore("Place is disabled.")
        }
        if (source == TriggerSource.GEOFENCE && !place.hasGeofence) {
            return TriggerDecision.Ignore("Geofence trigger is not configured for this place.")
        }
        if (source == TriggerSource.WIFI && place.enabledSsids.isEmpty()) {
            return TriggerDecision.Ignore("Wi-Fi trigger is not configured for this place.")
        }
        if (dailyReminderState.reminderStatus == ReminderStatus.COMPLETED) {
            return TriggerDecision.Ignore("Parking has already been confirmed for the current business day.")
        }
        if (dailyReminderState.snoozedUntil?.isAfter(now) == true) {
            return TriggerDecision.Ignore("Reminder is currently snoozed.")
        }
        val lastTriggerAt = dailyReminderState.lastTriggerAt
        val lastTriggerPlaceId = dailyReminderState.lastTriggerPlaceId
        if (lastTriggerPlaceId == null || lastTriggerAt == null) {
            return TriggerDecision.Activate
        }
        if (lastTriggerPlaceId == place.placeId && lastTriggerAt.plus(debounceWindow).isAfter(now)) {
            return TriggerDecision.Ignore("Recent trigger still within debounce window.")
        }
        if (lastTriggerPlaceId != place.placeId) {
            return if (place.triggerBehavior.triggerOnPlaceSwitch) {
                TriggerDecision.Activate
            } else {
                TriggerDecision.Ignore("Place switch retriggering is disabled.")
            }
        }
        val returnAfterAwayMinutes = place.triggerBehavior.returnAfterAwayMinutes
            ?: return TriggerDecision.Ignore("Same-place retriggering is disabled.")
        val lastOutsideAllPlacesAt = monitoringStatus.lastOutsideAllPlacesAt
            ?: return TriggerDecision.Ignore("Return-away timer has not started yet.")
        return if (lastOutsideAllPlacesAt.plus(Duration.ofMinutes(returnAfterAwayMinutes.toLong())).isAfter(now)) {
            TriggerDecision.Ignore("Away duration is still below the configured retrigger threshold.")
        } else {
            TriggerDecision.Activate
        }
    }
}
