package me.tooobiiii.codingtime.core

import java.time.LocalDate

/**
 * Groups daily totals into chart buckets, so that long ranges stay readable: a year of daily bars
 * would leave each one a couple of pixels wide, so days roll up into weeks and weeks into months.
 *
 * The coarsest granularity is only used when a finer one would exceed the bar budget, which keeps
 * short ranges exactly as detailed as they were before.
 *
 * Pure logic — no IDE dependencies, so it is directly unit-testable.
 */
object ChartBuckets {

    enum class Granularity { DAY, WEEK, MONTH }

    data class Bucket(
        val start: LocalDate,
        val end: LocalDate,
        val seconds: Long,
        val granularity: Granularity,
    )

    /** How many bars a chart can show before rolling up to a coarser granularity. */
    const val DEFAULT_MAX_BARS = 31

    fun of(days: List<DayTotal>, maxBars: Int = DEFAULT_MAX_BARS): List<Bucket> {
        if (days.isEmpty()) return emptyList()

        val ordered = days.sortedBy { it.date }
        val granularity = granularityFor(ordered, maxBars)
        if (granularity == Granularity.DAY) {
            return ordered.map { Bucket(it.date, it.date, it.seconds, granularity) }
        }

        return ordered.groupBy { startOfPeriod(it.date, granularity) }
            .map { (start, group) ->
                Bucket(start, group.last().date, group.sumOf { it.seconds }, granularity)
            }
    }

    private fun granularityFor(days: List<DayTotal>, maxBars: Int): Granularity = when {
        days.size <= maxBars -> Granularity.DAY
        periodCount(days, Granularity.WEEK) <= maxBars -> Granularity.WEEK
        else -> Granularity.MONTH
    }

    private fun periodCount(days: List<DayTotal>, granularity: Granularity): Int =
        days.mapTo(HashSet()) { startOfPeriod(it.date, granularity) }.size

    private fun startOfPeriod(date: LocalDate, granularity: Granularity): LocalDate = when (granularity) {
        Granularity.DAY -> date
        // ISO weeks start on Monday, independent of the platform locale.
        Granularity.WEEK -> date.minusDays((date.dayOfWeek.value - 1).toLong())
        Granularity.MONTH -> date.withDayOfMonth(1)
    }
}
