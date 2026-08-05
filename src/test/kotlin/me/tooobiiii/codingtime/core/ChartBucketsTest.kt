package me.tooobiiii.codingtime.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ChartBucketsTest {

    @Test
    fun `a short range keeps one bucket per day`() {
        val buckets = ChartBuckets.of(days("2026-08-01", count = 7))

        assertEquals(7, buckets.size)
        assertTrue(buckets.all { it.granularity == ChartBuckets.Granularity.DAY })
        assertEquals(LocalDate.parse("2026-08-01"), buckets.first().start)
        assertEquals(buckets.first().start, buckets.first().end)
    }

    @Test
    fun `a range at the bar budget is still daily`() {
        val buckets = ChartBuckets.of(days("2026-08-01", count = ChartBuckets.DEFAULT_MAX_BARS))

        assertEquals(ChartBuckets.DEFAULT_MAX_BARS, buckets.size)
        assertTrue(buckets.all { it.granularity == ChartBuckets.Granularity.DAY })
    }

    @Test
    fun `exceeding the bar budget rolls up into ISO weeks`() {
        // A Saturday start, so the first week is partial and must not be padded.
        val buckets = ChartBuckets.of(days("2026-08-01", count = 60))

        assertTrue(buckets.all { it.granularity == ChartBuckets.Granularity.WEEK })
        assertEquals(LocalDate.parse("2026-07-27"), buckets.first().start, "week starts on Monday")
        assertEquals(LocalDate.parse("2026-08-02"), buckets.first().end)
        assertEquals(120L, buckets.first().seconds, "only the two days that exist are counted")
    }

    @Test
    fun `a multi-year range rolls up into months`() {
        val buckets = ChartBuckets.of(days("2026-01-01", count = 800))

        assertTrue(buckets.all { it.granularity == ChartBuckets.Granularity.MONTH })
        assertEquals(27, buckets.size)
        assertEquals(LocalDate.parse("2026-01-01"), buckets.first().start)
        assertEquals(LocalDate.parse("2026-01-31"), buckets.first().end)
        assertEquals(1_860L, buckets.first().seconds)
    }

    @Test
    fun `bucket seconds add up to the input regardless of granularity`() {
        for (count in listOf(7, 30, 60, 400, 800)) {
            val input = days("2026-01-01", count)

            assertEquals(
                input.sumOf { it.seconds },
                ChartBuckets.of(input).sumOf { it.seconds },
                "totals must survive bucketing of $count days",
            )
        }
    }

    @Test
    fun `buckets come back in chronological order even when the input is shuffled`() {
        val buckets = ChartBuckets.of(days("2026-08-01", count = 60).reversed())

        assertEquals(buckets.sortedBy { it.start }, buckets)
    }

    @Test
    fun `no days produce no buckets`() {
        assertTrue(ChartBuckets.of(emptyList()).isEmpty())
    }

    /** [count] consecutive days of one minute each, starting at [start]. */
    private fun days(start: String, count: Int): List<DayTotal> {
        val first = LocalDate.parse(start)
        return (0 until count).map { DayTotal(first.plusDays(it.toLong()), 60) }
    }
}
