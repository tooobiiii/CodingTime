package me.tooobiiii.codingtime.core

import java.time.LocalDate

/** Coding time recorded on a single day. */
data class DayTotal(val date: LocalDate, val seconds: Long)
