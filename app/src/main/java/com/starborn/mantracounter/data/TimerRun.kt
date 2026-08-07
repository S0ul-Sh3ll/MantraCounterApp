package com.starborn.mantracounter.data

/**
 * One timed session, held as two absolute counts rather than a running tally.
 *
 * The first version kept a mutable "counted so far" number and stopped when it reached the
 * target. Pausing and resuming re-entered that loop with the tally intact but the goal
 * recomputed, so a session paused at 5 of 11 went on to count a further 11. Anchoring both ends
 * to the japa's real count at the moment Start was pressed removes the possibility: the finish
 * line does not move, whatever happens in between.
 */
data class TimerRun(val anchor: Long, val target: Long) {

    /** How many japa this session is for in total. */
    val total: Long get() = (target - anchor).coerceAtLeast(0)

    fun counted(current: Long): Long = (current - anchor).coerceIn(0, total)

    fun remaining(current: Long): Long = (target - current).coerceAtLeast(0)

    fun isComplete(current: Long): Boolean = current >= target

    fun progress(current: Long): Float =
        if (total <= 0) 1f else (counted(current).toFloat() / total).coerceIn(0f, 1f)

    companion object {
        /** A session with nothing chosen runs for one mala. */
        const val DEFAULT_END_MALAS = 1

        fun start(currentCount: Long, endMalas: Int, malaSize: Int): TimerRun {
            val malas = endMalas.coerceAtLeast(DEFAULT_END_MALAS)
            val size = malaSize.coerceAtLeast(1)
            return TimerRun(anchor = currentCount, target = currentCount + malas.toLong() * size)
        }
    }
}
