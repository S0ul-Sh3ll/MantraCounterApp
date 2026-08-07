package com.starborn.mantracounter.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class WeekDaysTest {

    private val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    @Test
    fun `a week is seven distinct days`() {
        val week = currentWeekDays()
        assertEquals(7, week.size)
        assertEquals(7, week.toSet().size)
    }

    @Test
    fun `the week starts on a Monday`() {
        val first = Calendar.getInstance().apply { time = format.parse(currentWeekDays().first())!! }
        assertEquals(Calendar.MONDAY, first.get(Calendar.DAY_OF_WEEK))
    }

    @Test
    fun `the days are consecutive`() {
        val times = currentWeekDays().map { format.parse(it)!!.time }
        times.zipWithNext { a, b ->
            // Compared as whole days, so a daylight-saving change cannot fail this.
            val gap = TimeUnit.MILLISECONDS.toDays(b - a + TimeUnit.HOURS.toMillis(2))
            assertEquals(1L, gap)
        }
    }

    @Test
    fun `today is somewhere in this week`() {
        assertTrue(today() in currentWeekDays())
    }

    @Test
    fun `the week ends on a Sunday, after today`() {
        val week = currentWeekDays()
        val last = Calendar.getInstance().apply { time = format.parse(week.last())!! }
        assertEquals(Calendar.SUNDAY, last.get(Calendar.DAY_OF_WEEK))
        assertTrue(week.last() >= today())
        assertTrue(week.first() <= today())
    }

    @Test
    fun `the week contains no future beyond Sunday`() {
        val week = currentWeekDays()
        val todayMs = format.parse(today())!!.time
        val endMs = format.parse(week.last())!!.time
        assertTrue(endMs - todayMs <= TimeUnit.DAYS.toMillis(6) + TimeUnit.HOURS.toMillis(2))
        assertTrue(format.parse(week.first())!!.time <= Date().time)
    }
}
