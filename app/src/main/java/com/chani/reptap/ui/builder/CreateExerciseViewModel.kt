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

data class CreateExerciseUiState(
    val name: String = "",
    val primaryMuscleId: Long? = null,
    val trackingType: ExerciseTrackingType =
        ExerciseTrackingType.REPS,
    val isBodyweight: Boolean = false,
    val isPerSide: Boolean = false,

    val isSaving: Boolean = false,
    val saveComplete: Boolean = false,
    val errorMessage: String? = null
)

class CreateExerciseViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    val muscleGroups =
        repository.muscleGroups
            .stateIn(
                scope = viewModelScope,
                started =
                    SharingStarted
                        .WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    private val _uiState =
        MutableStateFlow(
            CreateExerciseUiState()
        )

    val uiState:
            StateFlow<CreateExerciseUiState> =
        _uiState.asStateFlow()

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
                primaryMuscleId = muscleId,
                errorMessage = null
            )
    }

    fun setTrackingType(
        trackingType: ExerciseTrackingType
    ) {
        _uiState.value =
            _uiState.value.copy(
                trackingType = trackingType,
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

        if (current.isSaving) {
            return
        }

        if (current.name.trim().isBlank()) {
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

            val newExerciseId =
                repository.createExercise(
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

            _uiState.value =
                _uiState.value.copy(
                    isSaving = false,
                    saveComplete =
                        newExerciseId != null,
                    errorMessage =
                        if (newExerciseId == null) {
                            "Unable to create exercise"
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