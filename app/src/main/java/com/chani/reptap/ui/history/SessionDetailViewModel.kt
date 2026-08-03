package com.chani.reptap.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chani.reptap.data.local.dao.SessionRecap
import com.chani.reptap.data.local.dao.SetLogDetail
import com.chani.reptap.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExerciseGroup(
    val exerciseName: String,
    val sets: List<SetLogDetail>
)

class SessionDetailViewModel(
    private val repository: WorkoutRepository,
    private val sessionId: Long,
    private val workoutDayId: Long,
    private val currentStartedAt: Long
) : ViewModel() {

    private val _exerciseGroups = MutableStateFlow<List<ExerciseGroup>>(emptyList())
    val exerciseGroups: StateFlow<List<ExerciseGroup>> = _exerciseGroups.asStateFlow()

    private val _previousSession = MutableStateFlow<SessionRecap?>(null)
    val previousSession: StateFlow<SessionRecap?> = _previousSession.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getSetDetailsForSession(sessionId).collect { list ->
                _exerciseGroups.value = list.groupBy { it.exerciseName }
                    .map { (name, sets) -> ExerciseGroup(name, sets) }
            }
        }
        viewModelScope.launch {
            _previousSession.value = repository.getPreviousSessionForDay(workoutDayId, currentStartedAt)
        }
    }
}