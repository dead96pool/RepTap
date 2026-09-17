package com.chani.reptap.data.repository

import com.chani.reptap.data.local.db.AppDatabase
import com.chani.reptap.data.local.entity.CycleState
import com.chani.reptap.data.local.entity.SessionStatus
import com.chani.reptap.data.local.entity.WorkoutDay
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class CycleRepository(private val db: AppDatabase) {

    val cycleState: Flow<CycleState?> = db.cycleStateDao().observe()

    private data class NextTrainingDay(
        val day: WorkoutDay,
        val daysAhead: Long
    )

    suspend fun getCurrentDay(): WorkoutDay? {
        val state = ensureStateExists()
        return db.workoutDayDao().getBySlot(state.currentSlot)
    }

    private suspend fun ensureStateExists(): CycleState {
        val existing = db.cycleStateDao().getOnce()
        if (existing != null) return existing
        val initial = CycleState(id = 1, currentSlot = 1, scheduledEpochDay = LocalDate.now().toEpochDay())
        db.cycleStateDao().upsert(initial)
        return initial
    }

    // Call this every time the Home screen loads. If today's date has passed the
    // scheduled date and the workout still hasn't been completed, silently bump
    // the scheduled date to today (this is the "pause and wait" behavior).
    suspend fun refreshScheduleIfOverdue() {
        val state = ensureStateExists()
        val today = LocalDate.now().toEpochDay()
        if (state.scheduledEpochDay < today) {
            db.cycleStateDao().upsert(state.copy(scheduledEpochDay = today))
        }
    }

    suspend fun getScheduledDateForCurrentSlot(): LocalDate {
        val state = ensureStateExists()
        return LocalDate.ofEpochDay(state.scheduledEpochDay)
    }

    private suspend fun findNextTrainingDay(
        currentSlot: Int
    ): NextTrainingDay? {
        val days =
            db.workoutDayDao()
                .getAllOnce()

        if (days.isEmpty()) {
            return null
        }

        val currentIndex =
            days.indexOfFirst {
                it.slotNumber == currentSlot
            }

        /*
         * If the current slot somehow no longer exists,
         * start from the beginning rather than relying on
         * a hard-coded slot number.
         */
        val startIndex =
            if (currentIndex >= 0) {
                currentIndex
            } else {
                days.lastIndex
            }

        var daysAhead = 0L

        /*
         * Examine at most one complete cycle.
         *
         * Recovery slots still count as elapsed calendar
         * days, but are skipped as workout destinations.
         */
        for (offset in 1..days.size) {
            val index =
                (startIndex + offset) %
                        days.size

            val candidate =
                days[index]

            /*
             * Recovery days are genuine cycle/calendar days,
             * so they advance the scheduled date.
             */
            if (candidate.isRecovery) {
                daysAhead += 1L
                continue
            }

            val exerciseCount =
                db.workoutExerciseDao()
                    .getCountForDay(
                        candidate.id
                    )

            /*
             * A non-recovery day with no exercises is a
             * draft workout.
             *
             * Drafts are editable and selectable manually, but
             * do not participate in automatic cycle scheduling
             * until their first exercise is added.
             *
             * They therefore do NOT consume a calendar day.
             */
            if (exerciseCount == 0) {
                continue
            }

            daysAhead += 1L

            return NextTrainingDay(
                day = candidate,
                daysAhead = daysAhead
            )
        }

        /*
         * A cycle containing only recovery days has no
         * valid next workout.
         */
        return null
    }

    suspend fun setSlotManually(slot: Int) {
        db.cycleStateDao().upsert(
            CycleState(id = 1, currentSlot = slot, scheduledEpochDay = LocalDate.now().toEpochDay())
        )
    }

    suspend fun completeSession(
        sessionId: Long
    ) {
        val session = db.sessionLogDao().getById(sessionId) ?: return
        db.sessionLogDao().update(session.copy(finishedAt = System.currentTimeMillis(), status = SessionStatus.COMPLETED))

        val state = ensureStateExists()

        val next = findNextTrainingDay(state.currentSlot) ?: return

        val completionDate =
            LocalDate.now()

        val nextScheduledDate =
            completionDate.plusDays(
                next.daysAhead
            )

        db.cycleStateDao().upsert(
            CycleState(
                id = 1,
                currentSlot =
                    next.day.slotNumber,
                scheduledEpochDay =
                    nextScheduledDate
                        .toEpochDay()
            )
        )
    }
}