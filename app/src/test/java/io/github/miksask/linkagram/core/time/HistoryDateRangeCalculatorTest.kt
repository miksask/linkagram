package io.github.miksask.linkagram.core.time

import io.github.miksask.linkagram.domain.HistoryDateFilter
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HistoryDateRangeCalculatorTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val clock = Clock.fixed(Instant.parse("2026-03-29T12:00:00Z"), zone)
    private val calculator = HistoryDateRangeCalculator(clock)

    @Test
    fun all_returnsOpenBounds() {
        val bounds = calculator.bounds(HistoryDateFilter.All, zoneId = zone)
        assertNull(bounds.startInclusiveMillis)
        assertNull(bounds.endExclusiveMillis)
    }

    @Test
    fun today_usesLocalMidnightBounds() {
        val bounds = calculator.bounds(HistoryDateFilter.Today, zoneId = zone)
        val expectedStart = LocalDate.of(2026, 3, 29).atStartOfDay(zone).toInstant().toEpochMilli()
        val expectedEnd = LocalDate.of(2026, 3, 30).atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals(expectedStart, bounds.startInclusiveMillis)
        assertEquals(expectedEnd, bounds.endExclusiveMillis)
    }

    @Test
    fun last7Days_includesTodayAndSixPriorDays() {
        val bounds = calculator.bounds(HistoryDateFilter.Last7Days, zoneId = zone)
        val expectedStart = LocalDate.of(2026, 3, 23).atStartOfDay(zone).toInstant().toEpochMilli()
        val expectedEnd = LocalDate.of(2026, 3, 30).atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals(expectedStart, bounds.startInclusiveMillis)
        assertEquals(expectedEnd, bounds.endExclusiveMillis)
    }

    @Test
    fun customInclusiveLocalDates_includesBothEnds() {
        val bounds = calculator.customInclusiveLocalDates(
            startDate = LocalDate.of(2026, 3, 20),
            endDate = LocalDate.of(2026, 3, 21),
            zoneId = zone,
        )
        val expectedStart = LocalDate.of(2026, 3, 20).atStartOfDay(zone).toInstant().toEpochMilli()
        val expectedEnd = LocalDate.of(2026, 3, 22).atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals(expectedStart, bounds.startInclusiveMillis)
        assertEquals(expectedEnd, bounds.endExclusiveMillis)
    }

    @Test
    fun customInclusiveLocalDates_swapsInvertedRange() {
        val bounds = calculator.customInclusiveLocalDates(
            startDate = LocalDate.of(2026, 3, 25),
            endDate = LocalDate.of(2026, 3, 20),
            zoneId = zone,
        )
        val expectedStart = LocalDate.of(2026, 3, 20).atStartOfDay(zone).toInstant().toEpochMilli()
        val expectedEnd = LocalDate.of(2026, 3, 26).atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals(expectedStart, bounds.startInclusiveMillis)
        assertEquals(expectedEnd, bounds.endExclusiveMillis)
    }
}
