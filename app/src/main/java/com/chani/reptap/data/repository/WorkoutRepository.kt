package com.chani.reptap.data.repository

import com.chani.reptap.data.local.db.AppDatabase
import com.chani.reptap.data.local.entity.*
import kotlinx.coroutines.flow.first


class WorkoutRepository(private val db: AppDatabase) {

    val muscleGroups = db.muscleGroupDao().getAll()
    val exercises = db.exerciseDao().getAll()
    val workoutDays = db.workoutDayDao().getAll()

    val sessionHistory = db.sessionLogDao().getSessionSummaries()

    fun getExercisesForDay(dayId: Long) = db.workoutExerciseDao().getForDay(dayId)

    fun getExercisesForDayWithNames(dayId: Long) = db.workoutExerciseDao().getForDayWithNames(dayId)

    fun getMuscleProgress(startTime: Long, endTime: Long) = db.setLogDao().getMuscleProgress(startTime, endTime)

    val completedSessionRecaps = db.sessionLogDao().getCompletedSessionRecaps()

    suspend fun getPreviousSessionForDay(workoutDayId: Long, currentStartedAt: Long) =
        db.sessionLogDao().getPreviousSessionForDay(workoutDayId, currentStartedAt)

    suspend fun getTotalSessions() = db.sessionLogDao().getTotalSessionsCount()

    suspend fun getAverageDurationMillis() = db.sessionLogDao().getAverageDurationMillis()

    suspend fun getFirstSessionDate() = db.sessionLogDao().getFirstSessionDate()

    suspend fun getCompletedNonRecoverySessionsCount() = db.sessionLogDao().getCompletedNonRecoverySessionsCount()

    suspend fun getAllExerciseSessionVolumes() = db.setLogDao().getAllExerciseSessionVolumes()

    suspend fun getExerciseSessionDataPoints(exerciseId: Long) = db.setLogDao().getExerciseSessionDataPoints(exerciseId)

    suspend fun getExerciseDetailStats(exerciseId: Long) = db.setLogDao().getExerciseDetailStats(exerciseId)

    fun getSetDetailsForSession(sessionId: Long) = db.setLogDao().getSetDetailsForSession(sessionId)

    suspend fun getSessionById(sessionId: Long) = db.sessionLogDao().getById(sessionId)

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

        val exerciseDefs = listOf(
            Triple("Dumbbell curl", "Biceps", true),
            Triple("Cross-body hammer curl", "Biceps", true),
            Triple("Overhead dumbbell triceps extension", "Triceps", true),
            Triple("Dumbbell skullcrusher on floor", "Triceps", false),
            Triple("Standing dumbbell shoulder press", "Shoulders", false),
            Triple("Dumbbell lateral raise", "Shoulders", false),
            Triple("Wrist curl", "Forearms", false),
            Triple("Reverse wrist curl", "Forearms", false),
            Triple("Incline push-up", "Chest", false),
            Triple("One-arm dumbbell row", "Back", true),
            Triple("Goblet squat", "Legs", false),
            Triple("Dumbbell Romanian deadlift", "Legs", false),
            Triple("Dumbbell floor press", "Chest", false),
            Triple("Reverse fly", "Shoulders", false),
            Triple("Glute bridge", "Legs", false),
            Triple("Side plank", "Core", true),
            Triple("Russian twist", "Core", true),
            Triple("Seated preacher-style supinated curl", "Biceps", true),
            Triple("Bent-over triceps kickback", "Triceps", true),
            Triple("Dumbbell shrug", "Shoulders", false),
            Triple("Dumbbell reverse curl", "Forearms", false),
            Triple("Split squat", "Legs", true),
            Triple("Slow-tempo sumo squat", "Legs", false),
            Triple("Incline close-grip push-up", "Chest", false),
            Triple("Dumbbell squeeze floor press", "Chest", false),
            Triple("Calf raise", "Legs", false),
            Triple("Bent-knee leg raise", "Core", false)
        )
        exerciseDao.insertAll(exerciseDefs.map { (name, muscle, perSide) ->
            Exercise(name = name, primaryMuscleId = m(muscle), isBodyweight = name.contains("push-up", true) || name.contains("plank", true) || name.contains("twist", true) || name.contains("leg raise", true), isPerSide = perSide)
        })
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
            WorkoutExercise(workoutDayId = day2, exerciseId = ex("Side plank"), orderIndex = 8, targetSets = 2, repRangeMin = 20, repRangeMax = 45, restBetweenSetsSec = 45, restBeforeNextSec = 45),
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
        val dayNames = listOf(
            "Day 1 - Arms and Shoulders A",
            "Day 2 - Legs, Chest, Back, Core",
            "Day 4 - Arms and Shoulders B",
            "Day 5 - Legs, Chest, Back, Core",
            "Day 6 - Arm Thickness Specialization"
        )
        val days = dayNames.map { name ->
            db.workoutDayDao().getAllOnce().first { it.name == name }
        }

        val now = System.currentTimeMillis()
        val dayMillis = 24 * 60 * 60 * 1000L

        // 3 full cycles, 7 days each, starting 21 days ago
        var cursorDayOffset = 21L
        val cycleOffsets = listOf(0L, 1L, 3L, 4L, 5L) // Day1, Day2, Day4, Day5, Day6 within a 7-day cycle

        for (cycle in 0 until 3) {
            for ((index, day) in days.withIndex()) {
                val daysAgo = cursorDayOffset - (cycle * 7L) - cycleOffsets[index]
                val startedAt = now - (daysAgo * dayMillis)
                val finishedAt = startedAt + (55 * 60 * 1000L)

                val sessionId = db.sessionLogDao().insert(
                    SessionLog(
                        workoutDayId = day.id,
                        startedAt = startedAt,
                        finishedAt = finishedAt,
                        status = SessionStatus.COMPLETED
                    )
                )

                val exercisesForDay = db.workoutExerciseDao().getForDay(day.id).first()
                for (we in exercisesForDay) {
                    // Rising weight each cycle to simulate improvement
                    val baseWeight = 5f + cycle * 1.5f
                    db.setLogDao().insert(
                        SetLog(
                            sessionLogId = sessionId,
                            workoutExerciseId = we.id,
                            setNumber = 1,
                            repsCompleted = 15,
                            weightKg = baseWeight,
                            timestamp = startedAt
                        )
                    )
                    db.setLogDao().insert(
                        SetLog(
                            sessionLogId = sessionId,
                            workoutExerciseId = we.id,
                            setNumber = 2,
                            repsCompleted = 14,
                            weightKg = baseWeight,
                            timestamp = startedAt
                        )
                    )
                }
            }
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