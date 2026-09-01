package com.chani.reptap.ui.builder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chani.reptap.data.local.entity.Exercise
import com.chani.reptap.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class AddExercisePickerViewModel(
    repository: WorkoutRepository,
    dayId: Long
) : ViewModel() {

    val availableExercises:
            StateFlow<List<Exercise>> =
        combine(
            repository.exercises,
            repository
                .getExercisesForDayWithNames(dayId)
        ) { allExercises, activeExercises ->

            val activeExerciseIds =
                activeExercises
                    .map { it.exerciseId }
                    .toSet()

            allExercises.filterNot {
                it.id in activeExerciseIds
            }
        }.stateIn(
            scope = viewModelScope,
            started =
                SharingStarted
                    .WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}