package com.chani.reptap.ui.builder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chani.reptap.data.local.entity.ExerciseTrackingType
import com.chani.reptap.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class EditWorkoutExerciseUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isRemoving: Boolean = false,
    val saveComplete: Boolean = false,
    val removeComplete: Boolean = false,

    val exerciseName: String = "",
    val trackingType: ExerciseTrackingType =
        ExerciseTrackingType.REPS,
    val isPerSide: Boolean = false,

    val targetSets: String = "",
    val targetValue: String = "",

    val hasExternalLoad: Boolean = false,
    val externalLoadKg: String = "",

    val restBetweenSetsSec: String = "",
    val restBeforeNextSec: String = "",

    val errorMessage: String? = null
)

class EditWorkoutExerciseViewModel(
    private val repository: WorkoutRepository,
    private val workoutExerciseId: Long
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            EditWorkoutExerciseUiState()
        )

    val uiState:
            StateFlow<EditWorkoutExerciseUiState> =
        _uiState.asStateFlow()

    init {
        loadExercise()
    }

    fun removeFromWorkout() {
        val state =
            _uiState.value

        if (
            state.isSaving ||
            state.isRemoving
        ) {
            return
        }

        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(
                    isRemoving = true,
                    errorMessage = null
                )

            val success =
                repository
                    .removeWorkoutExercise(
                        workoutExerciseId
                    )

            _uiState.value =
                _uiState.value.copy(
                    isRemoving = false,
                    removeComplete = success,
                    errorMessage =
                        if (success) {
                            null
                        } else {
                            "Unable to remove exercise"
                        }
                )
        }
    }

    private fun loadExercise() {
        viewModelScope.launch {
            val exercise =
                repository
                    .getWorkoutExerciseWithName(
                        workoutExerciseId
                    )
                    .first()

            if (exercise == null) {
                _uiState.value =
                    EditWorkoutExerciseUiState(
                        isLoading = false,
                        errorMessage =
                            "Workout exercise not found"
                    )

                return@launch
            }

            val targetValue =
                when (exercise.trackingType) {
                    ExerciseTrackingType.REPS -> {
                        exercise.targetReps.toString()
                    }

                    ExerciseTrackingType.TIME -> {
                        exercise.targetDurationSec
                            ?.toString()
                            ?: ""
                    }
                }

            val weightText =
                exercise.defaultWeightKg
                    ?.let { weight ->
                        if (
                            weight ==
                            weight.toInt().toFloat()
                        ) {
                            weight.toInt().toString()
                        } else {
                            weight.toString()
                        }
                    }
                    ?: ""

            _uiState.value =
                EditWorkoutExerciseUiState(
                    isLoading = false,
                    exerciseName =
                        exercise.exerciseName,
                    trackingType =
                        exercise.trackingType,
                    isPerSide =
                        exercise.isPerSide,
                    targetSets =
                        exercise.targetSets.toString(),
                    targetValue =
                        targetValue,
                    hasExternalLoad =
                        exercise.defaultWeightKg != null,
                    externalLoadKg =
                        weightText,
                    restBetweenSetsSec =
                        exercise.restBetweenSetsSec
                            .toString(),
                    restBeforeNextSec =
                        exercise.restBeforeNextSec
                            .toString()
                )
        }
    }

    fun setTargetSets(value: String) {
        _uiState.value =
            _uiState.value.copy(
                targetSets = value,
                errorMessage = null
            )
    }

    fun setTargetValue(value: String) {
        _uiState.value =
            _uiState.value.copy(
                targetValue = value,
                errorMessage = null
            )
    }

    fun setHasExternalLoad(value: Boolean) {
        _uiState.value =
            _uiState.value.copy(
                hasExternalLoad = value,
                errorMessage = null
            )
    }

    fun setExternalLoadKg(value: String) {
        _uiState.value =
            _uiState.value.copy(
                externalLoadKg = value,
                errorMessage = null
            )
    }

    fun setRestBetweenSetsSec(value: String) {
        _uiState.value =
            _uiState.value.copy(
                restBetweenSetsSec = value,
                errorMessage = null
            )
    }

    fun setRestBeforeNextSec(value: String) {
        _uiState.value =
            _uiState.value.copy(
                restBeforeNextSec = value,
                errorMessage = null
            )
    }

    fun save() {
        val state = _uiState.value

        if (state.isSaving) {
            return
        }

        val targetSets =
            state.targetSets.toIntOrNull()

        if (
            targetSets == null ||
            targetSets <= 0
        ) {
            showError(
                "Sets must be at least 1"
            )
            return
        }

        val targetValue =
            state.targetValue.toIntOrNull()

        if (
            targetValue == null ||
            targetValue <= 0
        ) {
            showError(
                when (state.trackingType) {
                    ExerciseTrackingType.REPS ->
                        "Target reps must be at least 1"

                    ExerciseTrackingType.TIME ->
                        "Target duration must be at least 1 second"
                }
            )
            return
        }

        val restBetween =
            state.restBetweenSetsSec
                .toIntOrNull()

        if (
            restBetween == null ||
            restBetween < 0
        ) {
            showError(
                "Rest between sets cannot be negative"
            )
            return
        }

        val restBeforeNext =
            state.restBeforeNextSec
                .toIntOrNull()

        if (
            restBeforeNext == null ||
            restBeforeNext < 0
        ) {
            showError(
                "Rest before next exercise cannot be negative"
            )
            return
        }

        val externalLoad =
            if (state.hasExternalLoad) {
                state.externalLoadKg
                    .toFloatOrNull()
            } else {
                null
            }

        if (
            state.hasExternalLoad &&
            (
                    externalLoad == null ||
                            externalLoad <= 0f
                    )
        ) {
            showError(
                "External load must be greater than 0kg"
            )
            return
        }

        val targetReps =
            when (state.trackingType) {
                ExerciseTrackingType.REPS ->
                    targetValue

                ExerciseTrackingType.TIME ->
                    0
            }

        val targetDuration =
            when (state.trackingType) {
                ExerciseTrackingType.REPS ->
                    null

                ExerciseTrackingType.TIME ->
                    targetValue
            }

        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(
                    isSaving = true,
                    errorMessage = null
                )

            val success =
                repository
                    .updateWorkoutExerciseConfiguration(
                        workoutExerciseId =
                            workoutExerciseId,
                        targetSets =
                            targetSets,
                        targetReps =
                            targetReps,
                        targetDurationSec =
                            targetDuration,
                        defaultWeightKg =
                            externalLoad,
                        restBetweenSetsSec =
                            restBetween,
                        restBeforeNextSec =
                            restBeforeNext
                    )

            _uiState.value =
                _uiState.value.copy(
                    isSaving = false,
                    saveComplete = success,
                    errorMessage =
                        if (success) {
                            null
                        } else {
                            "Unable to save changes"
                        }
                )
        }
    }

    private fun showError(
        message: String
    ) {
        _uiState.value =
            _uiState.value.copy(
                errorMessage = message
            )
    }
}