package com.starborn.mantracounter.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Japa::class, DailyCount::class], version = 5, exportSchema = false)
abstract class JapaDatabase : RoomDatabase() {

    abstract fun japaDao(): JapaDao

    abstract fun dailyCountDao(): DailyCountDao

    companion object {
        /**
         * Adds the daily log. A real migration rather than a destructive one — anyone already
         * using the app has counts in here that must survive the update.
         */
        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS daily_counts (
                        japaId INTEGER NOT NULL,
                        date TEXT NOT NULL,
                        count INTEGER NOT NULL,
                        PRIMARY KEY(japaId, date)
                    )
                    """.trimIndent()
                )
            }
        }

        /** Adds the favourite flag used by the star button and the favourites sort. */
        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE japas ADD COLUMN favourite INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /** Adds time-spent tracking, shown on the stats screen. */
        internal val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE japas ADD COLUMN totalTimeMs INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /** Adds the deity a mantra belongs to, which the list can also be sorted by. */
        internal val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE japas ADD COLUMN deity TEXT NOT NULL DEFAULT ''")
            }
        }

        @Volatile
        private var instance: JapaDatabase? = null

        fun get(context: Context): JapaDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    JapaDatabase::class.java,
                    "japa.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { instance = it }
            }
    }
}
