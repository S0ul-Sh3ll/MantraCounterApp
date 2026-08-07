package com.starborn.mantracounter.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The invariant behind the history editor: a japa's lifetime count always equals the sum of its
 * daily log. Every edit has to move both, or the headline figure quietly drifts from the history
 * that explains it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HistoryEditTest {

    private lateinit var db: JapaDatabase
    private lateinit var repository: JapaRepository

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, JapaDatabase::class.java).build()
        repository = JapaRepository(db.japaDao(), db.dailyCountDao())
    }

    @After
    fun tearDown() = db.close()

    private suspend fun newJapa(): Long = repository.create(Japa(name = "Gayatri", malaSize = 108))

    private suspend fun total(id: Long): Long = repository.get(id)?.count ?: -1

    private suspend fun daysSum(id: Long): Long = repository.dailyLog(id).sumOf { it.count }

    @Test
    fun `counting keeps the total equal to the sum of days`() = runBlocking {
        val id = newJapa()
        repository.commitBeads(id, 108, "2026-08-01")
        repository.commitBeads(id, 54, "2026-08-02")
        assertEquals(162L, total(id))
        assertEquals(total(id), daysSum(id))
    }

    @Test
    fun `editing a day moves the total by the difference`() = runBlocking {
        val id = newJapa()
        repository.commitBeads(id, 108, "2026-08-01")
        repository.commitBeads(id, 54, "2026-08-02")

        repository.setDayCount(id, "2026-08-01", 200)
        assertEquals(254L, total(id))
        assertEquals(total(id), daysSum(id))

        repository.setDayCount(id, "2026-08-01", 10)
        assertEquals(64L, total(id))
        assertEquals(total(id), daysSum(id))
    }

    @Test
    fun `adding a day that was counted off the app raises the total`() = runBlocking {
        val id = newJapa()
        repository.commitBeads(id, 108, "2026-08-01")
        repository.setDayCount(id, "2026-07-15", 216)
        assertEquals(324L, total(id))
        assertEquals(total(id), daysSum(id))
    }

    @Test
    fun `deleting a day removes exactly that day's beads`() = runBlocking {
        val id = newJapa()
        repository.commitBeads(id, 108, "2026-08-01")
        repository.commitBeads(id, 54, "2026-08-02")

        repository.deleteDay(id, "2026-08-01")
        assertEquals(54L, total(id))
        assertEquals(total(id), daysSum(id))
        assertNull(repository.dailyLog(id).firstOrNull { it.date == "2026-08-01" })
    }

    @Test
    fun `deleting a day that does not exist changes nothing`() = runBlocking {
        val id = newJapa()
        repository.commitBeads(id, 108, "2026-08-01")
        repository.deleteDay(id, "2020-01-01")
        assertEquals(108L, total(id))
    }

    @Test
    fun `moving a day to a free date keeps the total`() = runBlocking {
        val id = newJapa()
        repository.commitBeads(id, 108, "2026-08-01")

        repository.moveDay(id, "2026-08-01", "2026-07-31")
        assertEquals(108L, total(id))
        assertEquals(total(id), daysSum(id))
        assertEquals(listOf("2026-07-31"), repository.dailyLog(id).map { it.date })
    }

    @Test
    fun `moving a day onto an occupied date merges rather than overwrites`() = runBlocking {
        val id = newJapa()
        repository.commitBeads(id, 108, "2026-08-01")
        repository.commitBeads(id, 54, "2026-08-02")

        repository.moveDay(id, "2026-08-01", "2026-08-02")
        assertEquals(162L, total(id))
        assertEquals(total(id), daysSum(id))
        assertEquals(listOf("2026-08-02"), repository.dailyLog(id).map { it.date })
        assertEquals(162L, repository.dailyLog(id).single().count)
    }

    @Test
    fun `a session that runs past midnight logs against the day it started`() = runBlocking {
        val id = newJapa()
        // Started 2026-08-01 at 23:58 and carried on into the next date.
        repository.commitBeads(id, 40, "2026-08-01")
        repository.commitBeads(id, 68, "2026-08-01")

        val log = repository.dailyLog(id)
        assertEquals(listOf("2026-08-01"), log.map { it.date })
        assertEquals(108L, log.single().count)
    }

    @Test
    fun `a day cannot be edited below zero`() = runBlocking {
        val id = newJapa()
        repository.commitBeads(id, 50, "2026-08-01")
        repository.setDayCount(id, "2026-08-01", -20)
        assertEquals(0L, repository.dailyLog(id).single().count)
        assertEquals(0L, total(id))
    }
}
