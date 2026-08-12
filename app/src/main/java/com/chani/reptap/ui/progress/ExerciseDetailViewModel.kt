package com.chani.reptap.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chani.reptap.data.local.dao.ExerciseDetailStats
import com.chani.reptap.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExerciseDetailViewModel(
    private val repository: WorkoutRepository,
    private val exerciseId: Long
) : ViewModel() {

    private val _stats = MutableStateFlow<ExerciseDetailStats?>(null)
    val stats: StateFlow<ExerciseDetailStats?> = _stats.asStateFlow()

    init {
        viewModelScope.launch {
            _stats.value = repository.getExerciseDetailStats(exerciseId)
        }
    }
}