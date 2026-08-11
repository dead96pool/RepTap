package com.chani.reptap.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chani.reptap.data.local.dao.ExerciseSessionVolume
import com.chani.reptap.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class ProgressSummary(
    val totalSessions: Int,
    val averageDurationMillis: Long,
    val averageSessionsPerWeek: Double
)

data class ExerciseImprovement(
    val exerciseName: String,
    val percentChange: Double // positive = improved, negative = declined, 0 = neutral
)

class ProgressViewModel(private val repository: WorkoutRepository) : ViewModel() {

    private val _summary = MutableStateFlow<ProgressSummary?>(null)
    val summary: StateFlow<ProgressSummary?> = _summary.asStateFlow()

    private val _exerciseImprovements = MutableStateFlow<List<ExerciseImprovement>>(emptyList())
    val exerciseImprovements: StateFlow<List<ExerciseImprovement>> = _exerciseImprovements.asStateFlow()

    init {
        viewModelScope.launch {
            val totalSessions = repository.getTotalSessions()
            val avgDuration = repository.getAverageDurationMillis() ?: 0L
            val firstSessionDate = repository.getFirstSessionDate()

            val avgSessionsPerWeek = calculateAverageSessionsPerWeek(totalSessions, firstSessionDate)

            _summary.value = ProgressSummary(
                totalSessions = totalSessions,
                averageDurationMillis = avgDuration,
                averageSessionsPerWeek = avgSessionsPerWeek
            )

            val volumes = repository.getAllExerciseSessionVolumes()
            _exerciseImprovements.value = calculateExerciseImprovements(volumes)
        }
    }

    fun seedFakeDataForTesting() {
        viewModelScope.launch {
            repository.seedFakeProgressData()

            val totalSessions = repository.getTotalSessions()
            val avgDuration = repository.getAverageDurationMillis() ?: 0L
            val firstSessionDate = repository.getFirstSessionDate()

            val avgSessionsPerWeek = calculateAverageSessionsPerWeek(totalSessions, firstSessionDate)

            _summary.value = ProgressSummary(
                totalSessions = totalSessions,
                averageDurationMillis = avgDuration,
                averageSessionsPerWeek = avgSessionsPerWeek
            )

            val volumes = repository.getAllExerciseSessionVolumes()
            _exerciseImprovements.value = calculateExerciseImprovements(volumes)
        }
    }

    private fun calculateAverageSessionsPerWeek(totalSessions: Int, firstSessionDate: Long?): Double {
        if (firstSessionDate == null || totalSessions == 0) return 0.0
        val daysSinceStart = daysSince(firstSessionDate)
        val weeksSinceStart = maxOf(daysSinceStart / 7.0, 1.0 / 7.0)
        return totalSessions / weeksSinceStart
    }

    private fun daysSince(epochMillis: Long): Double {
        val startDate = LocalDate.ofEpochDay(epochMillis / (1000 * 60 * 60 * 24))
        val today = LocalDate.now(ZoneId.systemDefault())
        val days = java.time.temporal.ChronoUnit.DAYS.between(startDate, today)
        return maxOf(days.toDouble(), 1.0)
    }

    private fun calculateExerciseImprovements(volumes: List<ExerciseSessionVolume>): List<ExerciseImprovement> {
        val grouped = volumes.groupBy { it.exerciseId to it.exerciseName }
        val results = mutableListOf<ExerciseImprovement>()

        for ((key, sessions) in grouped) {
            val (_, exerciseName) = key
            val sorted = sessions.sortedByDescending { it.startedAt }
            if (sorted.size < 6) continue

            val recent3 = sorted.subList(0, 3).map { it.sessionVolume }.average()
            val earlier3 = sorted.subList(3, 6).map { it.sessionVolume }.average()

            val percentChange = if (earlier3 == 0.0) 0.0 else ((recent3 - earlier3) / earlier3) * 100.0
            results.add(ExerciseImprovement(exerciseName, percentChange))
        }
        return results.sortedByDescending { it.percentChange }
    }
}