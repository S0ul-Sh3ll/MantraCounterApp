package com.starborn.mantracounter.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BackupWriterTest {

    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun daysAgo(n: Int): String {
        val cal = Calendar.getInstance().apply {
            time = Date()
            add(Calendar.DAY_OF_YEAR, -n)
        }
        return dayFormat.format(cal.time)
    }

    private fun entry(
        name: String = "Hare Krishna Maha Mantra",
        count: Long = 1_080,
        target: Long = 0,
        archived: Boolean = false,
        log: List<DailyCount> = emptyList(),
    ) = JapaWithLog(
        japa = Japa(
            id = 1,
            name = name,
            count = count,
            malaSize = 108,
            lifetimeTarget = target,
            archived = archived,
            createdAt = 1_700_000_000_000,
            updatedAt = 1_700_000_000_000,
        ),
        log = log,
    )

    @Test
    fun `backup names every japa and separates active from archived`() {
        val text = BackupWriter.render(
            listOf(
                entry(name = "Gayatri Mantra"),
                entry(name = "Om Namah Shivaya", archived = true),
            ),
            generatedAt = 1_700_000_000_000,
        )
        assertTrue(text.contains("MANTRA COUNTER — BACKUP"))
        assertTrue(text.contains("ACTIVE JAPAS"))
        assertTrue(text.contains("ARCHIVED JAPAS"))
        assertTrue(text.contains("Gayatri Mantra"))
        assertTrue(text.contains("Om Namah Shivaya"))
    }

    @Test
    fun `a japa with no days recorded still exports`() {
        val text = BackupWriter.render(listOf(entry()), generatedAt = 0)
        assertTrue(text.contains("no days recorded yet"))
        assertFalse(text.contains("DATE           JAPA"))
    }

    @Test
    fun `daily log lines carry both beads and malas for the day`() {
        val text = BackupWriter.render(
            listOf(entry(log = listOf(DailyCount(1, "2026-08-01", 216)))),
            generatedAt = 0,
        )
        assertTrue(text.contains("Daily log"))
        assertTrue(text.contains("2026-08-01"))
        // 216 beads at 108 per mala is exactly 2 malas.
        assertTrue(text.lines().any { it.contains("2026-08-01") && it.trimEnd().endsWith("2") })
    }

    @Test
    fun `current streak counts back from today through consecutive days`() {
        val text = BackupWriter.render(
            listOf(
                entry(
                    log = listOf(
                        DailyCount(1, daysAgo(4), 108),
                        // gap on day 3
                        DailyCount(1, daysAgo(2), 108),
                        DailyCount(1, daysAgo(1), 108),
                        DailyCount(1, daysAgo(0), 108),
                    )
                )
            ),
            generatedAt = 0,
        )
        assertTrue(text.contains("Current streak    : 3 day(s)"))
        assertTrue(text.contains("Longest streak    : 3 day(s)"))
    }

    @Test
    fun `a streak that ran up to yesterday is still current`() {
        val text = BackupWriter.render(
            listOf(
                entry(
                    log = listOf(
                        DailyCount(1, daysAgo(2), 50),
                        DailyCount(1, daysAgo(1), 50),
                    )
                )
            ),
            generatedAt = 0,
        )
        assertTrue(text.contains("Current streak    : 2 day(s)"))
    }

    @Test
    fun `a lapsed streak reads as zero`() {
        val text = BackupWriter.render(
            listOf(entry(log = listOf(DailyCount(1, daysAgo(9), 108)))),
            generatedAt = 0,
        )
        assertTrue(text.contains("Current streak    : 0 day(s)"))
        assertTrue(text.contains("Longest streak    : 1 day(s)"))
    }

    @Test
    fun `rolling windows only include days inside them`() {
        val text = BackupWriter.render(
            listOf(
                entry(
                    log = listOf(
                        DailyCount(1, daysAgo(40), 1_000),
                        DailyCount(1, daysAgo(10), 200),
                        DailyCount(1, daysAgo(1), 30),
                    )
                )
            ),
            generatedAt = 0,
        )
        assertTrue(text.contains("Last 7 days       : 30"))
        assertTrue(text.contains("Last 30 days      : 230"))
    }

    @Test
    fun `target progress and remaining are reported when a target is set`() {
        val text = BackupWriter.render(
            listOf(entry(count = 25_000, target = 100_000)),
            generatedAt = 0,
        )
        assertTrue(text.contains("Target progress   : 25.00%"))
        assertTrue(text.contains("Remaining"))
    }

    @Test
    fun `no target says so rather than printing a bogus percentage`() {
        val text = BackupWriter.render(listOf(entry(target = 0)), generatedAt = 0)
        assertTrue(text.contains("Lifetime target   : none set"))
        assertFalse(text.contains("Target progress"))
    }
}
