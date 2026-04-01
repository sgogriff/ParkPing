package com.gowain.parkping.util

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BusinessDayCalculator @Inject constructor() {
    private val rolloverHour = 4

    fun currentBusinessDate(now: Instant = Instant.now(), zoneId: ZoneId = ZoneId.systemDefault()): LocalDate {
        val zonedNow = now.atZone(zoneId)
        return if (zonedNow.hour < rolloverHour) {
            zonedNow.toLocalDate().minusDays(1)
        } else {
            zonedNow.toLocalDate()
        }
    }

    fun nextResetDelay(now: Instant = Instant.now(), zoneId: ZoneId = ZoneId.systemDefault()): Duration {
        val zonedNow = now.atZone(zoneId)
        val nextResetBaseDate = if (zonedNow.hour < rolloverHour) {
            zonedNow.toLocalDate()
        } else {
            zonedNow.toLocalDate().plusDays(1)
        }
        val nextReset = ZonedDateTime.of(
            nextResetBaseDate.year,
            nextResetBaseDate.monthValue,
            nextResetBaseDate.dayOfMonth,
            rolloverHour,
            0,
            0,
            0,
            zoneId,
        )
        val duration = Duration.between(zonedNow, nextReset)
        return if (duration.isNegative) Duration.ZERO else duration
    }
}
