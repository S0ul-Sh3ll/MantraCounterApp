package com.starborn.mantracounter.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerRunTest {

    @Test
    fun `a session with nothing chosen runs for exactly one mala`() {
        val run = TimerRun.start(currentCount = 0, endMalas = 0, malaSize = 108)
        assertEquals(108L, run.total)
        assertEquals(108L, run.target)
        assertFalse(run.isComplete(107))
        assertTrue(run.isComplete(108))
    }

    @Test
    fun `a negative or nonsense length still gives one mala rather than running forever`() {
        assertEquals(54L, TimerRun.start(0, -5, 54).total)
        assertEquals(1L, TimerRun.start(0, 1, 0).total)
        assertEquals(1L, TimerRun.start(0, 0, -3).total)
    }

    @Test
    fun `a session starts from wherever the japa already is`() {
        val run = TimerRun.start(currentCount = 5_000, endMalas = 1, malaSize = 108)
        assertEquals(5_000L, run.anchor)
        assertEquals(5_108L, run.target)
        assertEquals(0L, run.counted(5_000))
        assertEquals(40L, run.counted(5_040))
    }

    /**
     * The reported bug: a session of 11, paused at 5, went on to count a further 11. The finish
     * line has to be the same object before and after a pause.
     */
    @Test
    fun `pausing and resuming does not move the finish line`() {
        val run = TimerRun.start(currentCount = 0, endMalas = 1, malaSize = 11)
        var count = 0L

        repeat(5) { count++ }            // counted to 5, then paused
        assertEquals(5L, run.counted(count))
        assertFalse(run.isComplete(count))

        // Resume: the same run, not a new one.
        while (!run.isComplete(count)) count++

        assertEquals(11L, count)
        assertEquals(11L, run.counted(count))
    }

    @Test
    fun `a resumed session is complete after exactly the chosen number of malas`() {
        val run = TimerRun.start(currentCount = 240, endMalas = 3, malaSize = 27)
        assertEquals(81L, run.total)
        assertEquals(321L, run.target)

        var count = 240L
        var ticks = 0
        while (!run.isComplete(count)) {
            count++
            ticks++
        }
        assertEquals(81, ticks)
    }

    @Test
    fun `counted and remaining stay inside the session`() {
        val run = TimerRun.start(currentCount = 100, endMalas = 1, malaSize = 10)
        assertEquals(0L, run.counted(100))
        assertEquals(10L, run.remaining(100))
        assertEquals(0L, run.remaining(110))
        // Counting past the target (a stray tap) does not report more than the session held.
        assertEquals(10L, run.counted(130))
        assertEquals(0L, run.remaining(130))
    }

    @Test
    fun `progress runs zero to one and stops there`() {
        val run = TimerRun.start(currentCount = 0, endMalas = 1, malaSize = 100)
        assertEquals(0f, run.progress(0), 0.0001f)
        assertEquals(0.5f, run.progress(50), 0.0001f)
        assertEquals(1f, run.progress(100), 0.0001f)
        assertEquals(1f, run.progress(400), 0.0001f)
    }

    @Test
    fun `a multi-mala session does not stop at the first mala`() {
        val run = TimerRun.start(currentCount = 0, endMalas = 5, malaSize = 108)
        assertFalse(run.isComplete(108))
        assertFalse(run.isComplete(431))
        assertTrue(run.isComplete(540))
    }
}
