package me.tooobiiii.codingtime.core

/**
 * Turns a stream of activity pings into closed coding [Segment]s.
 *
 * A segment stays open while pings keep arriving for the same language. It is closed when the
 * developer goes idle for longer than the configured timeout, when the language changes, or when
 * the caller explicitly flushes (project closing, IDE shutdown).
 *
 * Idle time is never part of a segment: the segment ends at the *last* ping, not at the moment the
 * timeout is noticed. Pure logic — no IDE dependencies, so it is directly unit-testable.
 *
 * Not thread-safe; callers guard it.
 */
class ActivityAccumulator {

    data class Segment(val startMillis: Long, val endMillis: Long, val language: String)

    private var startMillis = 0L
    private var lastActivityMillis = 0L
    private var language: String? = null

    /** The segment currently being recorded, spanning up to the last activity. */
    fun openSegment(): Segment? =
        language?.let { Segment(startMillis, lastActivityMillis, it) }

    /**
     * Registers activity in [language] at [now], returning a segment that this activity completed,
     * if any.
     */
    fun onActivity(now: Long, language: String, idleTimeoutMillis: Long): Segment? {
        val current = this.language
        if (current == null) {
            open(now, language)
            return null
        }

        val completed = when {
            now - lastActivityMillis > idleTimeoutMillis -> Segment(startMillis, lastActivityMillis, current)
            current != language -> Segment(startMillis, now, current)
            else -> null
        }

        if (completed == null) {
            lastActivityMillis = now
        } else {
            open(now, language)
        }
        return completed
    }

    /** Closes the open segment if the idle timeout has elapsed since the last activity. */
    fun onTick(now: Long, idleTimeoutMillis: Long): Segment? {
        val current = this.language ?: return null
        if (now - lastActivityMillis <= idleTimeoutMillis) return null
        return Segment(startMillis, lastActivityMillis, current).also { close() }
    }

    /** Closes the open segment unconditionally. */
    fun flush(): Segment? = openSegment()?.also { close() }

    private fun open(now: Long, language: String) {
        startMillis = now
        lastActivityMillis = now
        this.language = language
    }

    private fun close() {
        startMillis = 0
        lastActivityMillis = 0
        language = null
    }
}
