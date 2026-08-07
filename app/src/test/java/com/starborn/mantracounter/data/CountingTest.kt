package com.starborn.mantracounter.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CountingTest {

    @Test
    fun `counts never go below zero`() {
        assertEquals(0L, Counting.next(0, -1))
        assertEquals(0L, Counting.next(3, -500))
        assertEquals(4L, Counting.next(5, -1))
    }

    @Test
    fun `a mala is reached on its last bead and not before or after`() {
        assertNull(Counting.malaReached(106, 107, 108))
        assertEquals(1L, Counting.malaReached(107, 108, 108))
        assertNull(Counting.malaReached(108, 109, 108))
        assertEquals(2L, Counting.malaReached(215, 216, 108))
    }

    @Test
    fun `moving backwards never announces a mala`() {
        assertNull(Counting.malaReached(108, 107, 108))
        assertNull(Counting.malaReached(216, 108, 108))
        assertEquals(0L, Counting.malasCrossed(216, 108, 108))
    }

    @Test
    fun `a manual adjustment can cross several malas at once`() {
        assertEquals(2L, Counting.malasCrossed(100, 320, 108))
        // The mala number reported is the total completed, not the number of jumps.
        assertEquals(2L, Counting.malaReached(100, 320, 108))
    }

    @Test
    fun `a zero mala size cannot divide by zero`() {
        assertEquals(0L, Counting.malasCrossed(0, 500, 0))
        assertNull(Counting.malaReached(0, 500, 0))
    }

    @Test
    fun `the target fires once, on the bead that reaches it`() {
        assertFalse(Counting.reachedTarget(99_998, 99_999, 100_000))
        assertTrue(Counting.reachedTarget(99_999, 100_000, 100_000))
        assertFalse(Counting.reachedTarget(100_000, 100_001, 100_000))
        assertFalse(Counting.reachedTarget(50, 60, 0)) // no target set
    }

    /**
     * Regression guard. The counter screen used to compute each bead from a count captured when
     * the screen composed, so a hundred swipes all produced the same result and the number froze
     * at +1. Feeding each step from the previous result is the behaviour that must hold.
     */
    @Test
    fun `a run of single beads advances one at a time and closes exactly one mala`() {
        var count = 0L
        var malasAnnounced = 0
        repeat(108) {
            val next = Counting.next(count, 1)
            if (Counting.malaReached(count, next, 108) != null) malasAnnounced++
            count = next
        }
        assertEquals(108L, count)
        assertEquals(1, malasAnnounced)
    }

    @Test
    fun `swiping back up removes beads one at a time`() {
        var count = 20L
        repeat(5) { count = Counting.next(count, -1) }
        assertEquals(15L, count)
    }

    @Test
    fun `two full malas announce mala one then mala two`() {
        var count = 0L
        val announced = mutableListOf<Long>()
        repeat(216) {
            val next = Counting.next(count, 1)
            Counting.malaReached(count, next, 108)?.let(announced::add)
            count = next
        }
        assertEquals(listOf(1L, 2L), announced)
    }
}
