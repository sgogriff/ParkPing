package com.gowain.parkping.monitor

import com.gowain.parkping.data.ParkPingRepository
import com.gowain.parkping.model.DailyReminderState
import com.gowain.parkping.model.PlaceConfig
import com.gowain.parkping.model.ReminderStatus
import com.gowain.parkping.model.TriggerSource
import com.gowain.parkping.notification.ReminderNotificationManager
import com.gowain.parkping.util.BusinessDayCalculator
import com.gowain.parkping.worker.ReminderWorkScheduler
import com.google.android.gms.location.Geofence
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TriggerProcessor @Inject constructor(
    private val repository: ParkPingRepository,
    private val notificationManager: ReminderNotificationManager,
    private val decisionEngine: TriggerDecisionEngine,
    private val businessDayCalculator: BusinessDayCalculator,
    private val workScheduler: ReminderWorkScheduler,
) {
    suspend fun handleGeofenceTransition(
        placeIds: Set<String>,
        transition: Int,
        now: Instant = Instant.now(),
    ) {
        val businessDate = businessDayCalculator.currentBusinessDate(now)
        rollOverIfNeeded(businessDate)
        when (transition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                repository.updateMonitoringStatus { current ->
                    current.copy(
                        lastGeofenceEventAt = now,
                        activeGeofencePlaceIds = current.activeGeofencePlaceIds + placeIds,
                        lastOutsideAllPlacesAt = null,
                    )
                }
                val snapshot = repository.currentSnapshot()
                val targetPlaceId = placeIds
                    .mapNotNull(snapshot.settings::placeById)
                    .sortedWith(compareBy<PlaceConfig> { it.radiusMeters }.thenBy { it.name.lowercase() })
                    .firstOrNull()
                    ?.placeId
                    ?: return
                handleArrivalTrigger(targetPlaceId, TriggerSource.GEOFENCE, now)
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                repository.updateMonitoringStatus { current ->
                    val activeGeofences = current.activeGeofencePlaceIds - placeIds
                    current.copy(
                        lastGeofenceEventAt = now,
                        activeGeofencePlaceIds = activeGeofences,
                        lastOutsideAllPlacesAt = if (
                            activeGeofences.isEmpty() &&
                            current.activeWifiPlaceIds.isEmpty()
                        ) {
                            current.lastOutsideAllPlacesAt ?: now
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }

    suspend fun handleWifiObservation(
        matchedPlaceId: String?,
        now: Instant = Instant.now(),
    ) {
        val businessDate = businessDayCalculator.currentBusinessDate(now)
        rollOverIfNeeded(businessDate)
        repository.updateMonitoringStatus { current ->
            val activeWifiPlaceIds = matchedPlaceId?.let(::setOf).orEmpty()
            current.copy(
                lastWifiEventAt = now,
                activeWifiPlaceIds = activeWifiPlaceIds,
                lastOutsideAllPlacesAt = if (
                    current.activeGeofencePlaceIds.isEmpty() &&
                    activeWifiPlaceIds.isEmpty()
                ) {
                    current.lastOutsideAllPlacesAt ?: now
                } else {
                    null
                },
            )
        }
        matchedPlaceId?.let { handleArrivalTrigger(it, TriggerSource.WIFI, now) }
    }

    suspend fun handleArrivalTrigger(
        placeId: String,
        source: TriggerSource,
        now: Instant = Instant.now(),
    ): TriggerDecision {
        val businessDate = businessDayCalculator.currentBusinessDate(now)
        rollOverIfNeeded(businessDate)
        recordTriggerEvent(source, now)
        val snapshot = repository.currentSnapshot()
        val place = snapshot.settings.placeById(placeId)
            ?: return TriggerDecision.Ignore("Triggered place no longer exists.")
        return when (val decision = decisionEngine.evaluate(place, snapshot.dailyReminderState, snapshot.monitoringStatus, source, now)) {
            TriggerDecision.Activate -> {
                repository.setDailyReminderState(
                    snapshot.dailyReminderState.copy(
                        businessDate = businessDate,
                        reminderStatus = ReminderStatus.ACTIVE,
                        completedAt = null,
                        snoozedUntil = null,
                        lastTriggerSource = source,
                        lastTriggerAt = now,
                        lastTriggerPlaceId = placeId,
                        activeNotification = true,
                    ),
                )
                repository.updateMonitoringStatus { current ->
                    current.copy(lastNotificationAt = now)
                }
                notificationManager.showReminder(snapshot.settings.reminderText)
                workScheduler.cancelSnooze()
                TriggerDecision.Activate
            }
            is TriggerDecision.Ignore -> decision
        }
    }

    suspend fun markDone(now: Instant = Instant.now()) {
        val businessDate = businessDayCalculator.currentBusinessDate(now)
        rollOverIfNeeded(businessDate)
        val snapshot = repository.currentSnapshot()
        repository.setDailyReminderState(
            snapshot.dailyReminderState.copy(
                businessDate = businessDate,
                reminderStatus = ReminderStatus.COMPLETED,
                completedAt = now,
                snoozedUntil = null,
                activeNotification = false,
            ),
        )
        workScheduler.cancelSnooze()
        notificationManager.cancelReminder()
    }

    suspend fun rearm(now: Instant = Instant.now()) {
        val businessDate = businessDayCalculator.currentBusinessDate(now)
        rollOverIfNeeded(businessDate)
        repository.setDailyReminderState(DailyReminderState.fresh(businessDate))
        workScheduler.cancelSnooze()
        notificationManager.cancelReminder()
    }

    suspend fun previewReminder(now: Instant = Instant.now()): Boolean {
        val businessDate = businessDayCalculator.currentBusinessDate(now)
        rollOverIfNeeded(businessDate)
        val snapshot = repository.currentSnapshot()
        val place = snapshot.settings.enabledPlaces.firstOrNull { it.hasGeofence || it.enabledSsids.isNotEmpty() }
            ?: return false
        val source = if (place.enabledSsids.isNotEmpty()) {
            TriggerSource.WIFI
        } else {
            TriggerSource.GEOFENCE
        }
        repository.setDailyReminderState(
            snapshot.dailyReminderState.copy(
                businessDate = businessDate,
                reminderStatus = ReminderStatus.ACTIVE,
                completedAt = null,
                snoozedUntil = null,
                lastTriggerSource = source,
                lastTriggerAt = now,
                lastTriggerPlaceId = place.placeId,
                activeNotification = true,
            ),
        )
        repository.updateMonitoringStatus { current ->
            current.copy(lastNotificationAt = now)
        }
        notificationManager.showReminder(snapshot.settings.reminderText)
        workScheduler.cancelSnooze()
        return true
    }

    suspend fun snooze(now: Instant = Instant.now()) {
        val businessDate = businessDayCalculator.currentBusinessDate(now)
        rollOverIfNeeded(businessDate)
        val snapshot = repository.currentSnapshot()
        if (snapshot.dailyReminderState.reminderStatus == ReminderStatus.COMPLETED) {
            return
        }
        val snoozedUntil = now.plus(Duration.ofMinutes(snapshot.settings.snoozeMinutes.toLong()))
        repository.setDailyReminderState(
            snapshot.dailyReminderState.copy(
                businessDate = businessDate,
                reminderStatus = ReminderStatus.SNOOZED,
                snoozedUntil = snoozedUntil,
                activeNotification = false,
            ),
        )
        notificationManager.cancelReminder()
        workScheduler.scheduleSnooze(snoozedUntil, now)
    }

    suspend fun refreshForCurrentDay(now: Instant = Instant.now()) {
        val businessDate = businessDayCalculator.currentBusinessDate(now)
        rollOverIfNeeded(businessDate)
        val snapshot = repository.currentSnapshot()
        when {
            snapshot.dailyReminderState.reminderStatus == ReminderStatus.ACTIVE && snapshot.dailyReminderState.activeNotification -> {
                notificationManager.showReminder(snapshot.settings.reminderText)
            }
            snapshot.dailyReminderState.reminderStatus == ReminderStatus.SNOOZED -> {
                val snoozedUntil = snapshot.dailyReminderState.snoozedUntil
                when {
                    snoozedUntil == null -> reissueSnoozedReminder(now)
                    snoozedUntil.isAfter(now) -> workScheduler.scheduleSnooze(snoozedUntil, now)
                    else -> reissueSnoozedReminder(now)
                }
            }
            else -> {
                workScheduler.cancelSnooze()
                notificationManager.cancelReminder()
            }
        }
    }

    suspend fun reissueSnoozedReminder(now: Instant = Instant.now()) {
        val businessDate = businessDayCalculator.currentBusinessDate(now)
        rollOverIfNeeded(businessDate)
        val snapshot = repository.currentSnapshot()
        val state = snapshot.dailyReminderState
        if (state.reminderStatus == ReminderStatus.COMPLETED || state.reminderStatus != ReminderStatus.SNOOZED) {
            notificationManager.cancelReminder()
            return
        }
        repository.setDailyReminderState(
            state.copy(
                businessDate = businessDate,
                reminderStatus = ReminderStatus.ACTIVE,
                snoozedUntil = null,
                activeNotification = true,
            ),
        )
        repository.updateMonitoringStatus { current ->
            current.copy(lastNotificationAt = now)
        }
        notificationManager.showReminder(snapshot.settings.reminderText)
        workScheduler.cancelSnooze()
    }

    private suspend fun rollOverIfNeeded(currentBusinessDate: LocalDate) {
        val snapshot = repository.currentSnapshot()
        if (snapshot.dailyReminderState.businessDate == currentBusinessDate) {
            return
        }
        repository.setDailyReminderState(DailyReminderState.fresh(currentBusinessDate))
        notificationManager.cancelReminder()
        workScheduler.cancelSnooze()
    }

    private suspend fun recordTriggerEvent(source: TriggerSource, now: Instant) {
        repository.updateMonitoringStatus { current ->
            when (source) {
                TriggerSource.GEOFENCE -> current.copy(lastGeofenceEventAt = now)
                TriggerSource.WIFI -> current.copy(lastWifiEventAt = now)
            }
        }
    }
}
