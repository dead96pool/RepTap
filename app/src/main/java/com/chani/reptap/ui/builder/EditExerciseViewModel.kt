package com.chani.reptap.ui.builder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chani.reptap.data.local.entity.ExerciseTrackingType
import com.chani.reptap.data.repository.UpdateExerciseResult
import com.chani.reptap.data.repository.WorkoutRepository
import com.chani.reptap.data.repository.DeleteExerciseResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EditExerciseUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,

    val saveComplete: Boolean = false,
    val deleteComplete: Boolean = false,

    val name: String = "",
    val primaryMuscleId: Long? = null,

    val trackingType:
    ExerciseTrackingType =
        ExerciseTrackingType.REPS,

    val isBodyweight: Boolean = false,
    val isPerSide: Boolean = false,

    val workoutUsageNames:
    List<String> =
        emptyList(),

    val errorMessage: String? = null
)

class EditExerciseViewModel(
    private val repository:
    WorkoutRepository,
    private val exerciseId: Long
) : ViewModel() {

    val muscleGroups =
        repository.muscleGroups
            .stateIn(
                scope = viewModelScope,
                started =
                    SharingStarted
                        .WhileSubscribed(5_000),
                initialValue =
                    emptyList()
            )

    private val _uiState =
        MutableStateFlow(
            EditExerciseUiState()
        )

    val uiState:
            StateFlow<EditExerciseUiState> =
        _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {

            val exercise =
                repository.getExercise(
                    exerciseId
                )

            if (exercise == null) {
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        errorMessage =
                            "Exercise not found"
                    )

                return@launch
            }

            val usages =
                repository
                    .getExerciseWorkoutUsages(
                        exerciseId
                    )

            _uiState.value =
                EditExerciseUiState(
                    isLoading = false,

                    name =
                        exercise.name,

                    primaryMuscleId =
                        exercise.primaryMuscleId,

                    trackingType =
                        exercise.trackingType,

                    isBodyweight =
                        exercise.isBodyweight,

                    isPerSide =
                        exercise.isPerSide,

                    workoutUsageNames =
                        usages
                            .map {
                                it.workoutDayName
                            }
                )
        }
    }

    fun setName(
        value: String
    ) {
        _uiState.value =
            _uiState.value.copy(
                name = value,
                errorMessage = null
            )
    }

    fun setPrimaryMuscleId(
        muscleId: Long
    ) {
        _uiState.value =
            _uiState.value.copy(
                primaryMuscleId =
                    muscleId,
                errorMessage = null
            )
    }

    fun setIsBodyweight(
        value: Boolean
    ) {
        _uiState.value =
            _uiState.value.copy(
                isBodyweight = value,
                errorMessage = null
            )
    }

    fun setIsPerSide(
        value: Boolean
    ) {
        _uiState.value =
            _uiState.value.copy(
                isPerSide = value,
                errorMessage = null
            )
    }

    fun save() {

        val current =
            _uiState.value

        if (
            current.isLoading ||
            current.isSaving
        ) {
            return
        }

        if (
            current.name
                .trim()
                .isBlank()
        ) {
            showError(
                "Exercise name cannot be empty"
            )
            return
        }

        val muscleId =
            current.primaryMuscleId

        if (muscleId == null) {
            showError(
                "Select a primary muscle"
            )
            return
        }

        viewModelScope.launch {

            _uiState.value =
                current.copy(
                    isSaving = true,
                    errorMessage = null
                )

            val result =
                repository
                    .updateExerciseDefinition(
                        exerciseId =
                            exerciseId,

                        name =
                            current.name,

                        primaryMuscleId =
                            muscleId,

                        trackingType =
                            current.trackingType,

                        isBodyweight =
                            current.isBodyweight,

                        isPerSide =
                            current.isPerSide
                    )

            when (result) {

                UpdateExerciseResult.SUCCESS -> {
                    _uiState.value =
                        _uiState.value.copy(
                            isSaving = false,
                            saveComplete = true
                        )
                }

                UpdateExerciseResult.NOT_FOUND -> {
                    showSaveError(
                        "Exercise no longer exists"
                    )
                }

                UpdateExerciseResult.INVALID_NAME -> {
                    showSaveError(
                        "Exercise name cannot be empty"
                    )
                }

                UpdateExerciseResult.MUSCLE_NOT_FOUND -> {
                    showSaveError(
                        "Selected muscle no longer exists"
                    )
                }

                UpdateExerciseResult
                    .TRACKING_TYPE_CHANGE_REQUIRES_NEW_IDENTITY -> {

                    showSaveError(
                        "Changing between reps and time requires a new exercise identity"
                    )
                }
            }
        }
    }

    fun delete() {

        val current =
            _uiState.value

        if (
            current.isLoading ||
            current.isSaving ||
            current.isDeleting
        ) {
            return
        }

        viewModelScope.launch {

            _uiState.value =
                current.copy(
                    isDeleting = true,
                    errorMessage = null
                )

            when (
                repository.deleteExercise(
                    exerciseId
                )
            ) {

                DeleteExerciseResult.SUCCESS -> {

                    _uiState.value =
                        _uiState.value.copy(
                            isDeleting = false,
                            deleteComplete = true
                        )
                }

                DeleteExerciseResult.NOT_FOUND -> {

                    showDeleteError(
                        "Exercise no longer exists"
                    )
                }

                DeleteExerciseResult.IN_USE -> {

                    /*
                     * Refresh the usage list in case the
                     * current workout configuration changed
                     * after this screen was opened.
                     */
                    val usages =
                        repository
                            .getExerciseWorkoutUsages(
                                exerciseId
                            )

                    _uiState.value =
                        _uiState.value.copy(
                            isDeleting = false,

                            workoutUsageNames =
                                usages.map {
                                    it.workoutDayName
                                },

                            errorMessage =
                                "Remove this exercise from its current workouts before deleting it."
                        )
                }
            }
        }
    }

    private fun showSaveError(
        message: String
    ) {
        _uiState.value =
            _uiState.value.copy(
                isSaving = false,
                errorMessage = message
            )
    }

    private fun showDeleteError(
        message: String
    ) {
        _uiState.value =
            _uiState.value.copy(
                isDeleting = false,
                errorMessage = message
            )
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