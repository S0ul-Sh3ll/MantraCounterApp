package com.starborn.mantracounter.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Locale-aware grouping, so an en-IN device reads 1,29,600 rather than 129,600. */
fun Long.grouped(): String = NumberFormat.getIntegerInstance().format(this)

fun Int.grouped(): String = NumberFormat.getIntegerInstance().format(this)

fun Float.asPercent(): String = "${(this * 100).toInt()}%"

fun formatDate(epochMillis: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(epochMillis))

/** Compact, human durations: "2h 14m", "6m 30s", "45s". */
fun formatDuration(millis: Long): String {
    if (millis < 1_000) return "0s"
    val seconds = millis / 1_000
    val hours = seconds / 3_600
    val minutes = (seconds % 3_600) / 60
    val rest = seconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${rest}s"
        else -> "${rest}s"
    }
}
