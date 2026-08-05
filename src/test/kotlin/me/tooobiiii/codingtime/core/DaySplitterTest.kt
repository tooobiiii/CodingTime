package me.tooobiiii.codingtime.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class DaySplitterTest {

    private val zone = ZoneId.of("Europe/Berlin")

    @Test
    fun `interval within one day stays a single slice`() {
        val slices = split("2026-08-04T10:00", "2026-08-04T11:30")

        assertEquals(listOf(DaySplitter.DaySlice(LocalDate.parse("2026-08-04"), 5_400)), slices)
    }

    @Test
    fun `interval across midnight is attributed to both days`() {
        val slices = split("2026-08-04T23:30", "2026-08-05T00:20")

        assertEquals(
            listOf(
                DaySplitter.DaySlice(LocalDate.parse("2026-08-04"), 1_800),
                DaySplitter.DaySlice(LocalDate.parse("2026-08-05"), 1_200),
            ),
            slices,
        )
    }

    @Test
    fun `a multi-day interval produces one slice per day`() {
        val slices = split("2026-08-04T22:00", "2026-08-06T01:00")

        assertEquals(3, slices.size)
        assertEquals(LocalDate.parse("2026-08-05"), slices[1].date)
        assertEquals(86_400, slices[1].seconds)
        assertEquals(2 * 3_600 + 86_400 + 3_600, slices.sumOf { it.seconds })
    }

    @Test
    fun `empty and inverted intervals are ignored`() {
        assertTrue(split("2026-08-04T10:00", "2026-08-04T10:00").isEmpty())
        assertTrue(split("2026-08-04T11:00", "2026-08-04T10:00").isEmpty())
    }

    private fun split(from: String, to: String) = DaySplitter.split(millis(from), millis(to), zone)

    private fun millis(dateTime: String): Long =
        LocalDateTime.parse(dateTime).atZone(zone).toInstant().toEpochMilli()
}
