package com.starborn.mantracounter.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One japa (mantra) the user is chanting.
 *
 * [count] is the lifetime bead count. Everything else the UI shows — malas completed,
 * beads into the current mala, progress toward the lifetime target — is derived from it
 * so there is only ever one number that can go out of sync.
 */
@Entity(tableName = "japas")
data class Japa(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    /** Whose mantra this is — Shiva, Krishna, Devi. Blank when not given. */
    @ColumnInfo(defaultValue = "''") val deity: String = "",
    val count: Long = 0,
    /** Beads in one mala. 108 traditionally; 1008 and 27 are also common. */
    val malaSize: Int = DEFAULT_MALA_SIZE,
    /** Lifetime goal in beads. 0 means no target set. */
    val lifetimeTarget: Long = 0,
    /** Absolute path to a copy of the user's chosen image, in app storage. */
    val backgroundUri: String? = null,
    val archived: Boolean = false,
    @ColumnInfo(defaultValue = "0") val favourite: Boolean = false,
    /** Milliseconds spent with this japa's counter open and the app in the foreground. */
    @ColumnInfo(defaultValue = "0") val totalTimeMs: Long = 0,
    val sortOrder: Int = 0,
    /** Index into [com.starborn.mantracounter.ui.theme.AccentPalette]. */
    @ColumnInfo(defaultValue = "0") val accentIndex: Int = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val archivedAt: Long? = null,
) {
    val malasCompleted: Long get() = if (malaSize > 0) count / malaSize else 0
    val beadsIntoMala: Long get() = if (malaSize > 0) count % malaSize else 0

    /** 0f..1f through the mala currently in progress. */
    val malaProgress: Float
        get() = if (malaSize > 0) beadsIntoMala.toFloat() / malaSize else 0f

    val hasTarget: Boolean get() = lifetimeTarget > 0

    /** 0f..1f toward the lifetime target, clamped. */
    val targetProgress: Float
        get() = if (hasTarget) (count.toFloat() / lifetimeTarget).coerceIn(0f, 1f) else 0f

    val targetRemaining: Long get() = (lifetimeTarget - count).coerceAtLeast(0)

    val targetReached: Boolean get() = hasTarget && count >= lifetimeTarget

    companion object {
        const val DEFAULT_MALA_SIZE = 108

        /**
         * True when landing on [count] closes a mala — the bead that gets the guru bead, the
         * tassel and the vibration. One definition, used by both the counter and the strand.
         */
        fun closesMala(count: Long, malaSize: Int): Boolean =
            malaSize > 0 && count > 0 && count % malaSize == 0L
    }
}
