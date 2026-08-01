package com.chani.reptap.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chani.reptap.data.local.dao.WorkoutExerciseWithName
import com.chani.reptap.data.local.entity.SessionLog
import com.chani.reptap.data.local.entity.SessionStatus
import com.chani.reptap.data.local.entity.SetLog
import com.chani.reptap.data.repository.CycleRepository
import com.chani.reptap.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

enum class SetPhase { COUNTING_REPS, RESTING_BETWEEN_SETS, RESTING_BEFORE_NEXT_EXERCISE, WORKOUT_COMPLETE }

data class ActiveWorkoutUiState(
    val exercises: List<WorkoutExerciseWithName> = emptyList(),
    val currentExerciseIndex: Int = 0,
    val currentSetNumber: Int = 1,
    val repsTapped: Int = 0,
    val phase: SetPhase = SetPhase.COUNTING_REPS,
    val secondsRemaining: Int = 0,
    val weightKg: Float = 5f,
    val isLoading: Boolean = true
) {
    val currentExercise: WorkoutExerciseWithName? get() = exercises.getOrNull(currentExerciseIndex)
}

class ActiveWorkoutViewModel(
    private val workoutRepository: WorkoutRepository,
    private val cycleRepository: CycleRepository,
    private val dayId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    private var sessionId: Long = -1L
    private var timerJob: kotlinx.coroutines.Job? = null

    init {
        loadExercises()
    }

    private fun loadExercises() {
        viewModelScope.launch {
            workoutRepository.getExercisesForDayWithNames(dayId).collect { list ->
                if (_uiState.value.exercises.isEmpty() && list.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        exercises = list,
                        weightKg = list.first().defaultWeightKg,
                        isLoading = false
                    )
                    startSessionIfNeeded()
                }
            }
        }
    }

    private suspend fun startSessionIfNeeded() {
        if (sessionId == -1L) {
            sessionId = workoutRepository.startSession(dayId)
        }
    }

    fun tapRep() {
        val state = _uiState.value
        if (state.phase != SetPhase.COUNTING_REPS) return
        _uiState.value = state.copy(repsTapped = state.repsTapped + 1)
    }

    fun undoTap() {
        val state = _uiState.value
        if (state.phase != SetPhase.COUNTING_REPS || state.repsTapped == 0) return
        _uiState.value = state.copy(repsTapped = state.repsTapped - 1)
    }

    fun finishSet() {
        val state = _uiState.value
        val exercise = state.currentExercise ?: return
        if (state.phase != SetPhase.COUNTING_REPS) return

        viewModelScope.launch {
            workoutRepository.logSet(
                SetLog(
                    sessionLogId = sessionId,
                    workoutExerciseId = exercise.id,
                    setNumber = state.currentSetNumber,
                    repsCompleted = state.repsTapped,
                    weightKg = state.weightKg,
                    timestamp = System.currentTimeMillis()
                )
            )

            val isLastSetOfExercise = state.currentSetNumber >= exercise.targetSets
            val restDuration = if (isLastSetOfExercise) exercise.restBeforeNextSec else exercise.restBetweenSetsSec
            val nextPhase = if (isLastSetOfExercise) SetPhase.RESTING_BEFORE_NEXT_EXERCISE else SetPhase.RESTING_BETWEEN_SETS

            if (restDuration <= 0) {
                advanceAfterRest(isLastSetOfExercise)
            } else {
                _uiState.value = _uiState.value.copy(phase = nextPhase, secondsRemaining = restDuration)
                startTimer(restDuration, isLastSetOfExercise)
            }
        }
    }

    private fun startTimer(seconds: Int, isLastSetOfExercise: Boolean) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000)
                remaining -= 1
                _uiState.value = _uiState.value.copy(secondsRemaining = remaining)
            }
            advanceAfterRest(isLastSetOfExercise)
        }
    }

    private fun advanceAfterRest(wasLastSetOfExercise: Boolean) {
        val state = _uiState.value
        if (wasLastSetOfExercise) {
            val nextIndex = state.currentExerciseIndex + 1
            if (nextIndex >= state.exercises.size) {
                completeWorkout()
            } else {
                _uiState.value = state.copy(
                    currentExerciseIndex = nextIndex,
                    currentSetNumber = 1,
                    repsTapped = 0,
                    phase = SetPhase.COUNTING_REPS,
                    weightKg = state.exercises[nextIndex].defaultWeightKg
                )
            }
        } else {
            _uiState.value = state.copy(
                currentSetNumber = state.currentSetNumber + 1,
                repsTapped = 0,
                phase = SetPhase.COUNTING_REPS
            )
        }
    }

    private fun completeWorkout() {
        viewModelScope.launch {
            cycleRepository.completeSession(sessionId)
            _uiState.value = _uiState.value.copy(phase = SetPhase.WORKOUT_COMPLETE)
        }
    }

    fun quitSession() {
        viewModelScope.launch {
            workoutRepository.markSessionQuit(sessionId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}