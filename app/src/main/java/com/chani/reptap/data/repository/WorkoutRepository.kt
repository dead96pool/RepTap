package com.chani.reptap.data.repository

import com.chani.reptap.data.local.db.AppDatabase
import com.chani.reptap.data.local.entity.*
import androidx.room.withTransaction
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId


class WorkoutRepository(private val db: AppDatabase) {

    val exercises = db.exerciseDao().getAll()
    val workoutDays = db.workoutDayDao().getAll()

    fun getExercisesForDayWithNames(dayId: Long) = db.workoutExerciseDao().getForDayWithNames(dayId)

    fun getWorkoutExerciseWithName(
        workoutExerciseId: Long
    ) =
        db.workoutExerciseDao()
            .getByIdWithName(workoutExerciseId)

    suspend fun updateWorkoutExerciseConfiguration(
        workoutExerciseId: Long,
        targetSets: Int,
        targetReps: Int,
        targetDurationSec: Int?,
        defaultWeightKg: Float?,
        restBetweenSetsSec: Int,
        restBeforeNextSec: Int
    ): Boolean {
        val current =
            db.workoutExerciseDao()
                .getById(workoutExerciseId)
                ?: return false

        db.workoutExerciseDao().update(
            current.copy(
                targetSets = targetSets,
                targetReps = targetReps,
                targetDurationSec = targetDurationSec,
                defaultWeightKg = defaultWeightKg,
                restBetweenSetsSec = restBetweenSetsSec,
                restBeforeNextSec = restBeforeNextSec
            )
        )

        return true
    }

    val completedSessionRecaps = db.sessionLogDao().getCompletedSessionRecaps()

    suspend fun getPreviousSessionForDay(workoutDayId: Long, currentStartedAt: Long) =
        db.sessionLogDao().getPreviousSessionForDay(workoutDayId, currentStartedAt)

    suspend fun getSessionRecapById(sessionId: Long) =
        db.sessionLogDao().getSessionRecapById(sessionId)

    suspend fun getTotalSessions() = db.sessionLogDao().getTotalSessionsCount()

    suspend fun getAverageDurationMillis() = db.sessionLogDao().getAverageDurationMillis()

    suspend fun getFirstSessionDate() = db.sessionLogDao().getFirstSessionDate()

    suspend fun getAllExerciseSessionVolumes() = db.setLogDao().getAllExerciseSessionVolumes()

    suspend fun getExerciseSessionDataPoints(exerciseId: Long) = db.setLogDao().getExerciseSessionDataPoints(exerciseId)

    suspend fun getExerciseSetDetails(exerciseId: Long) = db.setLogDao().getExerciseSetDetails(exerciseId)

    suspend fun getExerciseDetailStats(exerciseId: Long) = db.setLogDao().getExerciseDetailStats(exerciseId)

    fun getSetDetailsForSession(sessionId: Long) = db.setLogDao().getSetDetailsForSession(sessionId)


    suspend fun getAllExerciseTrendSessions() =
        db.setLogDao().getAllExerciseTrendSessions()

    suspend fun removeWorkoutExercise(
        workoutExerciseId: Long
    ): Boolean {
        return db.withTransaction {
            val dao =
                db.workoutExerciseDao()

            val current =
                dao.getById(
                    workoutExerciseId
                )
                    ?: return@withTransaction false

            if (!current.isActive) {
                return@withTransaction true
            }

            /*
             * For now a workout must retain at least one
             * active exercise.
             */
            val activeCount =
                dao.getActiveCountForDay(
                    current.workoutDayId
                )

            if (activeCount <= 1) {
                return@withTransaction false
            }

            /*
             * Archive instead of physically deleting.
             *
             * Historical SetLogs can therefore continue
             * referencing this WorkoutExercise row.
             */
            dao.update(
                current.copy(
                    isActive = false
                )
            )

            /*
             * Close any orderIndex gaps created by removal.
             */
            val remaining =
                dao.getForDayOnce(
                    current.workoutDayId
                )

            remaining.forEachIndexed {
                    index,
                    workoutExercise ->

                val newOrder =
                    index + 1

                if (
                    workoutExercise.orderIndex !=
                    newOrder
                ) {
                    dao.update(
                        workoutExercise.copy(
                            orderIndex = newOrder
                        )
                    )
                }
            }

            true
        }
    }

    suspend fun addWorkoutExercise(
        dayId: Long,
        exerciseId: Long,
        targetSets: Int,
        targetReps: Int,
        targetDurationSec: Int?,
        defaultWeightKg: Float?,
        restBetweenSetsSec: Int,
        restBeforeNextSec: Int
    ): Boolean {
        return db.withTransaction {
            val dao =
                db.workoutExerciseDao()

            /*
             * Prevent the same exercise from appearing twice
             * in the active workout.
             */
            val alreadyActive =
                dao.getActiveForExercise(
                    dayId = dayId,
                    exerciseId = exerciseId
                )

            if (alreadyActive != null) {
                return@withTransaction false
            }

            val nextOrderIndex =
                dao.getActiveCountForDay(dayId) + 1

            /*
             * Prefer reactivating an archived row. This keeps
             * historical SetLog foreign-key references intact.
             */
            val archived =
                dao.getInactiveForExercise(
                    dayId = dayId,
                    exerciseId = exerciseId
                )

            if (archived != null) {
                dao.update(
                    archived.copy(
                        orderIndex = nextOrderIndex,
                        targetSets = targetSets,
                        targetReps = targetReps,
                        targetDurationSec =
                            targetDurationSec,
                        restBetweenSetsSec =
                            restBetweenSetsSec,
                        restBeforeNextSec =
                            restBeforeNextSec,
                        defaultWeightKg =
                            defaultWeightKg,
                        isActive = true
                    )
                )
            } else {
                dao.insert(
                    WorkoutExercise(
                        workoutDayId = dayId,
                        exerciseId = exerciseId,
                        orderIndex = nextOrderIndex,
                        targetSets = targetSets,
                        targetReps = targetReps,
                        targetDurationSec =
                            targetDurationSec,
                        restBetweenSetsSec =
                            restBetweenSetsSec,
                        restBeforeNextSec =
                            restBeforeNextSec,
                        defaultWeightKg =
                            defaultWeightKg,
                        isActive = true
                    )
                )
            }

            true
        }
    }

    suspend fun reorderWorkoutExercises(
        dayId: Long,
        orderedWorkoutExerciseIds: List<Long>
    ): Boolean {
        return db.withTransaction {
            val dao =
                db.workoutExerciseDao()

            val current =
                dao.getForDayOnce(dayId)

            val currentIds =
                current.map { it.id }

            /*
             * Refuse an invalid reorder rather than risking
             * silently corrupting workout structure.
             */
            if (
                orderedWorkoutExerciseIds.size !=
                currentIds.size ||
                orderedWorkoutExerciseIds
                    .distinct()
                    .size !=
                currentIds.size ||
                orderedWorkoutExerciseIds.toSet() !=
                currentIds.toSet()
            ) {
                return@withTransaction false
            }

            val exercisesById =
                current.associateBy { it.id }

            orderedWorkoutExerciseIds
                .forEachIndexed { index, id ->

                    val exercise =
                        exercisesById[id]
                            ?: return@withTransaction false

                    val newOrderIndex =
                        index + 1

                    if (
                        exercise.orderIndex !=
                        newOrderIndex
                    ) {
                        dao.update(
                            exercise.copy(
                                orderIndex =
                                    newOrderIndex
                            )
                        )
                    }
                }

            true
        }
    }

    suspend fun renameWorkout(
        dayId: Long,
        newName: String
    ): Boolean {
        val trimmedName =
            newName.trim()

        if (trimmedName.isBlank()) {
            return false
        }

        val current =
            db.workoutDayDao()
                .getById(dayId)
                ?: return false

        db.workoutDayDao().update(
            current.copy(
                name = trimmedName
            )
        )

        return true
    }

    suspend fun duplicateWorkout(
        sourceDayId: Long,
        newName: String
    ): Long? {
        val trimmedName =
            newName.trim()

        if (trimmedName.isBlank()) {
            return null
        }

        return db.withTransaction {
            val dayDao =
                db.workoutDayDao()

            val exerciseDao =
                db.workoutExerciseDao()

            val sourceDay =
                dayDao.getById(
                    sourceDayId
                )
                    ?: return@withTransaction null

            if (sourceDay.isRecovery) {
                return@withTransaction null
            }

            val existingDays =
                dayDao.getAllOnce()

            /*
             * Keep workout names unambiguous in menus.
             */
            val nameAlreadyExists =
                existingDays.any { day ->
                    day.name.equals(
                        trimmedName,
                        ignoreCase = true
                    )
                }

            if (nameAlreadyExists) {
                return@withTransaction null
            }

            /*
             * New workouts are appended to the end of the
             * current cycle.
             */
            val nextSlotNumber =
                (
                        existingDays
                            .maxOfOrNull {
                                it.slotNumber
                            }
                            ?: 0
                        ) + 1

            val newDayId =
                dayDao.insert(
                    WorkoutDay(
                        name = trimmedName,
                        slotNumber =
                            nextSlotNumber,
                        isRecovery = false
                    )
                )

            if (newDayId <= 0L) {
                return@withTransaction null
            }

            /*
             * getForDayOnce() already returns only active
             * workout exercises.
             *
             * Archived exercises that were previously removed
             * from the source workout are therefore NOT copied.
             */
            val sourceExercises =
                exerciseDao.getForDayOnce(
                    sourceDayId
                )

            sourceExercises.forEach {
                    sourceExercise ->

                /*
                 * id = 0 creates a completely new
                 * WorkoutExercise row.
                 *
                 * This is essential: editing the duplicate
                 * must never alter the original workout.
                 */
                exerciseDao.insert(
                    sourceExercise.copy(
                        id = 0L,
                        workoutDayId =
                            newDayId,
                        isActive = true
                    )
                )
            }

            newDayId
        }
    }

    suspend fun createWorkout(
        newName: String
    ): Long? {
        val trimmedName =
            newName.trim()

        if (trimmedName.isBlank()) {
            return null
        }

        return db.withTransaction {
            val dayDao =
                db.workoutDayDao()

            val existingDays =
                dayDao.getAllOnce()

            val nameAlreadyExists =
                existingDays.any { day ->
                    day.name.equals(
                        trimmedName,
                        ignoreCase = true
                    )
                }

            if (nameAlreadyExists) {
                return@withTransaction null
            }

            val nextSlotNumber =
                (
                        existingDays
                            .maxOfOrNull {
                                it.slotNumber
                            }
                            ?: 0
                        ) + 1

            val newDayId =
                dayDao.insert(
                    WorkoutDay(
                        name = trimmedName,
                        slotNumber =
                            nextSlotNumber,
                        isRecovery = false
                    )
                )

            if (newDayId <= 0L) {
                null
            } else {
                newDayId
            }
        }
    }

    suspend fun seedIfEmpty() {
        val weDao = db.workoutExerciseDao()
        if (weDao.count() > 0) return // already seeded, skip

        val muscleDao = db.muscleGroupDao()
        val exerciseDao = db.exerciseDao()
        val dayDao = db.workoutDayDao()

        data class MuscleSeed(
            val historyKey: String,
            val name: String
        )

        val muscleDefs = listOf(
            MuscleSeed(
                historyKey = "builtin.muscle.biceps",
                name = "Biceps"
            ),
            MuscleSeed(
                historyKey = "builtin.muscle.triceps",
                name = "Triceps"
            ),
            MuscleSeed(
                historyKey = "builtin.muscle.shoulders",
                name = "Shoulders"
            ),
            MuscleSeed(
                historyKey = "builtin.muscle.forearms",
                name = "Forearms"
            ),
            MuscleSeed(
                historyKey = "builtin.muscle.chest",
                name = "Chest"
            ),
            MuscleSeed(
                historyKey = "builtin.muscle.back",
                name = "Back"
            ),
            MuscleSeed(
                historyKey = "builtin.muscle.legs",
                name = "Legs"
            ),
            MuscleSeed(
                historyKey = "builtin.muscle.core",
                name = "Core"
            )
        )

        muscleDao.insertAll(
            muscleDefs.map { definition ->
                MuscleGroup(
                    historyKey = definition.historyKey,
                    name = definition.name
                )
            }
        )

        val muscles =
            muscleDao.getAllOnce()
                .associateBy { it.name }

        fun m(name: String) =
            muscles.getValue(name).id

        data class ExerciseSeed(
            val historyKey: String,
            val name: String,
            val muscle: String,
            val isPerSide: Boolean,
            val trackingType: ExerciseTrackingType =
                ExerciseTrackingType.REPS
        )

        val exerciseDefs = listOf(
            ExerciseSeed(
                "builtin.exercise.dumbbell-curl",
                "Dumbbell curl",
                "Biceps",
                true
            ),
            ExerciseSeed(
                "builtin.exercise.cross-body-hammer-curl",
                "Cross-body hammer curl",
                "Biceps",
                true
            ),
            ExerciseSeed(
                "builtin.exercise.overhead-dumbbell-triceps-extension",
                "Overhead dumbbell triceps extension",
                "Triceps",
                true
            ),
            ExerciseSeed(
                "builtin.exercise.dumbbell-skullcrusher-on-floor",
                "Dumbbell skullcrusher on floor",
                "Triceps",
                false
            ),
            ExerciseSeed(
                "builtin.exercise.standing-dumbbell-shoulder-press",
                "Standing dumbbell shoulder press",
                "Shoulders",
                false
            ),
            ExerciseSeed(
                "builtin.exercise.dumbbell-lateral-raise",
                "Dumbbell lateral raise",
                "Shoulders",
                false
            ),
            ExerciseSeed(
                "builtin.exercise.wrist-curl",
                "Wrist curl",
                "Forearms",
                false
            ),
            ExerciseSeed(
                "builtin.exercise.reverse-wrist-curl",
                "Reverse wrist curl",
                "Forearms",
                false
            ),
            ExerciseSeed(
                "builtin.exercise.incline-push-up",
                "Incline push-up",
                "Chest",
                false
            ),
            ExerciseSeed(
                "builtin.exercise.one-arm-dumbbell-row",
                "One-arm dumbbell row",
                "Back",
                true
            ),
            ExerciseSeed(
                "builtin.exercise.goblet-squat",
                "Goblet squat",
                "Legs",
                false
            ),
            ExerciseSeed(
                "builtin.exercise.dumbbell-romanian-deadlift",
                "Dumbbell Romanian deadlift",
                "Legs",
                false
            ),
            ExerciseSeed(
                "builtin.exercise.dumbbell-floor-press",
                "Dumbbell floor press",
                "Chest",
                false
            ),
            ExerciseSeed(
                "builtin.exercise.reverse-fly",
                "Reverse fly",
                "Shoulders",
                false
            ),
            ExerciseSeed(
                "builtin.exercise.glute-bridge",
                "Glute bridge",
                "Legs",
                false
            ),
            ExerciseSeed(
                historyKey = "builtin.exercise.side-plank",
                name = "Side plank",
                muscle = "Core",
                isPerSide = true,
                trackingType = ExerciseTrackingType.TIME
            ),
            ExerciseSeed(
                "builtin.exercise.russian-twist",
                "Russian twist",
                "Core",
                true
            ),
            ExerciseSeed(
                "builtin.exercise.seated-preacher-style-supinated-curl",
                "Seated preacher-style supinated curl",
                "Biceps",
                true
            ),
            ExerciseSeed(
                "builtin.exercise.bent-over-triceps-kickback",
                "Bent-over triceps kickback",
                "Triceps",
                true
            ),
            ExerciseSeed(
                "builtin.exercise.dumbbell-shrug",
                "Dumbbell shrug",
                "Shoulders",
                false
            ),
            ExerciseSeed(
                "builtin.exercise.dumbbell-reverse-curl",
                "Dumbbell reverse curl",
                "Forearms",
                false
            ),
            ExerciseSeed(
                "builtin.exercise.split-squat",
                "Split squat",
                "Legs",
                true
            ),
            ExerciseSeed(
                "builtin.exercise.slow-tempo-sumo-squat",
                "Slow-tempo sumo squat",
                "Legs",
                false
            ),
            ExerciseSeed(
                "builtin.exercise.incline-close-grip-push-up",
                "Incline close-grip push-up",
                "Chest",
                false
            ),
            ExerciseSeed(
                "builtin.exercise.dumbbell-squeeze-floor-press",
                "Dumbbell squeeze floor press",
                "Chest",
                false
            ),
            ExerciseSeed(
                "builtin.exercise.calf-raise",
                "Calf raise",
                "Legs",
                false
            ),
            ExerciseSeed(
                "builtin.exercise.bent-knee-leg-raise",
                "Bent-knee leg raise",
                "Core",
                false
            )
        )

        exerciseDao.insertAll(
            exerciseDefs.map { definition ->
                Exercise(
                    historyKey = definition.historyKey,
                    name = definition.name,
                    primaryMuscleId = m(definition.muscle),
                    isBodyweight =
                        definition.name.contains(
                            "push-up",
                            true
                        ) ||
                                definition.name.contains(
                                    "plank",
                                    true
                                ) ||
                                definition.name.contains(
                                    "twist",
                                    true
                                ) ||
                                definition.name.contains(
                                    "leg raise",
                                    true
                                ),
                    isPerSide =
                        definition.isPerSide,
                    trackingType =
                        definition.trackingType
                )
            }
        )
        val exMap = exerciseDao.getAllOnce().associateBy { it.name }
        fun ex(name: String) = exMap.getValue(name).id

        val days = listOf(
            Triple("Day 1 - Arms and Shoulders A", 1, false),
            Triple("Day 2 - Legs, Chest, Back, Core", 2, false),
            Triple("Day 3 - Complete Rest", 3, true),
            Triple("Day 4 - Arms and Shoulders B", 4, false),
            Triple("Day 5 - Legs, Chest, Back, Core", 5, false),
            Triple("Day 6 - Arm Thickness Specialization", 6, false),
            Triple("Day 7 - Complete Rest", 7, true)
        )
        dayDao.insertAll(days.map { (name, slot, isRecovery) -> WorkoutDay(name = name, slotNumber = slot, isRecovery = isRecovery) })
        val dayMap = dayDao.getAllOnce().associateBy { it.name }
        fun day(name: String) = dayMap.getValue(name).id

        val day1 = day("Day 1 - Arms and Shoulders A")
        val day2 = day("Day 2 - Legs, Chest, Back, Core")
        val day4 = day("Day 4 - Arms and Shoulders B")
        val day5 = day("Day 5 - Legs, Chest, Back, Core")
        val day6 = day("Day 6 - Arm Thickness Specialization")

        val plan = listOf(
            // Day 1 — Arms + shoulders A
            WorkoutExercise(workoutDayId = day1, exerciseId = ex("Dumbbell curl"), orderIndex = 1, targetSets = 3, targetReps = 12, restBetweenSetsSec = 75, restBeforeNextSec = 75),
            WorkoutExercise(workoutDayId = day1, exerciseId = ex("Cross-body hammer curl"), orderIndex = 2, targetSets = 3, targetReps = 12, restBetweenSetsSec = 75, restBeforeNextSec = 75),
            WorkoutExercise(workoutDayId = day1, exerciseId = ex("Overhead dumbbell triceps extension"), orderIndex = 3, targetSets = 3, targetReps = 12, restBetweenSetsSec = 82, restBeforeNextSec = 82),
            WorkoutExercise(workoutDayId = day1, exerciseId = ex("Dumbbell skullcrusher on floor"), orderIndex = 4, targetSets = 3, targetReps = 12, restBetweenSetsSec = 82, restBeforeNextSec = 82),
            WorkoutExercise(workoutDayId = day1, exerciseId = ex("Standing dumbbell shoulder press"), orderIndex = 5, targetSets = 2, targetReps = 10, restBetweenSetsSec = 90, restBeforeNextSec = 90),
            WorkoutExercise(workoutDayId = day1, exerciseId = ex("Dumbbell lateral raise"), orderIndex = 6, targetSets = 2, targetReps = 15, restBetweenSetsSec = 68, restBeforeNextSec = 68),
            WorkoutExercise(workoutDayId = day1, exerciseId = ex("Wrist curl"), orderIndex = 7, targetSets = 2, targetReps = 15, restBetweenSetsSec = 52, restBeforeNextSec = 52),
            WorkoutExercise(workoutDayId = day1, exerciseId = ex("Reverse wrist curl"), orderIndex = 8, targetSets = 2, targetReps = 15, restBetweenSetsSec = 52, restBeforeNextSec = 0),

            // Day 2 — Legs + chest + back + core
            WorkoutExercise(workoutDayId = day2, exerciseId = ex("Incline push-up"), orderIndex = 1, targetSets = 2, targetReps = 6, restBetweenSetsSec = 90, restBeforeNextSec = 90),
            WorkoutExercise(workoutDayId = day2, exerciseId = ex("One-arm dumbbell row"), orderIndex = 2, targetSets = 2, targetReps = 12, restBetweenSetsSec = 82, restBeforeNextSec = 82),
            WorkoutExercise(workoutDayId = day2, exerciseId = ex("Goblet squat"), orderIndex = 3, targetSets = 2, targetReps = 15, restBetweenSetsSec = 105, restBeforeNextSec = 105),
            WorkoutExercise(workoutDayId = day2, exerciseId = ex("Dumbbell Romanian deadlift"), orderIndex = 4, targetSets = 2, targetReps = 15, restBetweenSetsSec = 105, restBeforeNextSec = 105),
            WorkoutExercise(workoutDayId = day2, exerciseId = ex("Dumbbell floor press"), orderIndex = 5, targetSets = 2, targetReps = 12, restBetweenSetsSec = 82, restBeforeNextSec = 82),
            WorkoutExercise(workoutDayId = day2, exerciseId = ex("Reverse fly"), orderIndex = 6, targetSets = 2, targetReps = 15, restBetweenSetsSec = 68, restBeforeNextSec = 68),
            WorkoutExercise(workoutDayId = day2, exerciseId = ex("Glute bridge"), orderIndex = 7, targetSets = 2, targetReps = 15, restBetweenSetsSec = 68, restBeforeNextSec = 68),
            WorkoutExercise(workoutDayId = day2, exerciseId = ex("Side plank"), orderIndex = 8, targetSets = 2, targetReps = 0, targetDurationSec = 20, restBetweenSetsSec = 45, restBeforeNextSec = 45, defaultWeightKg = null),
            WorkoutExercise(workoutDayId = day2, exerciseId = ex("Russian twist"), orderIndex = 9, targetSets = 2, targetReps = 10, restBetweenSetsSec = 52, restBeforeNextSec = 0),

            // Day 4 — Arms + shoulders B
            WorkoutExercise(workoutDayId = day4, exerciseId = ex("Seated preacher-style supinated curl"), orderIndex = 1, targetSets = 3, targetReps = 12, restBetweenSetsSec = 75, restBeforeNextSec = 75),
            WorkoutExercise(workoutDayId = day4, exerciseId = ex("Cross-body hammer curl"), orderIndex = 2, targetSets = 3, targetReps = 12, restBetweenSetsSec = 75, restBeforeNextSec = 75),
            WorkoutExercise(workoutDayId = day4, exerciseId = ex("Overhead dumbbell triceps extension"), orderIndex = 3, targetSets = 3, targetReps = 12, restBetweenSetsSec = 82, restBeforeNextSec = 82),
            WorkoutExercise(workoutDayId = day4, exerciseId = ex("Bent-over triceps kickback"), orderIndex = 4, targetSets = 3, targetReps = 15, restBetweenSetsSec = 68, restBeforeNextSec = 68),
            WorkoutExercise(workoutDayId = day4, exerciseId = ex("Dumbbell lateral raise"), orderIndex = 5, targetSets = 2, targetReps = 15, restBetweenSetsSec = 68, restBeforeNextSec = 68),
            WorkoutExercise(workoutDayId = day4, exerciseId = ex("Reverse fly"), orderIndex = 6, targetSets = 2, targetReps = 15, restBetweenSetsSec = 68, restBeforeNextSec = 68),
            WorkoutExercise(workoutDayId = day4, exerciseId = ex("Dumbbell shrug"), orderIndex = 7, targetSets = 2, targetReps = 15, restBetweenSetsSec = 68, restBeforeNextSec = 68),
            WorkoutExercise(workoutDayId = day4, exerciseId = ex("Dumbbell reverse curl"), orderIndex = 8, targetSets = 2, targetReps = 12, restBetweenSetsSec = 68, restBeforeNextSec = 0),

            // Day 5 — Legs + chest + back + core
            WorkoutExercise(workoutDayId = day5, exerciseId = ex("Split squat"), orderIndex = 1, targetSets = 2, targetReps = 10, restBetweenSetsSec = 90, restBeforeNextSec = 90),
            WorkoutExercise(workoutDayId = day5, exerciseId = ex("Dumbbell Romanian deadlift"), orderIndex = 2, targetSets = 2, targetReps = 15, restBetweenSetsSec = 105, restBeforeNextSec = 105),
            WorkoutExercise(workoutDayId = day5, exerciseId = ex("Slow-tempo sumo squat"), orderIndex = 3, targetSets = 2, targetReps = 15, restBetweenSetsSec = 90, restBeforeNextSec = 90),
            WorkoutExercise(workoutDayId = day5, exerciseId = ex("Incline close-grip push-up"), orderIndex = 4, targetSets = 2, targetReps = 6, restBetweenSetsSec = 105, restBeforeNextSec = 105),
            WorkoutExercise(workoutDayId = day5, exerciseId = ex("One-arm dumbbell row"), orderIndex = 5, targetSets = 2, targetReps = 12, restBetweenSetsSec = 82, restBeforeNextSec = 82),
            WorkoutExercise(workoutDayId = day5, exerciseId = ex("Dumbbell squeeze floor press"), orderIndex = 6, targetSets = 2, targetReps = 12, restBetweenSetsSec = 82, restBeforeNextSec = 82),
            WorkoutExercise(workoutDayId = day5, exerciseId = ex("Calf raise"), orderIndex = 7, targetSets = 2, targetReps = 15, restBetweenSetsSec = 52, restBeforeNextSec = 52),
            WorkoutExercise(workoutDayId = day5, exerciseId = ex("Bent-knee leg raise"), orderIndex = 8, targetSets = 2, targetReps = 10, restBetweenSetsSec = 52, restBeforeNextSec = 0),

            // Day 6 — Arm thickness specialization
            WorkoutExercise(workoutDayId = day6, exerciseId = ex("Cross-body hammer curl"), orderIndex = 1, targetSets = 2, targetReps = 12, restBetweenSetsSec = 75, restBeforeNextSec = 75),
            WorkoutExercise(workoutDayId = day6, exerciseId = ex("Dumbbell reverse curl"), orderIndex = 2, targetSets = 2, targetReps = 12, restBetweenSetsSec = 68, restBeforeNextSec = 68),
            WorkoutExercise(workoutDayId = day6, exerciseId = ex("Seated preacher-style supinated curl"), orderIndex = 3, targetSets = 2, targetReps = 12, restBetweenSetsSec = 75, restBeforeNextSec = 75),
            WorkoutExercise(workoutDayId = day6, exerciseId = ex("Overhead dumbbell triceps extension"), orderIndex = 4, targetSets = 2, targetReps = 12, restBetweenSetsSec = 82, restBeforeNextSec = 82),
            WorkoutExercise(workoutDayId = day6, exerciseId = ex("Dumbbell skullcrusher on floor"), orderIndex = 5, targetSets = 2, targetReps = 12, restBetweenSetsSec = 82, restBeforeNextSec = 82),
            WorkoutExercise(workoutDayId = day6, exerciseId = ex("Bent-over triceps kickback"), orderIndex = 6, targetSets = 2, targetReps = 15, restBetweenSetsSec = 68, restBeforeNextSec = 68),
            WorkoutExercise(workoutDayId = day6, exerciseId = ex("Wrist curl"), orderIndex = 7, targetSets = 2, targetReps = 15, restBetweenSetsSec = 52, restBeforeNextSec = 52),
            WorkoutExercise(workoutDayId = day6, exerciseId = ex("Reverse wrist curl"), orderIndex = 8, targetSets = 2, targetReps = 15, restBetweenSetsSec = 52, restBeforeNextSec = 0)
        )

        weDao.insertAll(plan)
        db.cycleStateDao().upsert(
            CycleState(id = 1, currentSlot = 1, scheduledEpochDay = LocalDate.now().toEpochDay())
        )
    }

    suspend fun seedFakeProgressData() {
        db.withTransaction {
            val sessionDao = db.sessionLogDao()
            val setDao = db.setLogDao()
            val dayDao = db.workoutDayDao()

            // TEMP TEST DATA:
            // Always start from a known, clean history.
            sessionDao.deleteAll()

            val workoutDays =
                dayDao.getAllOnce()
                    .associateBy { it.slotNumber }

            val zoneId = ZoneId.systemDefault()

            /*
             * Nine complete 7-day cycles occupy the previous 63 days.
             *
             * Example:
             *
             * Cycle 1  -> 63-57 days ago
             * ...
             * Cycle 9  -> 7-1 days ago
             *
             * We then add one extra Day 1 session today so "This Week"
             * can also be exercised.
             */
            val firstCycleDate =
                LocalDate.now(zoneId).minusDays(63)

            suspend fun seedTrainingDay(
                slotNumber: Int,
                cycleIndex: Int,
                date: LocalDate
            ) {
                val workoutDay =
                    workoutDays[slotNumber] ?: return

                if (workoutDay.isRecovery) {
                    return
                }

                val exercises =
                    getExercisesForDayWithNames(workoutDay.id)
                        .first()

                /*
                 * Vary the workout start time so History contains
                 * realistic start-time data instead of identical times.
                 */
                val startHour =
                    17 + ((cycleIndex + slotNumber) % 3)

                val startMinute =
                    ((cycleIndex * 11 + slotNumber * 7) % 4) * 15

                val startedAt =
                    date
                        .atTime(
                            LocalTime.of(
                                startHour,
                                startMinute
                            )
                        )
                        .atZone(zoneId)
                        .toInstant()
                        .toEpochMilli()

                /*
                 * Give different workout days different baseline lengths.
                 * Also vary them slightly between cycles.
                 */
                val baseDurationMinutes =
                    when (slotNumber) {
                        1 -> 52
                        2 -> 61
                        4 -> 50
                        5 -> 58
                        6 -> 44
                        else -> 50
                    }

                val durationVariation =
                    ((cycleIndex + slotNumber) % 5 - 2) * 2

                val durationMinutes =
                    baseDurationMinutes + durationVariation

                val finishedAt =
                    startedAt +
                            durationMinutes * 60_000L

                val sessionId =
                    sessionDao.insert(
                        SessionLog(
                            workoutDayId =
                                workoutDay.id,
                            workoutDayNameSnapshot =
                                workoutDay.name,
                            startedAt =
                                startedAt,
                            finishedAt =
                                finishedAt,
                            status =
                                SessionStatus.COMPLETED
                        )
                    )

                exercises.forEachIndexed { exerciseIndex, exercise ->
                    /*
                     * Divide rep exercises into three deterministic
                     * trend groups:
                     *
                     * 0 -> improving
                     * 1 -> declining
                     * 2 -> approximately flat
                     *
                     * This lets the Progress screen exercise green,
                     * red and near-zero trend values.
                     */
                    val trendGroup =
                        Math.floorMod(
                            exercise.exerciseName.hashCode(),
                            3
                        )

                    repeat(exercise.targetSets) { setIndex ->
                        val setNumber = setIndex + 1

                        val timestamp =
                            startedAt +
                                    (
                                            exerciseIndex * 8L +
                                                    setIndex * 2L +
                                                    1L
                                            ) * 60_000L

                        if (
                            exercise.trackingType ==
                            ExerciseTrackingType.TIME
                        ) {
                            val targetDuration =
                                exercise.targetDurationSec ?: 20

                            /*
                             * Side Plank progresses steadily:
                             *
                             * early sessions ~20 sec
                             * middle sessions ~30 sec
                             * later sessions 40+ sec
                             *
                             * Timed performance is free to continue beyond
                             * the configured target.
                             */
                            val durationSeconds =
                                targetDuration +
                                        cycleIndex * 3 +
                                        setIndex * 2

                            setDao.insert(
                                SetLog(
                                    sessionLogId = sessionId,
                                    workoutExerciseId = exercise.id,
                                    setNumber = setNumber,
                                    repsCompleted = 0,
                                    durationCompletedSec =
                                        durationSeconds,
                                    weightKg =
                                        exercise.defaultWeightKg,
                                    timestamp = timestamp
                                )
                            )
                        } else {
                            val targetReps =
                                exercise.targetReps

                            /*
                             * There is no longer a configured maximum.
                             *
                             * Fake performance is generated relative to the
                             * exercise's single target instead.
                             *
                             * Trend groups remain:
                             *
                             * 0 -> improving
                             * 1 -> declining
                             * 2 -> approximately flat
                             *
                             * Importantly, declining test data approaches the
                             * target but never drops below it.
                             */
                            val sessionReps =
                                when (trendGroup) {
                                    0 -> {
                                        // Gradually improve above target.
                                        targetReps +
                                                cycleIndex / 2
                                    }

                                    1 -> {
                                        // Start above target and gradually
                                        // decline back toward the target.
                                        targetReps +
                                                (8 - cycleIndex) / 2
                                    }

                                    else -> {
                                        // Stable performance above target.
                                        targetReps + 2
                                    }
                                }

                            /*
                             * Sets within the same workout still vary slightly.
                             */
                            val setAdjustment =
                                when (setIndex) {
                                    0 -> 1
                                    1 -> 0
                                    else -> -1
                                }

                            val completedReps =
                                (sessionReps + setAdjustment)
                                    .coerceAtLeast(targetReps)

                            /*
                             * Existing external-weight trend behaviour remains
                             * unchanged.
                             */
                            val loggedWeight =
                                exercise.defaultWeightKg
                                    ?.let { baseWeight ->
                                        when (trendGroup) {
                                            0 -> {
                                                baseWeight +
                                                        (cycleIndex / 3) * 0.5f
                                            }

                                            1 -> {
                                                baseWeight
                                            }

                                            else -> {
                                                baseWeight +
                                                        if (cycleIndex >= 6) {
                                                            0.5f
                                                        } else {
                                                            0f
                                                        }
                                            }
                                        }
                                    }

                            setDao.insert(
                                SetLog(
                                    sessionLogId = sessionId,
                                    workoutExerciseId = exercise.id,
                                    setNumber = setNumber,
                                    repsCompleted = completedReps,
                                    durationCompletedSec = null,
                                    weightKg = loggedWeight,
                                    timestamp = timestamp
                                )
                            )
                        }
                    }
                }
            }

            /*
             * Nine full Day 1 -> Day 7 loops.
             *
             * Recovery slots 3 and 7 deliberately generate no session.
             */
            repeat(9) { cycleIndex ->
                for (slotNumber in 1..7) {
                    val date =
                        firstCycleDate.plusDays(
                            cycleIndex * 7L +
                                    (slotNumber - 1L)
                        )

                    seedTrainingDay(
                        slotNumber = slotNumber,
                        cycleIndex = cycleIndex,
                        date = date
                    )
                }
            }

            /*
             * One current-day session gives the "This Week" statistic
             * something useful to display while still preserving the
             * nine complete historical cycles above.
             */
            seedTrainingDay(
                slotNumber = 1,
                cycleIndex = 9,
                date = LocalDate.now(zoneId)
            )
        }
    }

    suspend fun startSession(
        dayId: Long
    ): Long {
        val workoutDay =
            db.workoutDayDao()
                .getById(dayId)
                ?: return -1L

        return db.sessionLogDao().insert(
            SessionLog(
                workoutDayId =
                    dayId,
                workoutDayNameSnapshot =
                    workoutDay.name,
                startedAt =
                    System.currentTimeMillis()
            )
        )
    }

    suspend fun logSet(setLog: SetLog) {
        db.setLogDao().insert(setLog)
    }

    suspend fun markSessionQuit(sessionId: Long) {
        val session = db.sessionLogDao().getById(sessionId) ?: return
        db.sessionLogDao().update(session.copy(status = SessionStatus.QUIT, finishedAt = System.currentTimeMillis()))
    }


}