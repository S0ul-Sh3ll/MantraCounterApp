package com.starborn.mantracounter.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class DayKeyTest {

    private val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    @Test
    fun `both keys are well-formed dates`() {
        assertTrue(today().matches(Regex("""\d{4}-\d{2}-\d{2}""")))
        assertTrue(yesterday().matches(Regex("""\d{4}-\d{2}-\d{2}""")))
    }

    @Test
    fun `yesterday is exactly one day before today`() {
        val todayMs = format.parse(today())!!.time
        val yesterdayMs = format.parse(yesterday())!!.time
        assertEquals(TimeUnit.DAYS.toMillis(1), todayMs - yesterdayMs)
    }

    @Test
    fun `yesterday sorts before today as a plain string`() {
        // The daily log is ordered and compared as text, so this ordering has to hold.
        assertTrue(yesterday() < today())
    }
}
