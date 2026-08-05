package me.tooobiiii.codingtime.core

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Splits a wall-clock interval into per-day slices so a session running across midnight is
 * attributed to both days. Pure logic — no IDE dependencies, so it is directly unit-testable.
 */
object DaySplitter {

    data class DaySlice(val date: LocalDate, val seconds: Long)

    fun split(startMillis: Long, endMillis: Long, zone: ZoneId = ZoneId.systemDefault()): List<DaySlice> {
        if (endMillis <= startMillis) return emptyList()

        val end = Instant.ofEpochMilli(endMillis).atZone(zone)
        var cursor = Instant.ofEpochMilli(startMillis).atZone(zone)
        val slices = mutableListOf<DaySlice>()

        while (cursor.toLocalDate() < end.toLocalDate()) {
            val nextDayStart = cursor.toLocalDate().plusDays(1).atStartOfDay(zone)
            slices += DaySlice(cursor.toLocalDate(), Duration.between(cursor, nextDayStart).seconds)
            cursor = nextDayStart
        }
        slices += DaySlice(cursor.toLocalDate(), Duration.between(cursor, end).seconds)

        return slices.filter { it.seconds > 0 }
    }
}
