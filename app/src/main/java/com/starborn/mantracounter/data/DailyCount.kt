package com.starborn.mantracounter.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Beads counted for one japa on one day. [date] is `yyyy-MM-dd` in the device's local time —
 * a plain string so the daily log sorts, groups and exports without any date parsing.
 */
@Entity(tableName = "daily_counts", primaryKeys = ["japaId", "date"])
data class DailyCount(
    val japaId: Long,
    val date: String,
    val count: Long,
)

/** A day's figure summed across every japa. */
data class DayTotal(val date: String, val total: Long)

fun today(): String = dayKey(0)

fun yesterday(): String = dayKey(-1)

/**
 * The seven date keys of the week containing today, Monday first. Built by walking a calendar
 * back to Monday rather than by subtracting days from an epoch, so it survives daylight-saving
 * shifts and locales whose week starts elsewhere.
 */
fun currentWeekDays(): List<String> {
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val calendar = Calendar.getInstance()
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val backToMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
    calendar.add(Calendar.DAY_OF_YEAR, -backToMonday)
    return (0 until 7).map {
        val key = format.format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        key
    }
}

private fun dayKey(offsetDays: Int): String {
    val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offsetDays) }
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
}

@Dao
interface DailyCountDao {

    /**
     * Adds [delta] to today's row, creating it if this is the first bead of the day. Clamped at
     * zero so swiping back up past the start of a day cannot produce a negative daily total.
     */
    @Query(
        """
        INSERT INTO daily_counts (japaId, date, count) VALUES (:japaId, :date, MAX(0, :delta))
        ON CONFLICT(japaId, date) DO UPDATE SET count = MAX(0, count + :delta)
        """
    )
    suspend fun addToDay(japaId: Long, date: String, delta: Long)

    @Query("SELECT * FROM daily_counts WHERE japaId = :japaId ORDER BY date ASC")
    suspend fun forJapa(japaId: Long): List<DailyCount>

    @Query("SELECT * FROM daily_counts WHERE japaId = :japaId ORDER BY date DESC")
    fun observeForJapa(japaId: Long): kotlinx.coroutines.flow.Flow<List<DailyCount>>

    @Query("SELECT * FROM daily_counts WHERE japaId = :japaId AND date = :date")
    suspend fun day(japaId: Long, date: String): DailyCount?

    /** Every japa's figures for two given dates, for the today/yesterday line on the cards. */
    @Query("SELECT * FROM daily_counts WHERE date = :first OR date = :second")
    fun observeDates(first: String, second: String): Flow<List<DailyCount>>

    /** Sets a day to an exact figure, for the history editor. */
    @Query(
        """
        INSERT INTO daily_counts (japaId, date, count) VALUES (:japaId, :date, MAX(0, :count))
        ON CONFLICT(japaId, date) DO UPDATE SET count = MAX(0, :count)
        """
    )
    suspend fun setDay(japaId: Long, date: String, count: Long)

    @Query("DELETE FROM daily_counts WHERE japaId = :japaId AND date = :date")
    suspend fun deleteDay(japaId: Long, date: String)

    @Query("SELECT count FROM daily_counts WHERE japaId = :japaId AND date = :date")
    fun observeDay(japaId: Long, date: String): Flow<Long?>

    @Query("SELECT * FROM daily_counts ORDER BY japaId ASC, date ASC")
    suspend fun all(): List<DailyCount>

    /** Every japa's totals per day across a range, for the weekly chart. */
    @Query(
        """
        SELECT date, SUM(count) AS total FROM daily_counts
        WHERE date >= :start AND date <= :end
        GROUP BY date ORDER BY date ASC
        """
    )
    fun observeDailyTotals(start: String, end: String): Flow<List<DayTotal>>

    @Query("DELETE FROM daily_counts WHERE japaId = :japaId")
    suspend fun deleteForJapa(japaId: Long)
}
