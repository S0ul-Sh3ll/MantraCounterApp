package com.starborn.mantracounter.data

import kotlinx.coroutines.flow.Flow

class JapaRepository(
    private val dao: JapaDao,
    private val dailyDao: DailyCountDao,
) {

    fun active(query: String): Flow<List<Japa>> = dao.observeActive(query.trim())

    fun archived(query: String): Flow<List<Japa>> = dao.observeArchived(query.trim())

    fun japa(id: Long): Flow<Japa?> = dao.observeById(id)

    fun archivedCount(): Flow<Int> = dao.observeArchivedCount()

    fun dayCount(japaId: Long, date: String): Flow<Long?> = dailyDao.observeDay(japaId, date)

    fun history(japaId: Long): Flow<List<DailyCount>> = dailyDao.observeForJapa(japaId)

    fun dates(first: String, second: String): Flow<List<DailyCount>> =
        dailyDao.observeDates(first, second)

    fun dailyTotals(start: String, end: String): Flow<List<DayTotal>> =
        dailyDao.observeDailyTotals(start, end)

    suspend fun create(japa: Japa): Long {
        val now = System.currentTimeMillis()
        return dao.insert(
            japa.copy(sortOrder = dao.maxSortOrder() + 1, createdAt = now, updatedAt = now)
        )
    }

    suspend fun save(japa: Japa) = dao.update(japa.copy(updatedAt = System.currentTimeMillis()))

    /**
     * The single write path for counting. The lifetime total and the daily log move together, so
     * they cannot disagree — including on the way back down when beads are swiped off.
     */
    suspend fun commitBeads(japaId: Long, delta: Long, date: String = today()) {
        if (delta == 0L) return
        dao.addToCount(japaId, delta, System.currentTimeMillis())
        dailyDao.addToDay(japaId, date, delta)
    }

    fun allJapas(): Flow<List<Japa>> = dao.observeAll()

    /** Accrues time spent with a japa's counter open. */
    suspend fun addTime(japaId: Long, millis: Long) {
        if (millis <= 0) return
        dao.addTime(japaId, millis)
    }

    suspend fun setFavourite(id: Long, favourite: Boolean) =
        dao.setFavourite(id, favourite, System.currentTimeMillis())

    /**
     * History editing. Every one of these moves the lifetime total by the same amount as the day
     * it changes, so the headline count always equals the sum of its days.
     */
    suspend fun setDayCount(japaId: Long, date: String, count: Long) {
        val previous = dailyDao.day(japaId, date)?.count ?: 0
        val target = count.coerceAtLeast(0)
        dailyDao.setDay(japaId, date, target)
        dao.addToCount(japaId, target - previous, System.currentTimeMillis())
    }

    suspend fun deleteDay(japaId: Long, date: String) {
        val previous = dailyDao.day(japaId, date)?.count ?: return
        dailyDao.deleteDay(japaId, date)
        dao.addToCount(japaId, -previous, System.currentTimeMillis())
    }

    /** Moves a day's beads to another date, merging if that date already has an entry. */
    suspend fun moveDay(japaId: Long, from: String, to: String) {
        if (from == to) return
        val moving = dailyDao.day(japaId, from)?.count ?: return
        dailyDao.deleteDay(japaId, from)
        dailyDao.addToDay(japaId, to, moving)
    }

    suspend fun setArchived(id: Long, archived: Boolean) {
        val now = System.currentTimeMillis()
        dao.setArchived(id, archived, if (archived) now else null, now)
    }

    suspend fun delete(japa: Japa) {
        dailyDao.deleteForJapa(japa.id)
        dao.delete(japa)
        BackgroundStore.remove(japa.backgroundUri)
    }

    suspend fun get(id: Long): Japa? = dao.getById(id)

    /** Everything needed for a backup, read in one go. */
    suspend fun snapshot(): List<JapaWithLog> {
        val japas = dao.allForExport()
        val logs = dailyDao.all().groupBy { it.japaId }
        return japas.map { JapaWithLog(it, logs[it.id].orEmpty()) }
    }

    suspend fun dailyLog(japaId: Long): List<DailyCount> = dailyDao.forJapa(japaId)

    /**
     * Recreates japas from a parsed backup. A japa whose name already exists is left alone
     * rather than merged or overwritten — a restore should never be able to damage what is
     * already on the phone. Background images are not in the file and are not restored.
     */
    suspend fun restore(entries: List<JapaWithLog>): RestoreResult {
        val existing = dao.allForExport().map { it.name.trim().lowercase() }.toSet()
        var added = 0
        var skipped = 0
        var days = 0

        entries.forEach { entry ->
            if (entry.japa.name.trim().lowercase() in existing) {
                skipped++
                return@forEach
            }
            val id = dao.insert(
                entry.japa.copy(
                    id = 0,
                    backgroundUri = null,
                    sortOrder = dao.maxSortOrder() + 1,
                )
            )
            entry.log.forEach { day ->
                dailyDao.setDay(id, day.date, day.count)
                days++
            }
            added++
        }
        return RestoreResult(added = added, skipped = skipped, days = days)
    }
}

data class JapaWithLog(val japa: Japa, val log: List<DailyCount>)
