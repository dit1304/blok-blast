package com.rork.blockblastsolver.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val timeFormat = SimpleDateFormat("HH:mm", Locale("id", "ID"))
private val dateFormat = SimpleDateFormat("d MMM", Locale("id", "ID"))

/** "Hari ini, 14:32" / "Kemarin, 20:15" / "3 hari lalu, 11:27" / "12 Agu, 09:14". */
fun formatRelativeTimestamp(millis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val days = daysBetween(millis, nowMillis)
    val time = timeFormat.format(millis)
    return when {
        days == 0 -> "Hari ini, $time"
        days == 1 -> "Kemarin, $time"
        days in 2..6 -> "$days hari lalu, $time"
        else -> "${dateFormat.format(millis)}, $time"
    }
}

private fun daysBetween(from: Long, to: Long): Int {
    val start = startOfDay(from)
    val end = startOfDay(to)
    return ((end - start) / 86_400_000L).toInt()
}

private fun startOfDay(millis: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = millis
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

/** 12840 -> "12,840" to match the score styling in the design. */
fun formatScore(value: Int): String {
    val text = value.toString()
    if (text.length <= 3) return text
    return text.reversed().chunked(3).joinToString(",").reversed()
}
