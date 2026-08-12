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

    private val _percentChange = MutableStateFlow<Double?>(null)
    val percentChange: StateFlow<Double?> = _percentChange.asStateFlow()

    init {
        viewModelScope.launch {
            _stats.value = repository.getExerciseDetailStats(exerciseId)

            val volumes = repository.getAllExerciseSessionVolumes()
                .filter { it.exerciseId == exerciseId }
                .sortedByDescending { it.startedAt }

            if (volumes.size >= 6) {
                val recent3 = volumes.subList(0, 3).map { it.sessionVolume }.average()
                val earlier3 = volumes.subList(3, 6).map { it.sessionVolume }.average()
                _percentChange.value = if (earlier3 == 0.0) 0.0 else ((recent3 - earlier3) / earlier3) * 100.0
            }
        }
    }
}