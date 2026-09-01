package com.chani.reptap.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chani.reptap.data.local.dao.ExerciseSessionVolume
import com.chani.reptap.data.repository.WorkoutRepository
import com.chani.reptap.data.local.dao.ExerciseTrendSession
import com.chani.reptap.data.local.entity.ExerciseTrackingType
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import java.time.ZoneId
import java.time.DayOfWeek
import java.time.Instant
import java.time.temporal.TemporalAdjusters

data class ProgressSummary(
    val totalSessions: Int,
    val sessionsThisWeek: Int,
    val averageDurationMillis: Long,
    val averageSessionsPerWeek: Double
)

data class ExerciseImprovement(
    val exerciseId: Long,
    val exerciseName: String,
    val percentChange: Double
)

class ProgressViewModel(private val repository: WorkoutRepository) : ViewModel() {

    private val _summary = MutableStateFlow<ProgressSummary?>(null)
    val summary: StateFlow<ProgressSummary?> = _summary.asStateFlow()

    private val _exerciseImprovements = MutableStateFlow<List<ExerciseImprovement>>(emptyList())
    val exerciseImprovements: StateFlow<List<ExerciseImprovement>> = _exerciseImprovements.asStateFlow()

    init {
        viewModelScope.launch {
            repository.completedSessionRecaps
                .distinctUntilChanged()
                .collect { sessions ->
                    refreshProgress(sessions)
                }
        }
    }

    fun seedFakeDataForTesting() {
        viewModelScope.launch {
            repository.seedFakeProgressData()
        }
    }

    private suspend fun refreshProgress(
        sessions: List<com.chani.reptap.data.local.dao.SessionRecap>
    ) {
        val (totalSessions, avgDuration, firstSessionDate) = coroutineScope {
            val totalSessionsDeferred = async {
                repository.getTotalSessions()
            }

            val avgDurationDeferred = async {
                repository.getAverageDurationMillis() ?: 0L
            }

            val firstSessionDateDeferred = async {
                repository.getFirstSessionDate()
            }

            Triple(
                totalSessionsDeferred.await(),
                avgDurationDeferred.await(),
                firstSessionDateDeferred.await()
            )
        }

        val avgSessionsPerWeek =
            calculateAverageSessionsPerWeek(
                totalSessions,
                firstSessionDate
            )

        val sessionsThisWeek =
            calculateSessionsThisWeek(sessions)

        _summary.value = ProgressSummary(
            totalSessions = totalSessions,
            sessionsThisWeek = sessionsThisWeek,
            averageDurationMillis = avgDuration,
            averageSessionsPerWeek = avgSessionsPerWeek
        )

        val trendSessions =
            repository.getAllExerciseTrendSessions()

        _exerciseImprovements.value =
            withContext(Dispatchers.Default) {
                calculateExerciseImprovements(trendSessions)
            }
    }

    private fun calculateSessionsThisWeek(
        sessions: List<com.chani.reptap.data.local.dao.SessionRecap>
    ): Int {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)

        val startOfWeek =
            today.with(
                TemporalAdjusters.previousOrSame(
                    DayOfWeek.MONDAY
                )
            )

        val endOfWeek =
            today.with(
                TemporalAdjusters.nextOrSame(
                    DayOfWeek.SUNDAY
                )
            )

        return sessions.count { session ->
            val sessionDate =
                Instant.ofEpochMilli(session.startedAt)
                    .atZone(zoneId)
                    .toLocalDate()

            !sessionDate.isBefore(startOfWeek) &&
                    !sessionDate.isAfter(endOfWeek)
        }
    }

    private fun calculateAverageSessionsPerWeek(
        totalSessions: Int,
        firstSessionDate: Long?
    ): Double {
        if (firstSessionDate == null || totalSessions == 0) {
            return 0.0
        }

        val daysSinceStart = daysSince(firstSessionDate)

        val weeksSinceStart =
            maxOf(daysSinceStart / 7.0, 1.0)

        return totalSessions / weeksSinceStart
    }

    private fun daysSince(epochMillis: Long): Double {
        val startDate = LocalDate.ofEpochDay(epochMillis / (1000 * 60 * 60 * 24))
        val today = LocalDate.now(ZoneId.systemDefault())
        val days = java.time.temporal.ChronoUnit.DAYS.between(startDate, today)
        return maxOf(days.toDouble(), 1.0)
    }

    private fun calculateExerciseImprovements(
        sessions: List<ExerciseTrendSession>
    ): List<ExerciseImprovement> {
        val grouped =
            sessions.groupBy {
                Triple(
                    it.exerciseId,
                    it.exerciseName,
                    it.trackingType
                )
            }

        val results =
            mutableListOf<ExerciseImprovement>()

        for ((key, exerciseSessions) in grouped) {
            val (
                exerciseId,
                exerciseName,
                trackingType
            ) = key

            val sorted =
                exerciseSessions.sortedByDescending {
                    it.startedAt
                }

            if (sorted.size < 6) {
                continue
            }

            val recentValues: List<Double>
            val earlierValues: List<Double>

            when (trackingType) {
                ExerciseTrackingType.REPS -> {
                    recentValues =
                        sorted
                            .take(3)
                            .map { it.sessionVolume }

                    earlierValues =
                        sorted
                            .drop(3)
                            .take(3)
                            .map { it.sessionVolume }
                }

                ExerciseTrackingType.TIME -> {
                    recentValues =
                        sorted
                            .take(3)
                            .mapNotNull {
                                it.averageDurationSec
                            }

                    earlierValues =
                        sorted
                            .drop(3)
                            .take(3)
                            .mapNotNull {
                                it.averageDurationSec
                            }

                    if (
                        recentValues.size < 3 ||
                        earlierValues.size < 3
                    ) {
                        continue
                    }
                }
            }

            val recentAverage =
                recentValues.average()

            val earlierAverage =
                earlierValues.average()

            val percentChange =
                if (earlierAverage == 0.0) {
                    0.0
                } else {
                    (
                            (recentAverage - earlierAverage) /
                                    earlierAverage
                            ) * 100.0
                }

            results.add(
                ExerciseImprovement(
                    exerciseId = exerciseId,
                    exerciseName = exerciseName,
                    percentChange = percentChange
                )
            )
        }

        return results.sortedByDescending {
            it.percentChange
        }
    }
}