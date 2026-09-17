package com.chani.reptap.ui.builder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chani.reptap.data.local.dao.WorkoutExerciseWithName
import com.chani.reptap.data.local.entity.WorkoutDay
import com.chani.reptap.data.repository.DeleteWorkoutResult
import com.chani.reptap.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class WorkoutBuilderViewModel(
    private val repository: WorkoutRepository,
    private val dayId: Long
) : ViewModel() {

    private val _isSavingOrder =
        MutableStateFlow(false)

    val isSavingOrder =
        _isSavingOrder.asStateFlow()

    private val _orderSaveError =
        MutableStateFlow<String?>(null)

    val orderSaveError =
        _orderSaveError.asStateFlow()

    private val _isDuplicating =
        MutableStateFlow(false)

    val isDuplicating =
        _isDuplicating.asStateFlow()

    private val _duplicateError =
        MutableStateFlow<String?>(null)

    val duplicateError =
        _duplicateError.asStateFlow()

    private val _isDeleting =
        MutableStateFlow(false)

    val isDeleting =
        _isDeleting.asStateFlow()

    private val _deleteError =
        MutableStateFlow<String?>(null)

    val deleteError =
        _deleteError.asStateFlow()

    val workoutDay: StateFlow<WorkoutDay?> =
        repository.workoutDays
            .map { days ->
                days.firstOrNull { it.id == dayId }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )

    val exercises: StateFlow<List<WorkoutExerciseWithName>> =
        repository
            .getExercisesForDayWithNames(dayId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    suspend fun saveExerciseOrder(
        orderedWorkoutExerciseIds: List<Long>
    ): Boolean {
        if (_isSavingOrder.value) {
            return false
        }

        _isSavingOrder.value = true
        _orderSaveError.value = null

        return try {
            val success =
                repository.reorderWorkoutExercises(
                    dayId =
                        dayId,
                    orderedWorkoutExerciseIds =
                        orderedWorkoutExerciseIds
                )

            if (!success) {
                _orderSaveError.value =
                    "Unable to save exercise order"
            }

            success
        } finally {
            _isSavingOrder.value = false
        }
    }

    suspend fun renameWorkout(
        newName: String
    ): Boolean {
        return repository.renameWorkout(
            dayId = dayId,
            newName = newName
        )
    }

    suspend fun duplicateWorkout(
        newName: String
    ): Long? {
        if (_isDuplicating.value) {
            return null
        }

        val trimmedName =
            newName.trim()

        if (trimmedName.isBlank()) {
            _duplicateError.value =
                "Workout name cannot be empty"

            return null
        }

        _isDuplicating.value =
            true

        _duplicateError.value =
            null

        return try {
            val newDayId =
                repository.duplicateWorkout(
                    sourceDayId =
                        dayId,
                    newName =
                        trimmedName
                )

            if (newDayId == null) {
                _duplicateError.value =
                    "Unable to duplicate workout. " +
                            "Choose a different name."
            }

            newDayId
        } catch (
            exception: Exception
        ) {
            _duplicateError.value =
                "Unable to duplicate workout"

            null
        } finally {
            _isDuplicating.value =
                false
        }
    }

    suspend fun deleteWorkout():
            Boolean {

        if (
            _isDeleting.value ||
            _isDuplicating.value
        ) {
            return false
        }

        _isDeleting.value =
            true

        _deleteError.value =
            null

        return try {
            when (
                repository.deleteWorkout(
                    dayId
                )
            ) {
                DeleteWorkoutResult.SUCCESS -> {
                    true
                }

                DeleteWorkoutResult.LAST_WORKOUT -> {
                    _deleteError.value =
                        "At least one workout must remain in the training cycle."

                    false
                }

                DeleteWorkoutResult.RECOVERY_DAY -> {
                    _deleteError.value =
                        "Recovery days cannot be deleted here."

                    false
                }

                DeleteWorkoutResult.NOT_FOUND -> {
                    _deleteError.value =
                        "Workout no longer exists."

                    false
                }
            }
        } catch (
            exception: Exception
        ) {
            _deleteError.value =
                "Unable to delete workout"

            false
        } finally {
            _isDeleting.value =
                false
        }
    }

    fun clearDeleteError() {
        _deleteError.value =
            null
    }

    fun clearDuplicateError() {
        _duplicateError.value = null
    }

    fun clearOrderSaveError() {
        _orderSaveError.value = null
    }
}