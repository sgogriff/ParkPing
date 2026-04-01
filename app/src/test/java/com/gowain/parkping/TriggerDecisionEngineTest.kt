package com.gowain.parkping

import com.google.common.truth.Truth.assertThat
import com.gowain.parkping.model.DailyReminderState
import com.gowain.parkping.model.MonitoringStatus
import com.gowain.parkping.model.PlaceConfig
import com.gowain.parkping.model.PlaceTriggerBehavior
import com.gowain.parkping.model.ReminderStatus
import com.gowain.parkping.model.TriggerSource
import com.gowain.parkping.monitor.TriggerDecision
import com.gowain.parkping.monitor.TriggerDecisionEngine
import java.time.Instant
import org.junit.Test

class TriggerDecisionEngineTest {
    private val engine = TriggerDecisionEngine()
    private val now = Instant.parse("2026-04-01T08:00:00Z")
    private val place = PlaceConfig(
        placeId = "work",
        name = "Work",
        latitude = 60.1699,
        longitude = 24.9384,
    )

    @Test
    fun `activates when day is open`() {
        val decision = engine.evaluate(
            place = place,
            dailyReminderState = DailyReminderState(),
            monitoringStatus = MonitoringStatus(),
            source = TriggerSource.GEOFENCE,
            now = now,
        )

        assertThat(decision).isEqualTo(TriggerDecision.Activate)
    }

    @Test
    fun `ignores completed business day`() {
        val decision = engine.evaluate(
            place = place,
            dailyReminderState = DailyReminderState(
                reminderStatus = ReminderStatus.COMPLETED,
            ),
            monitoringStatus = MonitoringStatus(),
            source = TriggerSource.GEOFENCE,
            now = now,
        )

        assertThat(decision).isInstanceOf(TriggerDecision.Ignore::class.java)
    }

    @Test
    fun `ignores same place inside debounce window`() {
        val decision = engine.evaluate(
            place = place,
            dailyReminderState = DailyReminderState(
                lastTriggerPlaceId = place.placeId,
                lastTriggerAt = now.minusSeconds(60),
            ),
            monitoringStatus = MonitoringStatus(),
            source = TriggerSource.GEOFENCE,
            now = now,
        )

        assertThat(decision).isInstanceOf(TriggerDecision.Ignore::class.java)
    }

    @Test
    fun `ignores same place retrigger when away threshold is disabled`() {
        val decision = engine.evaluate(
            place = place,
            dailyReminderState = DailyReminderState(
                lastTriggerPlaceId = place.placeId,
                lastTriggerAt = now.minusSeconds(60 * 60),
            ),
            monitoringStatus = MonitoringStatus(
                lastOutsideAllPlacesAt = now.minusSeconds(60 * 60),
            ),
            source = TriggerSource.GEOFENCE,
            now = now,
        )

        assertThat(decision).isInstanceOf(TriggerDecision.Ignore::class.java)
    }

    @Test
    fun `activates when switching to a different place that allows retriggering`() {
        val switchPlace = place.copy(
            placeId = "office-2",
            name = "Overflow Office",
            triggerBehavior = PlaceTriggerBehavior(triggerOnPlaceSwitch = true),
        )

        val decision = engine.evaluate(
            place = switchPlace,
            dailyReminderState = DailyReminderState(
                lastTriggerPlaceId = place.placeId,
                lastTriggerAt = now.minusSeconds(60),
            ),
            monitoringStatus = MonitoringStatus(),
            source = TriggerSource.GEOFENCE,
            now = now,
        )

        assertThat(decision).isEqualTo(TriggerDecision.Activate)
    }

    @Test
    fun `activates when returning after configured away time`() {
        val returningPlace = place.copy(
            triggerBehavior = PlaceTriggerBehavior(returnAfterAwayMinutes = 30),
        )

        val decision = engine.evaluate(
            place = returningPlace,
            dailyReminderState = DailyReminderState(
                lastTriggerPlaceId = returningPlace.placeId,
                lastTriggerAt = now.minusSeconds(2 * 60 * 60),
            ),
            monitoringStatus = MonitoringStatus(
                lastOutsideAllPlacesAt = now.minusSeconds(45 * 60),
            ),
            source = TriggerSource.GEOFENCE,
            now = now,
        )

        assertThat(decision).isEqualTo(TriggerDecision.Activate)
    }
}
