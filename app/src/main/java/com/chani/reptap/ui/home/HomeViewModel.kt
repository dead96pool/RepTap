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
import kotlinx.coroutines.flow.first
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

    private val _nextWorkoutDate = MutableStateFlow<java.time.LocalDate?>(null)
    val nextWorkoutDate: StateFlow<java.time.LocalDate?> = _nextWorkoutDate.asStateFlow()

    init {
        viewModelScope.launch {
            workoutRepository.workoutDays.collect { days ->
                _allDays.value = days
            }
        }

        viewModelScope.launch {
            cycleRepository.cycleState.collect {
                loadToday()
            }
        }
    }

    fun formatNextWorkoutDate(date: java.time.LocalDate): String {
        val today = java.time.LocalDate.now()
        return when (date) {
            today -> "Today"
            today.plusDays(1) -> "Tomorrow"
            else -> {
                val formatter = java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM")
                date.format(formatter)
            }
        }
    }

    fun loadToday() {
        viewModelScope.launch {
            _isLoading.value = true

            cycleRepository.refreshScheduleIfOverdue()

            val day = cycleRepository.getCurrentDay()

            _todayWorkout.value = day
            _nextWorkoutDate.value = cycleRepository.getScheduledDateForCurrentSlot()

            _todayExercises.value = if (day != null && !day.isRecovery) {
                workoutRepository
                    .getExercisesForDayWithNames(day.id)
                    .first()
            } else {
                emptyList()
            }

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