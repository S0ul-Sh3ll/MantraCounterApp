package com.starborn.mantracounter.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Version 1 shipped. Anyone updating has counts in the old schema, so the 1→2 migration has to
 * both satisfy Room's schema validation and leave those counts untouched — neither of which the
 * compiler can check.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    /** The japas table exactly as version 1 created it. */
    private val v1JapasTable = """
        CREATE TABLE IF NOT EXISTS `japas` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `name` TEXT NOT NULL,
            `count` INTEGER NOT NULL,
            `malaSize` INTEGER NOT NULL,
            `lifetimeTarget` INTEGER NOT NULL,
            `backgroundUri` TEXT,
            `archived` INTEGER NOT NULL,
            `sortOrder` INTEGER NOT NULL,
            `accentIndex` INTEGER NOT NULL DEFAULT 0,
            `createdAt` INTEGER NOT NULL,
            `updatedAt` INTEGER NOT NULL,
            `archivedAt` INTEGER
        )
    """.trimIndent()

    private fun createVersion1Database(name: String) {
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        file.delete()
        val raw = SQLiteDatabase.openOrCreateDatabase(file, null)
        raw.execSQL(v1JapasTable)
        raw.execSQL(
            """
            INSERT INTO japas
              (name, count, malaSize, lifetimeTarget, backgroundUri, archived, sortOrder,
               accentIndex, createdAt, updatedAt, archivedAt)
            VALUES ('Hare Krishna Maha Mantra', 4321, 108, 100000, '/data/bg.jpg', 0, 1, 2,
                    1700000000000, 1700000000000, NULL)
            """.trimIndent()
        )
        raw.execSQL(
            """
            INSERT INTO japas
              (name, count, malaSize, lifetimeTarget, backgroundUri, archived, sortOrder,
               accentIndex, createdAt, updatedAt, archivedAt)
            VALUES ('Om Namah Shivaya', 900, 108, 0, NULL, 1, 2, 3,
                    1700000000000, 1700000000000, 1700000500000)
            """.trimIndent()
        )
        raw.version = 1
        raw.close()
    }

    @Test
    fun `existing japas survive the upgrade to the daily log`() {
        val name = "migration-test.db"
        createVersion1Database(name)

        // Opening through the real database class runs the migration and then Room's own schema
        // validation — which is what would crash on a user's phone if the DDL were wrong.
        val db = Room.databaseBuilder(context, JapaDatabase::class.java, name)
            .addMigrations(
                JapaDatabase.MIGRATION_1_2,
                JapaDatabase.MIGRATION_2_3,
                JapaDatabase.MIGRATION_3_4,
                JapaDatabase.MIGRATION_4_5,
            )
            .build()

        runBlocking {
            val japas = db.japaDao().allForExport()
            assertEquals(2, japas.size)

            val active = japas.first { !it.archived }
            assertEquals("Hare Krishna Maha Mantra", active.name)
            assertEquals(4321L, active.count)
            assertEquals(108, active.malaSize)
            assertEquals(100_000L, active.lifetimeTarget)
            assertEquals("/data/bg.jpg", active.backgroundUri)
            assertEquals(2, active.accentIndex)

            val archived = japas.first { it.archived }
            assertEquals("Om Namah Shivaya", archived.name)
            assertEquals(900L, archived.count)
            assertEquals(1_700_000_500_000L, archived.archivedAt)

            // The new table exists and is usable immediately after the upgrade.
            assertEquals(emptyList<DailyCount>(), db.dailyCountDao().forJapa(active.id))
            db.dailyCountDao().addToDay(active.id, "2026-08-06", 108)
            assertEquals(108L, db.dailyCountDao().forJapa(active.id).single().count)
        }

        db.close()
        context.deleteDatabase(name)
    }

    /** The version 2 schema: japas as in v1, plus the daily log. */
    private fun createVersion2Database(name: String) {
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        file.delete()
        val raw = SQLiteDatabase.openOrCreateDatabase(file, null)
        raw.execSQL(v1JapasTable)
        raw.execSQL(
            """
            CREATE TABLE IF NOT EXISTS daily_counts (
                japaId INTEGER NOT NULL,
                date TEXT NOT NULL,
                count INTEGER NOT NULL,
                PRIMARY KEY(japaId, date)
            )
            """.trimIndent()
        )
        raw.execSQL(
            """
            INSERT INTO japas
              (name, count, malaSize, lifetimeTarget, backgroundUri, archived, sortOrder,
               accentIndex, createdAt, updatedAt, archivedAt)
            VALUES ('Gayatri Mantra', 5400, 108, 0, NULL, 0, 1, 1,
                    1700000000000, 1700000000000, NULL)
            """.trimIndent()
        )
        raw.execSQL("INSERT INTO daily_counts (japaId, date, count) VALUES (1, '2026-08-01', 216)")
        raw.version = 2
        raw.close()
    }

    @Test
    fun `upgrading from version 2 adds favourites without touching counts or the daily log`() {
        val name = "migration-v2-test.db"
        createVersion2Database(name)

        val db = Room.databaseBuilder(context, JapaDatabase::class.java, name)
            .addMigrations(
                JapaDatabase.MIGRATION_1_2,
                JapaDatabase.MIGRATION_2_3,
                JapaDatabase.MIGRATION_3_4,
                JapaDatabase.MIGRATION_4_5,
            )
            .build()

        runBlocking {
            val japa = db.japaDao().allForExport().single()
            assertEquals("Gayatri Mantra", japa.name)
            assertEquals(5400L, japa.count)
            // Existing japas are not favourites until the star is pressed.
            assertEquals(false, japa.favourite)

            val log = db.dailyCountDao().forJapa(japa.id)
            assertEquals(1, log.size)
            assertEquals(216L, log.single().count)

            db.japaDao().setFavourite(japa.id, true, 0)
            assertEquals(true, db.japaDao().getById(japa.id)?.favourite)
        }

        db.close()
        context.deleteDatabase(name)
    }

    @Test
    fun `upgrading from version 3 adds time tracking starting at zero`() {
        val name = "migration-v3-test.db"
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        file.delete()
        val raw = SQLiteDatabase.openOrCreateDatabase(file, null)
        // Version 3 is version 1's japas table plus favourite, plus the daily log.
        raw.execSQL(v1JapasTable)
        raw.execSQL("ALTER TABLE japas ADD COLUMN favourite INTEGER NOT NULL DEFAULT 0")
        raw.execSQL(
            """
            CREATE TABLE IF NOT EXISTS daily_counts (
                japaId INTEGER NOT NULL,
                date TEXT NOT NULL,
                count INTEGER NOT NULL,
                PRIMARY KEY(japaId, date)
            )
            """.trimIndent()
        )
        raw.execSQL(
            """
            INSERT INTO japas
              (name, count, malaSize, lifetimeTarget, backgroundUri, archived, sortOrder,
               accentIndex, createdAt, updatedAt, archivedAt, favourite)
            VALUES ('Maha Mantra', 12345, 108, 0, NULL, 0, 1, 0,
                    1700000000000, 1700000000000, NULL, 1)
            """.trimIndent()
        )
        raw.version = 3
        raw.close()

        val db = Room.databaseBuilder(context, JapaDatabase::class.java, name)
            .addMigrations(
                JapaDatabase.MIGRATION_1_2,
                JapaDatabase.MIGRATION_2_3,
                JapaDatabase.MIGRATION_3_4,
                JapaDatabase.MIGRATION_4_5,
            )
            .build()

        runBlocking {
            val japa = db.japaDao().allForExport().single()
            assertEquals(12345L, japa.count)
            assertEquals(true, japa.favourite)
            assertEquals(0L, japa.totalTimeMs)

            db.japaDao().addTime(japa.id, 65_000)
            db.japaDao().addTime(japa.id, 5_000)
            assertEquals(70_000L, db.japaDao().getById(japa.id)?.totalTimeMs)
        }

        db.close()
        context.deleteDatabase(name)
    }

    @Test
    fun `upgrading from version 4 adds the deity field as blank`() {
        val name = "migration-v4-test.db"
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        file.delete()
        val raw = SQLiteDatabase.openOrCreateDatabase(file, null)
        raw.execSQL(v1JapasTable)
        raw.execSQL("ALTER TABLE japas ADD COLUMN favourite INTEGER NOT NULL DEFAULT 0")
        raw.execSQL("ALTER TABLE japas ADD COLUMN totalTimeMs INTEGER NOT NULL DEFAULT 0")
        raw.execSQL(
            """
            CREATE TABLE IF NOT EXISTS daily_counts (
                japaId INTEGER NOT NULL,
                date TEXT NOT NULL,
                count INTEGER NOT NULL,
                PRIMARY KEY(japaId, date)
            )
            """.trimIndent()
        )
        raw.execSQL(
            """
            INSERT INTO japas
              (name, count, malaSize, lifetimeTarget, backgroundUri, archived, sortOrder,
               accentIndex, createdAt, updatedAt, archivedAt, favourite, totalTimeMs)
            VALUES ('Panchakshari', 777, 108, 0, NULL, 0, 1, 0,
                    1700000000000, 1700000000000, NULL, 0, 90000)
            """.trimIndent()
        )
        raw.version = 4
        raw.close()

        val db = Room.databaseBuilder(context, JapaDatabase::class.java, name)
            .addMigrations(
                JapaDatabase.MIGRATION_1_2,
                JapaDatabase.MIGRATION_2_3,
                JapaDatabase.MIGRATION_3_4,
                JapaDatabase.MIGRATION_4_5,
            )
            .build()

        runBlocking {
            val japa = db.japaDao().allForExport().single()
            assertEquals(777L, japa.count)
            assertEquals(90_000L, japa.totalTimeMs)
            assertEquals("", japa.deity)

            db.japaDao().update(japa.copy(deity = "Shiva"))
            assertEquals("Shiva", db.japaDao().getById(japa.id)?.deity)
        }

        db.close()
        context.deleteDatabase(name)
    }

    @Test
    fun `daily rows accumulate on the same day and clamp at zero`() {
        val db = Room.inMemoryDatabaseBuilder(context, JapaDatabase::class.java).build()

        runBlocking {
            val id = db.japaDao().insert(Japa(name = "Gayatri", malaSize = 108))
            val daily = db.dailyCountDao()

            daily.addToDay(id, "2026-08-06", 108)
            assertEquals(108L, daily.forJapa(id).single().count)

            // A second batch the same day must add to the row, not replace or duplicate it.
            daily.addToDay(id, "2026-08-06", 54)
            assertEquals(1, daily.forJapa(id).size)
            assertEquals(162L, daily.forJapa(id).single().count)

            // Swiping beads back off subtracts from the day.
            daily.addToDay(id, "2026-08-06", -62)
            assertEquals(100L, daily.forJapa(id).single().count)

            // But can never drive the day negative.
            daily.addToDay(id, "2026-08-06", -500)
            assertEquals(0L, daily.forJapa(id).single().count)

            // A different date is a different row.
            daily.addToDay(id, "2026-08-07", 27)
            assertEquals(2, daily.forJapa(id).size)

            // Deleting a japa takes its log with it.
            daily.deleteForJapa(id)
            assertEquals(emptyList<DailyCount>(), daily.forJapa(id))
        }
        db.close()
    }

    @Test
    fun `a first bead of the day with a negative delta cannot create a negative row`() {
        val db = Room.inMemoryDatabaseBuilder(context, JapaDatabase::class.java).build()

        runBlocking {
            val id = db.japaDao().insert(Japa(name = "Edge"))
            db.dailyCountDao().addToDay(id, "2026-08-06", -5)
            assertEquals(0L, db.dailyCountDao().forJapa(id).single().count)
        }
        db.close()
    }

    @Test
    fun `lifetime count clamps at zero when beads are swiped back off`() {
        val db = Room.inMemoryDatabaseBuilder(context, JapaDatabase::class.java).build()

        runBlocking {
            val dao = db.japaDao()
            val id = dao.insert(Japa(name = "Om Namah Shivaya"))
            dao.addToCount(id, 5, 0)
            assertEquals(5L, dao.getById(id)?.count)
            dao.addToCount(id, -50, 0)
            assertEquals(0L, dao.getById(id)?.count)
        }
        db.close()
    }
}
