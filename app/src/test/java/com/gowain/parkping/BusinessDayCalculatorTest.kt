package com.gowain.parkping

import com.google.common.truth.Truth.assertThat
import com.gowain.parkping.util.BusinessDayCalculator
import java.time.Instant
import java.time.ZoneId
import org.junit.Test

class BusinessDayCalculatorTest {
    private val calculator = BusinessDayCalculator()
    private val zoneId = ZoneId.of("Europe/Helsinki")

    @Test
    fun `uses previous day before four am`() {
        val instant = Instant.parse("2026-04-01T00:30:00Z")

        val businessDate = calculator.currentBusinessDate(instant, zoneId)

        assertThat(businessDate.toString()).isEqualTo("2026-03-31")
    }

    @Test
    fun `uses current day at or after four am`() {
        val instant = Instant.parse("2026-04-01T02:15:00Z")

        val businessDate = calculator.currentBusinessDate(instant, zoneId)

        assertThat(businessDate.toString()).isEqualTo("2026-04-01")
    }

    @Test
    fun `calculates delay until next reset`() {
        val instant = Instant.parse("2026-04-01T10:00:00Z")

        val delay = calculator.nextResetDelay(instant, zoneId)

        assertThat(delay.toHours()).isEqualTo(15)
    }
}
