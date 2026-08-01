package com.chani.reptap.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chani.reptap.data.local.dao.MuscleProgress
import com.chani.reptap.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.TimeUnit

class ProgressViewModel(repository: WorkoutRepository) : ViewModel() {
    private val sevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
    private val now = System.currentTimeMillis()

    val muscleProgress: StateFlow<List<MuscleProgress>> =
        repository.getMuscleProgress(sevenDaysAgo, now).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}