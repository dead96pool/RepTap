package com.chani.reptap.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.chani.reptap.data.local.dao.*
import com.chani.reptap.data.local.entity.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MuscleGroup::class,
        Exercise::class,
        WorkoutDay::class,
        WorkoutExercise::class,
        SessionLog::class,
        SetLog::class,
        CycleState::class
    ],
    version = 10,
    exportSchema = false
)

@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun muscleGroupDao(): MuscleGroupDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDayDao(): WorkoutDayDao
    abstract fun workoutExerciseDao(): WorkoutExerciseDao
    abstract fun sessionLogDao(): SessionLogDao
    abstract fun setLogDao(): SetLogDao
    abstract fun cycleStateDao(): CycleStateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_8_9 =
            object : Migration(8, 9) {
                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {
                    database.execSQL(
                        """
                ALTER TABLE workout_exercises
                ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1
                """.trimIndent()
                    )
                }
            }
        private val MIGRATION_9_10 =
            object : Migration(9, 10) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {
                    database.execSQL(
                        """
                ALTER TABLE session_logs
                ADD COLUMN workoutDayNameSnapshot TEXT
                NOT NULL DEFAULT ''
                """.trimIndent()
                    )

                    /*
                     * Existing sessions were created before
                     * snapshots existed, so preserve their
                     * current workout name as their historical
                     * name.
                     */
                    database.execSQL(
                        """
                UPDATE session_logs
                SET workoutDayNameSnapshot =
                    COALESCE(
                        (
                            SELECT wd.name
                            FROM workout_days wd
                            WHERE wd.id =
                                session_logs.workoutDayId
                        ),
                        ''
                    )
                """.trimIndent()
                    )
                }
            }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "reptap_database"
                )
                    .addMigrations(
                        MIGRATION_8_9,
                        MIGRATION_9_10
                    )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}