package com.chani.reptap.data.repository

import com.chani.reptap.data.local.db.AppDatabase
import com.chani.reptap.data.local.entity.CycleState
import com.chani.reptap.data.local.entity.SessionLog
import com.chani.reptap.data.local.entity.SessionStatus
import com.chani.reptap.data.local.entity.WorkoutDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CycleRepository(private val db: AppDatabase) {

    val cycleState: Flow<CycleState?> = db.cycleStateDao().observe()

    suspend fun getCurrentDay(): WorkoutDay? {
        val state = db.cycleStateDao().getOnce() ?: return null
        return db.workoutDayDao().getBySlot(state.currentSlot)
    }

    suspend fun advanceToNextSlot() {
        val state = db.cycleStateDao().getOnce() ?: CycleState(currentSlot = 1)
        val nextSlot = if (state.currentSlot >= 6) 1 else state.currentSlot + 1
        db.cycleStateDao().upsert(CycleState(id = 1, currentSlot = nextSlot))
    }

    suspend fun setSlotManually(slot: Int) {
        db.cycleStateDao().upsert(CycleState(id = 1, currentSlot = slot))
    }

    suspend fun autoCompleteRecoveryIfNeeded() {
        val day = getCurrentDay() ?: return
        if (day.isRecovery) {
            advanceToNextSlot()
        }
    }

    suspend fun completeSession(sessionId: Long) {
        val session = db.sessionLogDao().getById(sessionId) ?: return
        db.sessionLogDao().update(session.copy(finishedAt = System.currentTimeMillis(), status = SessionStatus.COMPLETED))
        advanceToNextSlot()
    }
}