package com.chani.reptap.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chani.reptap.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProgressSummary(
    val exercisesImproved: Int,
    val personalRecords: Int,
    val totalSessions: Int,
    val averageDurationMillis: Long,
    val longestStreak: Int
)

class ProgressViewModel(private val repository: WorkoutRepository) : ViewModel() {

    private val _summary = MutableStateFlow<ProgressSummary?>(null)
    val summary: StateFlow<ProgressSummary?> = _summary.asStateFlow()

    init {
        viewModelScope.launch {
            val totalSessions = repository.getTotalSessions()
            val avgDuration = repository.getAverageDurationMillis() ?: 0L
            val volumes = repository.getAllExerciseSessionVolumes()
            val exercisesImproved = calculateExercisesImproved(volumes)
            val personalRecords = calculatePersonalRecords(volumes)
            val streak = calculateLongestStreak(
                repository.getAllCompletedSessionsOrdered(),
                repository.getWorkoutDaySlotMap()
            )

            _summary.value = ProgressSummary(
                exercisesImproved = exercisesImproved,
                personalRecords = personalRecords,
                totalSessions = totalSessions,
                averageDurationMillis = avgDuration,
                longestStreak = streak
            )
        }
    }

    private fun calculateExercisesImproved(
        volumes: List<com.chani.reptap.data.local.dao.ExerciseSessionVolume>
    ): Int {
        val grouped = volumes.groupBy { it.exerciseId }
        var improvedCount = 0

        for ((_, sessions) in grouped) {
            val sorted = sessions.sortedByDescending { it.startedAt }
            if (sorted.size < 6) continue

            val recent3 = sorted.subList(0, 3).map { it.sessionVolume }.average()
            val earlier3 = sorted.subList(3, 6).map { it.sessionVolume }.average()

            if (recent3 > earlier3) improvedCount++
        }
        return improvedCount
    }

    private fun calculatePersonalRecords(
        volumes: List<com.chani.reptap.data.local.dao.ExerciseSessionVolume>
    ): Int {
        val grouped = volumes.groupBy { it.exerciseId }
        var prCount = 0

        for ((_, sessions) in grouped) {
            if (sessions.isEmpty()) continue

            val bestVolume = sessions.maxOf { it.sessionVolume }
            val latestSession = sessions.maxByOrNull { it.startedAt }

            if (latestSession != null && latestSession.sessionVolume == bestVolume) {
                prCount++
            }
        }
        return prCount
    }

    private fun calculateLongestStreak(
        sessions: List<com.chani.reptap.data.local.dao.SessionDateAndDay>,
        slotMap: List<com.chani.reptap.data.local.dao.WorkoutDaySlot>
    ): Int {
        if (sessions.isEmpty()) return 0

        val idToSlot = slotMap.associate { it.id to it.slotNumber }
        val maxSlot = slotMap.maxOfOrNull { it.slotNumber } ?: return 0

        val sorted = sessions.sortedBy { it.startedAt }
        var longest = 1
        var current = 1

        for (i in 1 until sorted.size) {
            val prevSlot = idToSlot[sorted[i - 1].workoutDayId] ?: continue
            val currSlot = idToSlot[sorted[i].workoutDayId] ?: continue
            val expectedNextSlot = if (prevSlot == maxSlot) 1 else prevSlot + 1

            current = if (currSlot == expectedNextSlot) current + 1 else 1
            longest = maxOf(longest, current)
        }
        return longest
    }
}