package com.chani.reptap.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chani.reptap.data.local.dao.WorkoutExerciseWithName
import com.chani.reptap.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class WorkoutDetailsViewModel(
    repository: WorkoutRepository,
    dayId: Long
) : ViewModel() {
    val exercises: StateFlow<List<WorkoutExerciseWithName>> =
        repository.getExercisesForDayWithNames(dayId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}