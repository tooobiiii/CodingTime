package me.tooobiiii.codingtime.core

/** Compact, human-readable duration: `0m`, `45s`, `12m`, `3h 42m`, `5h`. */
fun formatDuration(seconds: Long): String = when {
    seconds <= 0 -> "0m"
    seconds < 60 -> "${seconds}s"
    seconds < 3600 -> "${seconds / 60}m"
    else -> {
        val hours = seconds / 3600
        val minutes = seconds % 3600 / 60
        if (minutes == 0L) "${hours}h" else "${hours}h ${minutes}m"
    }
}
