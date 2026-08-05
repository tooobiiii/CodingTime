package me.tooobiiii.codingtime.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ActivityAccumulatorTest {

    private val timeout = 120_000L
    private val accumulator = ActivityAccumulator()

    private companion object {
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }

    @Test
    fun `continuous activity stays in one segment`() {
        assertNull(accumulator.onActivity(0, "Kotlin", timeout))
        assertNull(accumulator.onActivity(30_000, "Kotlin", timeout))
        assertNull(accumulator.onActivity(60_000, "Kotlin", timeout))

        assertEquals(ActivityAccumulator.Segment(0, 60_000, "Kotlin"), accumulator.flush())
    }

    @Test
    fun `idle gap closes the segment at the last activity, not at the resume`() {
        accumulator.onActivity(0, "Kotlin", timeout)
        accumulator.onActivity(60_000, "Kotlin", timeout)

        val completed = accumulator.onActivity(600_000, "Kotlin", timeout)

        assertEquals(ActivityAccumulator.Segment(0, 60_000, "Kotlin"), completed)
        assertEquals(ActivityAccumulator.Segment(600_000, 600_000, "Kotlin"), accumulator.openSegment())
    }

    @Test
    fun `switching language splits the segment without losing time`() {
        accumulator.onActivity(0, "Kotlin", timeout)

        val completed = accumulator.onActivity(45_000, "JSON", timeout)

        assertEquals(ActivityAccumulator.Segment(0, 45_000, "Kotlin"), completed)
        assertEquals(ActivityAccumulator.Segment(45_000, 45_000, "JSON"), accumulator.openSegment())
    }

    @Test
    fun `tick closes the segment once the idle timeout has passed`() {
        accumulator.onActivity(0, "Kotlin", timeout)

        assertNull(accumulator.onTick(100_000, timeout))
        assertEquals(ActivityAccumulator.Segment(0, 0, "Kotlin"), accumulator.onTick(200_000, timeout))
        assertNull(accumulator.openSegment())
        assertNull(accumulator.onTick(300_000, timeout))
    }

    @Test
    fun `an infinite timeout keeps one session open across any gap`() {
        val never = Long.MAX_VALUE
        accumulator.onActivity(0, "Kotlin", never)

        assertNull(accumulator.onActivity(DAY_MILLIS, "Kotlin", never))
        assertNull(accumulator.onTick(DAY_MILLIS * 2, never))
        assertEquals(ActivityAccumulator.Segment(0, DAY_MILLIS, "Kotlin"), accumulator.flush())
    }

    @Test
    fun `flush without activity yields nothing`() {
        assertNull(accumulator.flush())
    }
}
