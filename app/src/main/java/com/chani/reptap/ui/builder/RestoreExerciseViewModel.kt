package com.chani.reptap.ui.builder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chani.reptap.data.local.entity.ExerciseTrackingType
import com.chani.reptap.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RestoreExerciseUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val restoreComplete: Boolean = false,

    val name: String = "",
    val primaryMuscleId: Long? = null,
    val historicalMuscleName: String = "",

    val trackingType:
    ExerciseTrackingType =
        ExerciseTrackingType.REPS,

    val isBodyweight: Boolean = false,
    val isPerSide: Boolean = false,

    val errorMessage: String? = null
)

class RestoreExerciseViewModel(
    private val repository: WorkoutRepository,
    private val historyKey: String
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
            RestoreExerciseUiState()
        )

    val uiState:
            StateFlow<RestoreExerciseUiState> =
        _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {

        viewModelScope.launch {

            val definition =
                repository
                    .getLatestHistoricalExerciseDefinition(
                        historyKey
                    )

            if (definition == null) {

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        errorMessage =
                            "Historical exercise could not be found"
                    )

                return@launch
            }

            val currentMuscle =
                repository
                    .getMuscleGroupByHistoryKey(
                        definition
                            .primaryMuscleHistoryKey
                    )

            _uiState.value =
                RestoreExerciseUiState(
                    isLoading = false,

                    name =
                        definition.exerciseName,

                    primaryMuscleId =
                        currentMuscle?.id,

                    historicalMuscleName =
                        definition.primaryMuscleName,

                    trackingType =
                        definition.trackingType,

                    isBodyweight =
                        definition.isBodyweight,

                    isPerSide =
                        definition.isPerSide
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

    fun restore() {

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

            val restoredId =
                repository
                    .restoreHistoricalExercise(
                        historyKey =
                            historyKey,

                        name =
                            current.name,

                        primaryMuscleId =
                            muscleId,

                        isBodyweight =
                            current.isBodyweight,

                        isPerSide =
                            current.isPerSide
                    )

            _uiState.value =
                _uiState.value.copy(
                    isSaving = false,

                    restoreComplete =
                        restoredId != null,

                    errorMessage =
                        if (
                            restoredId == null
                        ) {
                            "Unable to restore exercise"
                        } else {
                            null
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