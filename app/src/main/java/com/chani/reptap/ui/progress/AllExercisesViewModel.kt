package com.chani.reptap.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chani.reptap.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExerciseListItem(
    val id: Long,
    val name: String
)

class AllExercisesViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _groupedExercises = MutableStateFlow<Map<Char, List<ExerciseListItem>>>(emptyMap())
    val groupedExercises: StateFlow<Map<Char, List<ExerciseListItem>>> = _groupedExercises.asStateFlow()

    init {
        viewModelScope.launch {
            repository.exercises.collect { exercises ->
                _groupedExercises.value = exercises
                    .map { ExerciseListItem(it.id, it.name) }
                    .groupBy { it.name.first().uppercaseChar() }
                    .toSortedMap()
            }
        }
    }
}