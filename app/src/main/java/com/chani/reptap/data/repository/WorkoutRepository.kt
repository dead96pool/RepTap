package com.chani.reptap.data.repository

import com.chani.reptap.data.local.db.AppDatabase
import com.chani.reptap.data.local.entity.*
import androidx.room.withTransaction
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId


class WorkoutRepository(private val db: AppDatabase) {

    //val muscleGroups = db.muscleGroupDao().getAll()
    val exercises = db.exerciseDao().getAll()
    val workoutDays = db.workoutDayDao().getAll()

    //val sessionHistory = db.sessionLogDao().getSessionSummaries()
    //fun getExercisesForDay(dayId: Long) = db.workoutExerciseDao().getForDay(dayId)

    fun getExercisesForDayWithNames(dayId: Long) = db.workoutExerciseDao().getForDayWithNames(dayId)

    //fun getMuscleProgress(startTime: Long, endTime: Long) = db.setLogDao().getMuscleProgress(startTime, endTime)

    val completedSessionRecaps = db.sessionLogDao().getCompletedSessionRecaps()

    suspend fun getPreviousSessionForDay(workoutDayId: Long, currentStartedAt: Long) =
        db.sessionLogDao().getPreviousSessionForDay(workoutDayId, currentStartedAt)

    suspend fun getSessionRecapById(sessionId: Long) =
        db.sessionLogDao().getSessionRecapById(sessionId)

    suspend fun getTotalSessions() = db.sessionLogDao().getTotalSessionsCount()

    suspend fun getAverageDurationMillis() = db.sessionLogDao().getAverageDurationMillis()

    suspend fun getFirstSessionDate() = db.sessionLogDao().getFirstSessionDate()

    //suspend fun getCompletedNonRecoverySessionsCount() = db.sessionLogDao().getCompletedNonRecoverySessionsCount()

    suspend fun getAllExerciseSessionVolumes() = db.setLogDao().getAllExerciseSessionVolumes()

    suspend fun getExerciseSessionDataPoints(exerciseId: Long) = db.setLogDao().getExerciseSessionDataPoints(exerciseId)

    suspend fun getExerciseSetDetails(exerciseId: Long) = db.setLogDao().getExerciseSetDetails(exerciseId)

    suspend fun getExerciseDetailStats(exerciseId: Long) = db.setLogDao().getExerciseDetailStats(exerciseId)

    fun getSetDetailsForSession(sessionId: Long) = db.setLogDao().getSetDetailsForSession(sessionId)

    //suspend fun getSessionById(sessionId: Long) = db.sessionLogDao().getById(sessionId)

    suspend fun getAllExerciseTrendSessions() =
        db.setLogDao().getAllExerciseTrendSessions()

    suspend fun seedIfEmpty() {
        val weDao = db.workoutExerciseDao()
        if (weDao.count() > 0) return // already seeded, skip

        val muscleDao = db.muscleGroupDao()
        val exerciseDao = db.exerciseDao()
        val dayDao = db.workoutDayDao()

        val muscleNames = listOf("Biceps", "Triceps", "Shoulders", "Forearms", "Chest", "Back", "Legs", "Core")
        muscleDao.insertAll(muscleNames.map { MuscleGroup(name = it) })
        val muscles = muscleDao.getAllOnce().associateBy { it.name }

        fun m(name: String) = muscles.getValue(name).id

        data class ExerciseSeed(
            val name: String,
            val muscle: String,
            val isPerSide: Boolean,
            val trackingType: ExerciseTrackingType = ExerciseTrackingType.REPS
        )

        val exerciseDefs = listOf(
            ExerciseSeed("Dumbbell curl", "Biceps", true),
            ExerciseSeed("Cross-body hammer curl", "Biceps", true),
            ExerciseSeed("Overhead dumbbell triceps extension", "Triceps", true),
            ExerciseSeed("Dumbbell skullcrusher on floor", "Triceps", false),
            ExerciseSeed("Standing dumbbell shoulder press", "Shoulders", false),
            ExerciseSeed("Dumbbell lateral raise", "Shoulders", false),
            ExerciseSeed("Wrist curl", "Forearms", false),
            ExerciseSeed("Reverse wrist curl", "Forearms", false),
            ExerciseSeed("Incline push-up", "Chest", false),
            ExerciseSeed("One-arm dumbbell row", "Back", true),
            ExerciseSeed("Goblet squat", "Legs", false),
            ExerciseSeed("Dumbbell Romanian deadlift", "Legs", false),
            ExerciseSeed("Dumbbell floor press", "Chest", false),
            ExerciseSeed("Reverse fly", "Shoulders", false),
            ExerciseSeed("Glute bridge", "Legs", false),

            ExerciseSeed(
                name = "Side plank",
                muscle = "Core",
                isPerSide = true,
                trackingType = ExerciseTrackingType.TIME
            ),

            ExerciseSeed("Russian twist", "Core", true),
            ExerciseSeed("Seated preacher-style supinated curl", "Biceps", true),
            ExerciseSeed("Bent-over triceps kickback", "Triceps", true),
            ExerciseSeed("Dumbbell shrug", "Shoulders", false),
            ExerciseSeed("Dumbbell reverse curl", "Forearms", false),
            ExerciseSeed("Split squat", "Legs", true),
            ExerciseSeed("Slow-tempo sumo squat", "Legs", false),
            ExerciseSeed("Incline close-grip push-up", "Chest", false),
            ExerciseSeed("Dumbbell squeeze floor press", "Chest", false),
            ExerciseSeed("Calf raise", "Legs", false),
            ExerciseSeed("Bent-knee leg raise", "Core", false)
        )

        exerciseDao.insertAll(
            exerciseDefs.map { definition ->
                Exercise(
                    name = definition.name,
                    primaryMuscleId = m(definition.muscle),
                    isBodyweight =
                        definition.name.contains("push-up", true) ||
                                definition.name.contains("plank", true) ||
                                definition.name.contains("twist", true) ||
                                definition.name.contains("leg raise", true),
                    isPerSide = definition.isPerSide,
                    trackingType = definition.trackingType
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
            WorkoutExercise(workoutDayId = day1, exerciseId = ex("Dumbbell curl"), orderIndex = 1, targetSets = 3, repRangeMin = 12, repRangeMax = 20, restBetweenSetsSec = 75, restBeforeNextSec = 75),
            WorkoutExercise(workoutDayId = day1, exerciseId = ex("Cross-body hammer curl"), orderIndex = 2, targetSets = 3, repRangeMin = 12, repRangeMax = 20, restBetweenSetsSec = 75, restBeforeNextSec = 75),
            WorkoutExercise(workoutDayId = day1, exerciseId = ex("Overhead dumbbell triceps extension"), orderIndex = 3, targetSets = 3, repRangeMin = 12, repRangeMax = 20, restBetweenSetsSec = 82, restBeforeNextSec = 82),
            WorkoutExercise(workoutDayId = day1, exerciseId = ex("Dumbbell skullcrusher on floor"), orderIndex = 4, targetSets = 3, repRangeMin = 12, repRangeMax = 20, restBetweenSetsSec = 82, restBeforeNextSec = 82),
            WorkoutExercise(workoutDayId = day1, exerciseId = ex("Standing dumbbell shoulder press"), orderIndex = 5, targetSets = 2, repRangeMin = 10, repRangeMax = 18, restBetweenSetsSec = 90, restBeforeNextSec = 90),
            WorkoutExercise(workoutDayId = day1, exerciseId = ex("Dumbbell lateral raise"), orderIndex = 6, targetSets = 2, repRangeMin = 15, repRangeMax = 25, restBetweenSetsSec = 68, restBeforeNextSec = 68),
            WorkoutExercise(workoutDayId = day1, exerciseId = ex("Wrist curl"), orderIndex = 7, targetSets = 2, repRangeMin = 15, repRangeMax = 30, restBetweenSetsSec = 52, restBeforeNextSec = 52),
            WorkoutExercise(workoutDayId = day1, exerciseId = ex("Reverse wrist curl"), orderIndex = 8, targetSets = 2, repRangeMin = 15, repRangeMax = 30, restBetweenSetsSec = 52, restBeforeNextSec = 0),

            // Day 2 — Legs + chest + back + core
            WorkoutExercise(workoutDayId = day2, exerciseId = ex("Incline push-up"), orderIndex = 1, targetSets = 2, repRangeMin = 6, repRangeMax = 12, restBetweenSetsSec = 90, restBeforeNextSec = 90),
            WorkoutExercise(workoutDayId = day2, exerciseId = ex("One-arm dumbbell row"), orderIndex = 2, targetSets = 2, repRangeMin = 12, repRangeMax = 25, restBetweenSetsSec = 82, restBeforeNextSec = 82),
            WorkoutExercise(workoutDayId = day2, exerciseId = ex("Goblet squat"), orderIndex = 3, targetSets = 2, repRangeMin = 15, repRangeMax = 25, restBetweenSetsSec = 105, restBeforeNextSec = 105),
            WorkoutExercise(workoutDayId = day2, exerciseId = ex("Dumbbell Romanian deadlift"), orderIndex = 4, targetSets = 2, repRangeMin = 15, repRangeMax = 25, restBetweenSetsSec = 105, restBeforeNextSec = 105),
            WorkoutExercise(workoutDayId = day2, exerciseId = ex("Dumbbell floor press"), orderIndex = 5, targetSets = 2, repRangeMin = 12, repRangeMax = 20, restBetweenSetsSec = 82, restBeforeNextSec = 82),
            WorkoutExercise(workoutDayId = day2, exerciseId = ex("Reverse fly"), orderIndex = 6, targetSets = 2, repRangeMin = 15, repRangeMax = 25, restBetweenSetsSec = 68, restBeforeNextSec = 68),
            WorkoutExercise(workoutDayId = day2, exerciseId = ex("Glute bridge"), orderIndex = 7, targetSets = 2, repRangeMin = 15, repRangeMax = 30, restBetweenSetsSec = 68, restBeforeNextSec = 68),
            WorkoutExercise(workoutDayId = day2, exerciseId = ex("Side plank"), orderIndex = 8, targetSets = 2, repRangeMin = 0, repRangeMax = 0, targetDurationMinSec = 20, targetDurationMaxSec = 45, restBetweenSetsSec = 45, restBeforeNextSec = 45, defaultWeightKg = null),
            WorkoutExercise(workoutDayId = day2, exerciseId = ex("Russian twist"), orderIndex = 9, targetSets = 2, repRangeMin = 10, repRangeMax = 15, restBetweenSetsSec = 52, restBeforeNextSec = 0),

            // Day 4 — Arms + shoulders B
            WorkoutExercise(workoutDayId = day4, exerciseId = ex("Seated preacher-style supinated curl"), orderIndex = 1, targetSets = 3, repRangeMin = 12, repRangeMax = 20, restBetweenSetsSec = 75, restBeforeNextSec = 75),
            WorkoutExercise(workoutDayId = day4, exerciseId = ex("Cross-body hammer curl"), orderIndex = 2, targetSets = 3, repRangeMin = 12, repRangeMax = 20, restBetweenSetsSec = 75, restBeforeNextSec = 75),
            WorkoutExercise(workoutDayId = day4, exerciseId = ex("Overhead dumbbell triceps extension"), orderIndex = 3, targetSets = 3, repRangeMin = 12, repRangeMax = 20, restBetweenSetsSec = 82, restBeforeNextSec = 82),
            WorkoutExercise(workoutDayId = day4, exerciseId = ex("Bent-over triceps kickback"), orderIndex = 4, targetSets = 3, repRangeMin = 15, repRangeMax = 25, restBetweenSetsSec = 68, restBeforeNextSec = 68),
            WorkoutExercise(workoutDayId = day4, exerciseId = ex("Dumbbell lateral raise"), orderIndex = 5, targetSets = 2, repRangeMin = 15, repRangeMax = 25, restBetweenSetsSec = 68, restBeforeNextSec = 68),
            WorkoutExercise(workoutDayId = day4, exerciseId = ex("Reverse fly"), orderIndex = 6, targetSets = 2, repRangeMin = 15, repRangeMax = 25, restBetweenSetsSec = 68, restBeforeNextSec = 68),
            WorkoutExercise(workoutDayId = day4, exerciseId = ex("Dumbbell shrug"), orderIndex = 7, targetSets = 2, repRangeMin = 15, repRangeMax = 25, restBetweenSetsSec = 68, restBeforeNextSec = 68),
            WorkoutExercise(workoutDayId = day4, exerciseId = ex("Dumbbell reverse curl"), orderIndex = 8, targetSets = 2, repRangeMin = 12, repRangeMax = 20, restBetweenSetsSec = 68, restBeforeNextSec = 0),

            // Day 5 — Legs + chest + back + core
            WorkoutExercise(workoutDayId = day5, exerciseId = ex("Split squat"), orderIndex = 1, targetSets = 2, repRangeMin = 10, repRangeMax = 18, restBetweenSetsSec = 90, restBeforeNextSec = 90),
            WorkoutExercise(workoutDayId = day5, exerciseId = ex("Dumbbell Romanian deadlift"), orderIndex = 2, targetSets = 2, repRangeMin = 15, repRangeMax = 25, restBetweenSetsSec = 105, restBeforeNextSec = 105),
            WorkoutExercise(workoutDayId = day5, exerciseId = ex("Slow-tempo sumo squat"), orderIndex = 3, targetSets = 2, repRangeMin = 15, repRangeMax = 25, restBetweenSetsSec = 90, restBeforeNextSec = 90),
            WorkoutExercise(workoutDayId = day5, exerciseId = ex("Incline close-grip push-up"), orderIndex = 4, targetSets = 2, repRangeMin = 6, repRangeMax = 12, restBetweenSetsSec = 105, restBeforeNextSec = 105),
            WorkoutExercise(workoutDayId = day5, exerciseId = ex("One-arm dumbbell row"), orderIndex = 5, targetSets = 2, repRangeMin = 12, repRangeMax = 25, restBetweenSetsSec = 82, restBeforeNextSec = 82),
            WorkoutExercise(workoutDayId = day5, exerciseId = ex("Dumbbell squeeze floor press"), orderIndex = 6, targetSets = 2, repRangeMin = 12, repRangeMax = 25, restBetweenSetsSec = 82, restBeforeNextSec = 82),
            WorkoutExercise(workoutDayId = day5, exerciseId = ex("Calf raise"), orderIndex = 7, targetSets = 2, repRangeMin = 15, repRangeMax = 30, restBetweenSetsSec = 52, restBeforeNextSec = 52),
            WorkoutExercise(workoutDayId = day5, exerciseId = ex("Bent-knee leg raise"), orderIndex = 8, targetSets = 2, repRangeMin = 10, repRangeMax = 15, restBetweenSetsSec = 52, restBeforeNextSec = 0),

            // Day 6 — Arm thickness specialization
            WorkoutExercise(workoutDayId = day6, exerciseId = ex("Cross-body hammer curl"), orderIndex = 1, targetSets = 2, repRangeMin = 12, repRangeMax = 20, restBetweenSetsSec = 75, restBeforeNextSec = 75),
            WorkoutExercise(workoutDayId = day6, exerciseId = ex("Dumbbell reverse curl"), orderIndex = 2, targetSets = 2, repRangeMin = 12, repRangeMax = 20, restBetweenSetsSec = 68, restBeforeNextSec = 68),
            WorkoutExercise(workoutDayId = day6, exerciseId = ex("Seated preacher-style supinated curl"), orderIndex = 3, targetSets = 2, repRangeMin = 12, repRangeMax = 20, restBetweenSetsSec = 75, restBeforeNextSec = 75),
            WorkoutExercise(workoutDayId = day6, exerciseId = ex("Overhead dumbbell triceps extension"), orderIndex = 4, targetSets = 2, repRangeMin = 12, repRangeMax = 20, restBetweenSetsSec = 82, restBeforeNextSec = 82),
            WorkoutExercise(workoutDayId = day6, exerciseId = ex("Dumbbell skullcrusher on floor"), orderIndex = 5, targetSets = 2, repRangeMin = 12, repRangeMax = 20, restBetweenSetsSec = 82, restBeforeNextSec = 82),
            WorkoutExercise(workoutDayId = day6, exerciseId = ex("Bent-over triceps kickback"), orderIndex = 6, targetSets = 2, repRangeMin = 15, repRangeMax = 25, restBetweenSetsSec = 68, restBeforeNextSec = 68),
            WorkoutExercise(workoutDayId = day6, exerciseId = ex("Wrist curl"), orderIndex = 7, targetSets = 2, repRangeMin = 15, repRangeMax = 30, restBetweenSetsSec = 52, restBeforeNextSec = 52),
            WorkoutExercise(workoutDayId = day6, exerciseId = ex("Reverse wrist curl"), orderIndex = 8, targetSets = 2, repRangeMin = 15, repRangeMax = 30, restBetweenSetsSec = 52, restBeforeNextSec = 0)
        )

        weDao.insertAll(plan)
        db.cycleStateDao().upsert(
            CycleState(id = 1, currentSlot = 1, scheduledEpochDay = java.time.LocalDate.now().toEpochDay())
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
                            workoutDayId = workoutDay.id,
                            startedAt = startedAt,
                            finishedAt = finishedAt,
                            status = SessionStatus.COMPLETED
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
                            val minimum =
                                exercise.targetDurationMinSec
                                    ?: 20

                            /*
                             * Side Plank progresses steadily:
                             *
                             * early sessions ~20 sec
                             * middle sessions ~30 sec
                             * later sessions 40+ sec
                             *
                             * The final generated values can exceed
                             * the configured target maximum, testing
                             * the fact that timed sets are not capped.
                             */
                            val durationSeconds =
                                minimum +
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
                            val repMin =
                                exercise.repRangeMin

                            val repMax =
                                exercise.repRangeMax

                            val repRange =
                                (repMax - repMin)
                                    .coerceAtLeast(0)

                            val sessionReps =
                                when (trendGroup) {
                                    0 -> {
                                        // Clearly improving.
                                        repMin +
                                                (
                                                        repRange *
                                                                cycleIndex /
                                                                8
                                                        )
                                    }

                                    1 -> {
                                        // Clearly declining.
                                        repMax -
                                                (
                                                        repRange *
                                                                cycleIndex /
                                                                8
                                                        )
                                    }

                                    else -> {
                                        // Roughly stable.
                                        repMin +
                                                repRange / 2
                                    }
                                }

                            /*
                             * Sets within the same session aren't
                             * identical. Later sets generally lose
                             * a small number of reps.
                             */
                            val setAdjustment =
                                when (setIndex) {
                                    0 -> 1
                                    1 -> 0
                                    else -> -1
                                }

                            val completedReps =
                                (sessionReps + setAdjustment)
                                    .coerceIn(
                                        repMin,
                                        repMax
                                    )

                            /*
                             * For exercises that actually have an
                             * external load, some trend groups gain
                             * weight over time.
                             *
                             * Bodyweight/null-load exercises remain null.
                             */
                            val loggedWeight =
                                exercise.defaultWeightKg
                                    ?.let { baseWeight ->
                                        when (trendGroup) {
                                            0 -> {
                                                baseWeight +
                                                        (
                                                                cycleIndex /
                                                                        3
                                                                ) * 0.5f
                                            }

                                            1 -> {
                                                baseWeight
                                            }

                                            else -> {
                                                baseWeight +
                                                        if (
                                                            cycleIndex >= 6
                                                        ) {
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
                                    repsCompleted =
                                        completedReps,
                                    durationCompletedSec =
                                        null,
                                    weightKg =
                                        loggedWeight,
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

    suspend fun startSession(dayId: Long): Long {
        return db.sessionLogDao().insert(
            SessionLog(workoutDayId = dayId, startedAt = System.currentTimeMillis())
        )
    }

    suspend fun logSet(setLog: SetLog) {
        db.setLogDao().insert(setLog)
    }

    suspend fun markSessionQuit(sessionId: Long) {
        val session = db.sessionLogDao().getById(sessionId) ?: return
        db.sessionLogDao().update(session.copy(status = com.chani.reptap.data.local.entity.SessionStatus.QUIT, finishedAt = System.currentTimeMillis()))
    }


}