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

data class AddWorkoutExerciseUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveComplete: Boolean = false,

    val exerciseName: String = "",
    val trackingType: ExerciseTrackingType =
        ExerciseTrackingType.REPS,
    val isPerSide: Boolean = false,
    val isBodyweight: Boolean = false,

    val targetSets: String = "",
    val targetValue: String = "",

    val hasExternalLoad: Boolean = false,
    val externalLoadKg: String = "",

    val restBetweenSetsSec: String = "",
    val restBeforeNextSec: String = "",

    val errorMessage: String? = null
)

class AddWorkoutExerciseViewModel(
    private val repository: WorkoutRepository,
    private val dayId: Long,
    private val exerciseId: Long
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            AddWorkoutExerciseUiState()
        )

    val uiState:
            StateFlow<AddWorkoutExerciseUiState> =
        _uiState.asStateFlow()

    init {
        loadExercise()
    }

    private fun loadExercise() {
        viewModelScope.launch {
            val exercise =
                repository.exercises
                    .first()
                    .firstOrNull {
                        it.id == exerciseId
                    }

            if (exercise == null) {
                _uiState.value =
                    AddWorkoutExerciseUiState(
                        isLoading = false,
                        errorMessage =
                            "Exercise not found"
                    )

                return@launch
            }

            _uiState.value =
                AddWorkoutExerciseUiState(
                    isLoading = false,
                    exerciseName =
                        exercise.name,
                    trackingType =
                        exercise.trackingType,
                    isPerSide =
                        exercise.isPerSide,
                    isBodyweight =
                        exercise.isBodyweight,

                    /*
                     * Do not invent workout targets or rest
                     * settings for a newly added exercise.
                     * The user explicitly configures them.
                     */
                    targetSets = "",
                    targetValue = "",

                    /*
                     * Non-bodyweight exercises default to
                     * external-load mode, but no weight value
                     * is assumed.
                     */
                    hasExternalLoad =
                        !exercise.isBodyweight,

                    externalLoadKg = "",
                    restBetweenSetsSec = "",
                    restBeforeNextSec = ""
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
        val state =
            _uiState.value

        if (state.isSaving) {
            return
        }

        val sets =
            state.targetSets.toIntOrNull()

        if (
            sets == null ||
            sets <= 0
        ) {
            showError(
                "Sets must be at least 1"
            )
            return
        }

        val target =
            state.targetValue.toIntOrNull()

        if (
            target == null ||
            target <= 0
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

        val weight =
            if (state.hasExternalLoad) {
                state.externalLoadKg
                    .toFloatOrNull()
            } else {
                null
            }

        if (
            state.hasExternalLoad &&
            (
                    weight == null ||
                            weight <= 0f
                    )
        ) {
            showError(
                "External load must be greater than 0kg"
            )
            return
        }

        val targetReps =
            if (
                state.trackingType ==
                ExerciseTrackingType.REPS
            ) {
                target
            } else {
                0
            }

        val targetDuration =
            if (
                state.trackingType ==
                ExerciseTrackingType.TIME
            ) {
                target
            } else {
                null
            }

        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(
                    isSaving = true,
                    errorMessage = null
                )

            val success =
                repository.addWorkoutExercise(
                    dayId = dayId,
                    exerciseId = exerciseId,
                    targetSets = sets,
                    targetReps = targetReps,
                    targetDurationSec =
                        targetDuration,
                    defaultWeightKg = weight,
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
                            "This exercise is already in the workout"
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