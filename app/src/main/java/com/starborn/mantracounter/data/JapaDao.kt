package com.starborn.mantracounter.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JapaDao {

    @Query(
        """
        SELECT * FROM japas
        WHERE archived = 0 AND name LIKE '%' || :query || '%'
        ORDER BY sortOrder ASC, id ASC
        """
    )
    fun observeActive(query: String): Flow<List<Japa>>

    @Query(
        """
        SELECT * FROM japas
        WHERE archived = 1 AND name LIKE '%' || :query || '%'
        ORDER BY archivedAt DESC, id DESC
        """
    )
    fun observeArchived(query: String): Flow<List<Japa>>

    @Query("SELECT * FROM japas WHERE id = :id")
    fun observeById(id: Long): Flow<Japa?>

    @Query("SELECT * FROM japas WHERE id = :id")
    suspend fun getById(id: Long): Japa?

    @Query("SELECT * FROM japas ORDER BY archived ASC, sortOrder ASC, id ASC")
    suspend fun allForExport(): List<Japa>

    @Query("SELECT * FROM japas ORDER BY archived ASC, sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<Japa>>

    @Query("UPDATE japas SET totalTimeMs = totalTimeMs + :millis WHERE id = :id")
    suspend fun addTime(id: Long, millis: Long)

    @Query("SELECT COUNT(*) FROM japas WHERE archived = 1")
    fun observeArchivedCount(): Flow<Int>

    @Insert
    suspend fun insert(japa: Japa): Long

    @Update
    suspend fun update(japa: Japa)

    @Delete
    suspend fun delete(japa: Japa)

    /** Clamped at zero so the undo button can never drive a count negative. */
    @Query("UPDATE japas SET count = MAX(0, count + :delta), updatedAt = :now WHERE id = :id")
    suspend fun addToCount(id: Long, delta: Long, now: Long)

    @Query(
        "UPDATE japas SET archived = :archived, archivedAt = :at, updatedAt = :now WHERE id = :id"
    )
    suspend fun setArchived(id: Long, archived: Boolean, at: Long?, now: Long)

    @Query("UPDATE japas SET favourite = :favourite, updatedAt = :now WHERE id = :id")
    suspend fun setFavourite(id: Long, favourite: Boolean, now: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM japas")
    suspend fun maxSortOrder(): Int
}
