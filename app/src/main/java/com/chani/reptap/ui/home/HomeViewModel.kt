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
import kotlinx.coroutines.Job

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

    private val _isCreatingWorkout =
        MutableStateFlow(false)

    val isCreatingWorkout =
        _isCreatingWorkout.asStateFlow()

    private val _createWorkoutError =
        MutableStateFlow<String?>(null)

    val createWorkoutError =
        _createWorkoutError.asStateFlow()

    private var todayExercisesJob:
            Job? = null

    init {
        viewModelScope.launch {
            workoutRepository.workoutDays
                .collect { days ->

                    _allDays.value =
                        days

                    /*
                     * Keep the current Home WorkoutDay object
                     * synchronized with edits such as Rename.
                     */
                    val currentId =
                        _todayWorkout.value?.id

                    if (currentId != null) {
                        val updatedDay =
                            days.firstOrNull {
                                it.id == currentId
                            }

                        if (updatedDay != null) {
                            _todayWorkout.value =
                                updatedDay
                        }
                    }
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

            cycleRepository
                .refreshScheduleIfOverdue()

            val day =
                cycleRepository
                    .getCurrentDay()

            _todayWorkout.value =
                day

            _nextWorkoutDate.value =
                cycleRepository
                    .getScheduledDateForCurrentSlot()

            /*
             * Stop observing the previously selected day
             * before switching to the new one.
             */
            todayExercisesJob?.cancel()

            _todayExercises.value =
                emptyList()

            if (
                day != null &&
                !day.isRecovery
            ) {
                /*
                 * Keep observing Room rather than taking only
                 * the first value. Workout Builder edits now
                 * update Home automatically.
                 */
                todayExercisesJob =
                    viewModelScope.launch {
                        workoutRepository
                            .getExercisesForDayWithNames(
                                day.id
                            )
                            .collect { exercises ->
                                _todayExercises.value =
                                    exercises
                            }
                    }
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

    suspend fun createWorkout(
        name: String
    ): Long? {
        if (_isCreatingWorkout.value) {
            return null
        }

        val trimmedName =
            name.trim()

        if (trimmedName.isBlank()) {
            _createWorkoutError.value =
                "Workout name cannot be empty"

            return null
        }

        _isCreatingWorkout.value =
            true

        _createWorkoutError.value =
            null

        return try {
            val newDayId =
                workoutRepository
                    .createWorkout(
                        trimmedName
                    )

            if (newDayId == null) {
                _createWorkoutError.value =
                    "Unable to create workout. " +
                            "Choose a different name."
            }

            newDayId
        } catch (
            exception: Exception
        ) {
            _createWorkoutError.value =
                "Unable to create workout"

            null
        } finally {
            _isCreatingWorkout.value =
                false
        }
    }

    fun clearCreateWorkoutError() {
        _createWorkoutError.value =
            null
    }
}