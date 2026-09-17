package com.rork.blockblastsolver.domain

import com.rork.blockblastsolver.data.ScanRecord
import java.util.Calendar

data class DayBucket(val label: String, val score: Int)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val unlocked: Boolean
)

data class StatsSummary(
    val totalScore: Int,
    val totalScans: Int,
    val averageCombo: Float,
    val bestDayScans: Int,
    val totalLines: Int,
    val streakDays: Int,
    val week: List<DayBucket>,
    val achievements: List<Achievement>
) {
    val isEmpty: Boolean get() = totalScans == 0
}

/** Derives every number shown on the Statistik tab from the saved history. */
object StatsCalculator {

    private val dayLabels = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

    fun compute(records: List<ScanRecord>, nowMillis: Long = System.currentTimeMillis()): StatsSummary {
        val totalScore = records.sumOf { it.gainedScore }
        val comboRecords = records.filter { it.linesCleared > 0 }
        val averageCombo = if (comboRecords.isEmpty()) 0f
        else comboRecords.sumOf { it.combo }.toFloat() / comboRecords.size

        val byDay = records.groupBy { dayKey(it.timestampMillis) }
        val bestDayScans = byDay.values.maxOfOrNull { it.size } ?: 0

        val week = buildWeek(byDay, nowMillis)
        val streak = streak(byDay.keys, nowMillis)
        val totalLines = records.sumOf { it.linesCleared }

        return StatsSummary(
            totalScore = totalScore,
            totalScans = records.size,
            averageCombo = averageCombo,
            bestDayScans = bestDayScans,
            totalLines = totalLines,
            streakDays = streak,
            week = week,
            achievements = achievements(records, averageCombo, streak, totalScore)
        )
    }

    private fun buildWeek(byDay: Map<Long, List<ScanRecord>>, nowMillis: Long): List<DayBucket> {
        val calendar = Calendar.getInstance()
        val buckets = mutableListOf<DayBucket>()
        for (offset in 6 downTo 0) {
            calendar.timeInMillis = nowMillis
            calendar.add(Calendar.DAY_OF_YEAR, -offset)
            val key = dayKey(calendar.timeInMillis)
            val score = byDay[key]?.sumOf { it.gainedScore } ?: 0
            // Calendar.MONDAY == 2, so shift into a Monday-first label list.
            val labelIndex = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
            buckets.add(DayBucket(dayLabels[labelIndex], score))
        }
        return buckets
    }

    private fun streak(days: Set<Long>, nowMillis: Long): Int {
        if (days.isEmpty()) return 0
        val calendar = Calendar.getInstance()
        var streak = 0
        for (offset in 0 until 365) {
            calendar.timeInMillis = nowMillis
            calendar.add(Calendar.DAY_OF_YEAR, -offset)
            if (days.contains(dayKey(calendar.timeInMillis))) {
                streak++
            } else if (offset > 0) {
                break
            }
        }
        return streak
    }

    private fun achievements(
        records: List<ScanRecord>,
        averageCombo: Float,
        streak: Int,
        totalScore: Int
    ): List<Achievement> = listOf(
        Achievement(
            id = "combo-master",
            title = "Combo Master",
            description = "Capai rata-rata combo ≥ 2.0",
            unlocked = averageCombo >= 2f
        ),
        Achievement(
            id = "streak-7",
            title = "7 Hari Beruntun",
            description = "Scan 7 hari berturut-turut",
            unlocked = streak >= 7
        ),
        Achievement(
            id = "triple-clear",
            title = "Triple Blast",
            description = "Hapus 3 baris dalam satu langkah",
            unlocked = records.any { it.linesCleared >= 3 }
        ),
        Achievement(
            id = "score-10k",
            title = "10K Club",
            description = "Kumpulkan total skor 10.000",
            unlocked = totalScore >= 10_000
        ),
        Achievement(
            id = "first-scan",
            title = "Langkah Pertama",
            description = "Selesaikan scan pertamamu",
            unlocked = records.isNotEmpty()
        ),
        Achievement(
            id = "fifty-scans",
            title = "Kolektor Papan",
            description = "Simpan 50 langkah ke riwayat",
            unlocked = records.size >= 50
        )
    )

    private fun dayKey(millis: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
