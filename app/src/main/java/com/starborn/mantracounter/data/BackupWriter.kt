package com.starborn.mantracounter.data

import com.starborn.mantracounter.util.formatDuration
import com.starborn.mantracounter.util.grouped
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Renders the whole app to a plain-text backup: every japa, its full statistics, and its
 * day-by-day log. Plain text on purpose — it stays readable in ten years without this app.
 */
object BackupWriter {

    private const val RULE = "════════════════════════════════════════════════════════"
    private const val THIN = "────────────────────────────────────────────────────────"

    fun render(snapshot: List<JapaWithLog>, generatedAt: Long): String = buildString {
        val active = snapshot.filter { !it.japa.archived }
        val archived = snapshot.filter { it.japa.archived }

        appendLine(RULE)
        appendLine("MANTRA COUNTER — BACKUP")
        appendLine("Generated: ${formatDateTime(generatedAt)}")
        appendLine(RULE)
        appendLine()

        appendLine("SUMMARY")
        appendLine(THIN)
        appendLine("Japas          : ${snapshot.size} (${active.size} active, ${archived.size} archived)")
        appendLine("Total japa     : ${snapshot.sumOf { it.japa.count }.grouped()}")
        appendLine("Total malas    : ${snapshot.sumOf { it.japa.malasCompleted }.grouped()}")
        appendLine("Total time     : ${formatDuration(snapshot.sumOf { it.japa.totalTimeMs })}")
        val allDays = snapshot.flatMap { it.log }.map { it.date }.distinct()
        appendLine("Days recorded  : ${allDays.size}")
        if (allDays.isNotEmpty()) {
            appendLine("First activity : ${allDays.min()}")
            appendLine("Last activity  : ${allDays.max()}")
        }
        appendLine()

        if (active.isNotEmpty()) {
            appendLine(RULE)
            appendLine("ACTIVE JAPAS")
            appendLine(RULE)
            active.forEach { appendLine(renderJapa(it)) }
        }

        if (archived.isNotEmpty()) {
            appendLine(RULE)
            appendLine("ARCHIVED JAPAS")
            appendLine(RULE)
            archived.forEach { appendLine(renderJapa(it)) }
        }

        appendLine(RULE)
        appendLine("End of backup.")
        appendLine(RULE)
    }

    private fun renderJapa(entry: JapaWithLog): String = buildString {
        val japa = entry.japa
        val log = entry.log.sortedBy { it.date }
        val stats = statsOf(log)

        appendLine()
        appendLine(japa.name)
        appendLine(THIN)
        if (japa.deity.isNotBlank()) appendLine("Deity             : ${japa.deity}")
        appendLine("Total count       : ${japa.count.grouped()} japa")
        appendLine("Japa per mala     : ${japa.malaSize.grouped()}")
        appendLine("Malas completed   : ${japa.malasCompleted.grouped()}")
        appendLine("Into current mala : ${japa.beadsIntoMala.grouped()} / ${japa.malaSize.grouped()}")

        if (japa.hasTarget) {
            appendLine("Lifetime target   : ${japa.lifetimeTarget.grouped()} japa " +
                "(${(japa.lifetimeTarget / japa.malaSize.coerceAtLeast(1)).grouped()} malas)")
            appendLine("Target progress   : ${(japa.targetProgress * 100).format2()}%")
            appendLine(
                if (japa.targetReached) "Target status     : reached"
                else "Remaining         : ${japa.targetRemaining.grouped()} japa"
            )
            if (!japa.targetReached && stats.averagePerActiveDay > 0) {
                val days = Math.ceil(japa.targetRemaining / stats.averagePerActiveDay).toLong()
                appendLine("At current pace   : ~${days.grouped()} more days of chanting")
            }
        } else {
            appendLine("Lifetime target   : none set")
        }

        appendLine("Created           : ${isoDate(japa.createdAt)}")
        appendLine("Last counted      : ${isoDate(japa.updatedAt)}")
        if (japa.archived && japa.archivedAt != null) {
            appendLine("Archived          : ${isoDate(japa.archivedAt)}")
        }
        appendLine("Time spent        : ${formatDuration(japa.totalTimeMs)}")
        appendLine("Time spent (ms)   : ${japa.totalTimeMs}")
        appendLine("Accent colour     : ${japa.accentIndex}")
        appendLine("Favourite         : ${if (japa.favourite) "yes" else "no"}")
        appendLine("Background image  : ${if (japa.backgroundUri != null) "set" else "none"}")

        appendLine()
        appendLine("  Statistics")
        appendLine("  Days chanted      : ${stats.activeDays}")
        appendLine("  Logged japa       : ${stats.total.grouped()}")
        appendLine("  Best day          : " + (stats.bestDay?.let {
            "${it.count.grouped()} on ${it.date}"
        } ?: "—"))
        appendLine("  Average per day   : ${stats.averagePerActiveDay.format1()} (days chanted)")
        appendLine("  Current streak    : ${stats.currentStreak} day(s)")
        appendLine("  Longest streak    : ${stats.longestStreak} day(s)")
        appendLine("  Last 7 days       : ${stats.last7.grouped()}")
        appendLine("  Last 30 days      : ${stats.last30.grouped()}")

        appendLine()
        if (log.isEmpty()) {
            appendLine("  Daily log         : no days recorded yet")
        } else {
            appendLine("  Daily log")
            appendLine("  DATE           JAPA        MALAS")
            log.forEach { day ->
                val malas = if (japa.malaSize > 0) day.count / japa.malaSize else 0
                appendLine(
                    "  ${day.date}  ${day.count.grouped().padStart(10)}  ${malas.grouped().padStart(10)}"
                )
            }
        }
    }

    private data class Stats(
        val total: Long,
        val activeDays: Int,
        val bestDay: DailyCount?,
        val averagePerActiveDay: Double,
        val currentStreak: Int,
        val longestStreak: Int,
        val last7: Long,
        val last30: Long,
    )

    private fun statsOf(log: List<DailyCount>): Stats {
        val counted = log.filter { it.count > 0 }
        val total = counted.sumOf { it.count }
        val dates = counted.map { it.date }.toSet()

        var longest = 0
        var running = 0
        var cursor: String? = null
        counted.map { it.date }.sorted().forEach { date ->
            running = if (cursor != null && date == dayAfter(cursor!!)) running + 1 else 1
            longest = maxOf(longest, running)
            cursor = date
        }

        // A streak that ended yesterday still counts today, right up until midnight passes twice.
        var current = 0
        var probe = today()
        if (probe !in dates) probe = dayBefore(probe)
        while (probe in dates) {
            current++
            probe = dayBefore(probe)
        }

        return Stats(
            total = total,
            activeDays = counted.size,
            bestDay = counted.maxByOrNull { it.count },
            averagePerActiveDay = if (counted.isEmpty()) 0.0 else total.toDouble() / counted.size,
            currentStreak = current,
            longestStreak = longest,
            last7 = sumSince(counted, 7),
            last30 = sumSince(counted, 30),
        )
    }

    private fun sumSince(log: List<DailyCount>, days: Int): Long {
        val cutoff = shiftDays(today(), -(days - 1))
        return log.filter { it.date >= cutoff }.sumOf { it.count }
    }

    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun shiftDays(date: String, delta: Int): String {
        val parsed = runCatching { dayFormat.parse(date) }.getOrNull() ?: return date
        val cal = Calendar.getInstance().apply {
            time = parsed
            add(Calendar.DAY_OF_YEAR, delta)
        }
        return dayFormat.format(cal.time)
    }

    private fun dayAfter(date: String) = shiftDays(date, 1)

    private fun dayBefore(date: String) = shiftDays(date, -1)

    /** ISO, so a restore can read back what the export wrote. */
    private fun isoDate(millis: Long): String = dayFormat.format(Date(millis))

    private fun formatDateTime(millis: Long): String =
        SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(millis))

    private fun Double.format1() = String.format(Locale.US, "%.1f", this)

    private fun Float.format2() = String.format(Locale.US, "%.2f", this)
}
