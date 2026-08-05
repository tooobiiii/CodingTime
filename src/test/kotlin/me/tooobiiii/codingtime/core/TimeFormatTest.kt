package me.tooobiiii.codingtime.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TimeFormatTest {

    @Test
    fun `durations are formatted compactly`() {
        assertEquals("0m", formatDuration(0))
        assertEquals("0m", formatDuration(-5))
        assertEquals("45s", formatDuration(45))
        assertEquals("1m", formatDuration(60))
        assertEquals("12m", formatDuration(12 * 60 + 59))
        assertEquals("1h", formatDuration(3_600))
        assertEquals("3h 42m", formatDuration(3 * 3_600 + 42 * 60))
    }
}
