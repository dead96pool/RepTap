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

class WorkoutBuilderViewModel(
    repository: WorkoutRepository,
    dayId: Long
) : ViewModel() {

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
}