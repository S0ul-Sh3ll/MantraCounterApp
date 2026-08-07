package com.starborn.mantracounter.data

/**
 * The counting rules, as pure functions.
 *
 * These deliberately take the current count as a parameter rather than reading it from anywhere.
 * An earlier version computed the next bead from a value captured when the screen composed, so
 * every swipe recomputed from the same stale base and the number on screen stuck after one bead.
 * Making the input explicit makes that mistake impossible to write, and testable.
 */
object Counting {

    /** Counts can never go below zero, however far back the strand is swiped. */
    fun next(current: Long, delta: Long): Long = (current + delta).coerceAtLeast(0)

    /**
     * How many mala boundaries are crossed moving up from [previous] to [next]. Normally 0 or 1,
     * but a manual adjustment can jump several at once.
     */
    fun malasCrossed(previous: Long, next: Long, malaSize: Int): Long =
        if (malaSize <= 0 || next <= previous) 0
        else (next / malaSize) - (previous / malaSize)

    /** The mala number just reached, or null if this move did not close one. */
    fun malaReached(previous: Long, next: Long, malaSize: Int): Long? =
        if (malasCrossed(previous, next, malaSize) > 0) next / malaSize else null

    fun reachedTarget(previous: Long, next: Long, target: Long): Boolean =
        target > 0 && previous < target && next >= target
}
