package com.chani.reptap.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chani.reptap.data.local.entity.WorkoutDay
import com.chani.reptap.data.repository.CycleRepository
import com.chani.reptap.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val workoutRepository: WorkoutRepository,
    private val cycleRepository: CycleRepository
) : ViewModel() {

    private val _todayWorkout = MutableStateFlow<WorkoutDay?>(null)
    val todayWorkout: StateFlow<WorkoutDay?> = _todayWorkout.asStateFlow()

    private val _allDays = MutableStateFlow<List<WorkoutDay>>(emptyList())
    val allDays: StateFlow<List<WorkoutDay>> = _allDays.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadToday()
        viewModelScope.launch {
            workoutRepository.workoutDays.collect { days ->
                _allDays.value = days
            }
        }
    }

    fun loadToday() {
        viewModelScope.launch {
            _isLoading.value = true
            cycleRepository.autoCompleteRecoveryIfNeeded()
            _todayWorkout.value = cycleRepository.getCurrentDay()
            _isLoading.value = false
        }
    }

    fun selectDayManually(day: WorkoutDay) {
        viewModelScope.launch {
            cycleRepository.setSlotManually(day.slotNumber)
            loadToday()
        }
    }
}