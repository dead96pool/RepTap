package com.chani.reptap.ui.builder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chani.reptap.data.local.dao.WorkoutExerciseWithName
import com.chani.reptap.data.local.entity.WorkoutDay
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

    fun clearOrderSaveError() {
        _orderSaveError.value = null
    }
}