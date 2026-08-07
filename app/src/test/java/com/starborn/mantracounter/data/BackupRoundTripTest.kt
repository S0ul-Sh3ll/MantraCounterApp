package com.starborn.mantracounter.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Export then import has to give back what went in. This is the test that keeps [BackupWriter]
 * and [BackupReader] honest about each other — change one side's wording and this fails.
 */
class BackupRoundTripTest {

    private fun entry(
        name: String,
        count: Long,
        malaSize: Int = 108,
        target: Long = 0,
        archived: Boolean = false,
        favourite: Boolean = false,
        accent: Int = 0,
        deity: String = "",
        timeMs: Long = 0,
        log: List<DailyCount> = emptyList(),
    ) = JapaWithLog(
        japa = Japa(
            id = 7,
            name = name,
            deity = deity,
            count = count,
            malaSize = malaSize,
            lifetimeTarget = target,
            backgroundUri = "/data/should-not-survive.jpg",
            archived = archived,
            favourite = favourite,
            accentIndex = accent,
            totalTimeMs = timeMs,
            createdAt = 1_754_000_000_000,
            updatedAt = 1_754_400_000_000,
            archivedAt = if (archived) 1_754_400_000_000 else null,
        ),
        log = log,
    )

    private val snapshot = listOf(
        entry(
            name = "Hare Krishna Maha Mantra",
            count = 129_600,
            malaSize = 108,
            target = 1_000_000,
            favourite = true,
            accent = 3,
            deity = "Krishna",
            timeMs = 8_040_000,
            log = listOf(
                DailyCount(7, "2026-08-01", 1_080),
                DailyCount(7, "2026-08-02", 216),
                DailyCount(7, "2026-08-05", 108),
            ),
        ),
        entry(name = "Gayatri Mantra", count = 5_400, malaSize = 27, accent = 1),
        entry(
            name = "Om Namah Shivaya",
            count = 900,
            archived = true,
            log = listOf(DailyCount(7, "2026-07-20", 900)),
        ),
    )

    private fun roundTrip(): List<JapaWithLog> =
        BackupReader.parse(BackupWriter.render(snapshot, generatedAt = 1_754_400_000_000))

    @Test
    fun `every japa comes back, in the same order`() {
        val restored = roundTrip()
        assertEquals(3, restored.size)
        assertEquals(
            listOf("Hare Krishna Maha Mantra", "Gayatri Mantra", "Om Namah Shivaya"),
            restored.map { it.japa.name },
        )
    }

    @Test
    fun `counts, mala sizes and targets survive the trip`() {
        val restored = roundTrip()
        val first = restored[0].japa
        assertEquals(129_600L, first.count)
        assertEquals(108, first.malaSize)
        assertEquals(1_000_000L, first.lifetimeTarget)

        val second = restored[1].japa
        assertEquals(5_400L, second.count)
        assertEquals(27, second.malaSize)
        // No target set stays no target set, rather than becoming a zero-ish one.
        assertEquals(0L, second.lifetimeTarget)
    }

    @Test
    fun `favourites and accent colours survive the trip`() {
        val restored = roundTrip()
        assertEquals(true, restored[0].japa.favourite)
        assertEquals(3, restored[0].japa.accentIndex)
        assertEquals(false, restored[1].japa.favourite)
        assertEquals(1, restored[1].japa.accentIndex)
    }

    @Test
    fun `archived japas come back archived and active ones do not`() {
        val restored = roundTrip()
        assertEquals(false, restored[0].japa.archived)
        assertEquals(false, restored[1].japa.archived)
        assertEquals(true, restored[2].japa.archived)
    }

    @Test
    fun `the daily log survives date for date`() {
        val restored = roundTrip()
        assertEquals(
            listOf("2026-08-01" to 1_080L, "2026-08-02" to 216L, "2026-08-05" to 108L),
            restored[0].log.map { it.date to it.count },
        )
        assertEquals(emptyList<Pair<String, Long>>(), restored[1].log.map { it.date to it.count })
        assertEquals(listOf("2026-07-20" to 900L), restored[2].log.map { it.date to it.count })
    }

    @Test
    fun `a restored japa's total still equals the sum of its logged days`() {
        val restored = roundTrip()[2]
        assertEquals(restored.japa.count, restored.log.sumOf { it.count })
    }

    @Test
    fun `the deity survives the trip, and a blank one stays blank`() {
        assertEquals("Krishna", roundTrip()[0].japa.deity)
        assertEquals("", roundTrip()[1].japa.deity)
    }

    @Test
    fun `time spent survives the trip`() {
        assertEquals(8_040_000L, roundTrip()[0].japa.totalTimeMs)
        assertEquals(0L, roundTrip()[1].japa.totalTimeMs)
    }

    @Test
    fun `background images are not carried by a text backup`() {
        roundTrip().forEach { assertNull(it.japa.backgroundUri) }
    }

    @Test
    fun `created dates come back as the same day`() {
        val restored = roundTrip()[0].japa
        // Millisecond precision is not in the file; the day is.
        assertTrue(restored.createdAt > 0)
        assertEquals(
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .format(java.util.Date(1_754_000_000_000)),
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .format(java.util.Date(restored.createdAt)),
        )
    }

    @Test
    fun `a file that is not a backup yields nothing rather than nonsense`() {
        assertEquals(emptyList<JapaWithLog>(), BackupReader.parse("shopping list\nmilk\neggs\n"))
        assertEquals(emptyList<JapaWithLog>(), BackupReader.parse(""))
    }

    @Test
    fun `an empty backup round-trips to an empty list`() {
        val text = BackupWriter.render(emptyList(), generatedAt = 0)
        assertEquals(emptyList<JapaWithLog>(), BackupReader.parse(text))
    }
}
