package com.starborn.mantracounter.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The whole UI is derived from [Japa.count], so these are the numbers that must not drift.
 */
class JapaTest {

    private fun japa(count: Long, malaSize: Int = 108, target: Long = 0) =
        Japa(id = 1, name = "test", count = count, malaSize = malaSize, lifetimeTarget = target)

    @Test
    fun `fresh japa has nothing completed`() {
        val j = japa(0)
        assertEquals(0L, j.malasCompleted)
        assertEquals(0L, j.beadsIntoMala)
        assertEquals(0f, j.malaProgress, 0.0001f)
    }

    @Test
    fun `mala completes exactly on the last bead`() {
        assertEquals(0L, japa(107).malasCompleted)
        assertEquals(107L, japa(107).beadsIntoMala)

        assertEquals(1L, japa(108).malasCompleted)
        assertEquals(0L, japa(108).beadsIntoMala)

        assertEquals(1L, japa(109).malasCompleted)
        assertEquals(1L, japa(109).beadsIntoMala)
    }

    @Test
    fun `custom mala size is honoured`() {
        val j = japa(count = 2016, malaSize = 1008)
        assertEquals(2L, j.malasCompleted)
        assertEquals(0L, j.beadsIntoMala)
    }

    @Test
    fun `no target means no progress and no completion`() {
        val j = japa(5000)
        assertFalse(j.hasTarget)
        assertEquals(0f, j.targetProgress, 0.0001f)
        assertFalse(j.targetReached)
        assertEquals(0L, j.targetRemaining)
    }

    @Test
    fun `target progress is a clamped fraction`() {
        assertEquals(0.5f, japa(50_000, target = 100_000).targetProgress, 0.0001f)
        // Overshooting a target must not push the progress bar past full.
        assertEquals(1f, japa(250_000, target = 100_000).targetProgress, 0.0001f)
    }

    @Test
    fun `target remaining never goes negative`() {
        assertEquals(40_000L, japa(60_000, target = 100_000).targetRemaining)
        assertEquals(0L, japa(250_000, target = 100_000).targetRemaining)
    }

    @Test
    fun `target reached at exactly the target`() {
        assertFalse(japa(99_999, target = 100_000).targetReached)
        assertTrue(japa(100_000, target = 100_000).targetReached)
        assertTrue(japa(100_001, target = 100_000).targetReached)
    }

    @Test
    fun `a zero mala size cannot divide by zero`() {
        val j = japa(count = 500, malaSize = 0)
        assertEquals(0L, j.malasCompleted)
        assertEquals(0L, j.beadsIntoMala)
        assertEquals(0f, j.malaProgress, 0.0001f)
    }
}
