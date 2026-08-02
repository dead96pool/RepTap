package com.chani.reptap.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chani.reptap.data.local.dao.WorkoutExerciseWithName
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

    private val _todayExercises = MutableStateFlow<List<WorkoutExerciseWithName>>(emptyList())
    val todayExercises: StateFlow<List<WorkoutExerciseWithName>> = _todayExercises.asStateFlow()

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
            val day = cycleRepository.getCurrentDay()
            _todayWorkout.value = day
            if (day != null && !day.isRecovery) {
                workoutRepository.getExercisesForDayWithNames(day.id).collect { list ->
                    _todayExercises.value = list
                    _isLoading.value = false
                    return@collect
                }
            } else {
                _todayExercises.value = emptyList()
                _isLoading.value = false
            }
        }
    }

    fun selectDayManually(day: WorkoutDay) {
        viewModelScope.launch {
            cycleRepository.setSlotManually(day.slotNumber)
            loadToday()
        }
    }
}