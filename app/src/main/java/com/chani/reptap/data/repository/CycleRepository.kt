package com.chani.reptap.data.repository

import com.chani.reptap.data.local.db.AppDatabase
import com.chani.reptap.data.local.entity.CycleState
import com.chani.reptap.data.local.entity.SessionLog
import com.chani.reptap.data.local.entity.SessionStatus
import com.chani.reptap.data.local.entity.WorkoutDay
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class CycleRepository(private val db: AppDatabase) {

    val cycleState: Flow<CycleState?> = db.cycleStateDao().observe()

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

    // Returns the date of the NEXT training day (skipping over recovery slots),
    // without actually advancing the stored state. Used for display only.
    suspend fun getNextWorkoutDate(): LocalDate {
        val state = ensureStateExists()
        var slot = state.currentSlot
        var daysAhead = 0L
        do {
            slot = if (slot >= 6) 1 else slot + 1
            daysAhead += 1
        } while (db.workoutDayDao().getBySlot(slot)?.isRecovery == true)
        return LocalDate.ofEpochDay(state.scheduledEpochDay).plusDays(daysAhead)
    }

    suspend fun setSlotManually(slot: Int) {
        db.cycleStateDao().upsert(
            CycleState(id = 1, currentSlot = slot, scheduledEpochDay = LocalDate.now().toEpochDay())
        )
    }

    suspend fun completeSession(sessionId: Long) {
        val session = db.sessionLogDao().getById(sessionId) ?: return
        db.sessionLogDao().update(session.copy(finishedAt = System.currentTimeMillis(), status = SessionStatus.COMPLETED))

        val state = ensureStateExists()
        var nextSlot = state.currentSlot
        var daysAhead = 0L
        do {
            nextSlot = if (nextSlot >= 6) 1 else nextSlot + 1
            daysAhead += 1
        } while (db.workoutDayDao().getBySlot(nextSlot)?.isRecovery == true)

        val nextScheduledDate = LocalDate.ofEpochDay(state.scheduledEpochDay).plusDays(daysAhead)
        db.cycleStateDao().upsert(
            CycleState(id = 1, currentSlot = nextSlot, scheduledEpochDay = nextScheduledDate.toEpochDay())
        )
    }
}