package io.github.miksask.linkagram.core.time

import io.github.miksask.linkagram.domain.HistoryDateFilter
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class HistoryDateBounds(
    val startInclusiveMillis: Long?,
    val endExclusiveMillis: Long?,
)

class HistoryDateRangeCalculator(
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    fun bounds(
        filter: HistoryDateFilter,
        customStartInclusiveMillis: Long? = null,
        customEndExclusiveMillis: Long? = null,
        zoneId: ZoneId = clock.zone,
    ): HistoryDateBounds {
        val today = LocalDate.now(clock.withZone(zoneId))
        return when (filter) {
            HistoryDateFilter.All -> HistoryDateBounds(null, null)
            HistoryDateFilter.Today -> dayRange(today, zoneId)
            HistoryDateFilter.Last7Days -> HistoryDateBounds(
                startInclusiveMillis = startOfDayMillis(today.minusDays(6), zoneId),
                endExclusiveMillis = startOfDayMillis(today.plusDays(1), zoneId),
            )
            HistoryDateFilter.Last30Days -> HistoryDateBounds(
                startInclusiveMillis = startOfDayMillis(today.minusDays(29), zoneId),
                endExclusiveMillis = startOfDayMillis(today.plusDays(1), zoneId),
            )
            HistoryDateFilter.Custom -> customBounds(
                customStartInclusiveMillis,
                customEndExclusiveMillis,
                zoneId,
            )
        }
    }

    fun customInclusiveLocalDates(
        startDate: LocalDate,
        endDate: LocalDate,
        zoneId: ZoneId = clock.zone,
    ): HistoryDateBounds {
        val start = minOf(startDate, endDate)
        val end = maxOf(startDate, endDate)
        return HistoryDateBounds(
            startInclusiveMillis = startOfDayMillis(start, zoneId),
            endExclusiveMillis = startOfDayMillis(end.plusDays(1), zoneId),
        )
    }

    private fun customBounds(
        startInclusiveMillis: Long?,
        endExclusiveMillis: Long?,
        zoneId: ZoneId,
    ): HistoryDateBounds {
        if (startInclusiveMillis == null && endExclusiveMillis == null) {
            return HistoryDateBounds(null, null)
        }
        if (startInclusiveMillis != null && endExclusiveMillis == null) {
            val day = Instant.ofEpochMilli(startInclusiveMillis).atZone(zoneId).toLocalDate()
            return dayRange(day, zoneId)
        }
        if (startInclusiveMillis == null && endExclusiveMillis != null) {
            // Treat single end as a single-day period ending at that exclusive bound's previous day.
            val endDay = Instant.ofEpochMilli(endExclusiveMillis - 1).atZone(zoneId).toLocalDate()
            return dayRange(endDay, zoneId)
        }
        val start = startInclusiveMillis!!
        val end = endExclusiveMillis!!
        return if (end < start) {
            HistoryDateBounds(end, start)
        } else {
            HistoryDateBounds(start, end)
        }
    }

    private fun dayRange(day: LocalDate, zoneId: ZoneId): HistoryDateBounds =
        HistoryDateBounds(
            startInclusiveMillis = startOfDayMillis(day, zoneId),
            endExclusiveMillis = startOfDayMillis(day.plusDays(1), zoneId),
        )

    private fun startOfDayMillis(day: LocalDate, zoneId: ZoneId): Long =
        day.atStartOfDay(zoneId).toInstant().toEpochMilli()
}
