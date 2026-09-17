package com.chani.reptap.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chani.reptap.data.local.dao.ExerciseDetailStats
import com.chani.reptap.data.local.entity.ExerciseTrackingType
import com.chani.reptap.data.local.dao.ExerciseSessionDataPoint
import com.chani.reptap.data.local.dao.ExerciseSetDetail
import com.chani.reptap.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

class ExerciseDetailViewModel(
    private val repository: WorkoutRepository,
    private val exerciseHistoryKey: String
) : ViewModel() {

    private val _stats = MutableStateFlow<ExerciseDetailStats?>(null)
    val stats: StateFlow<ExerciseDetailStats?> = _stats.asStateFlow()

    private val _percentChange = MutableStateFlow<Double?>(null)
    val percentChange: StateFlow<Double?> = _percentChange.asStateFlow()

    private val _monthlyChartData = MutableStateFlow<Map<YearMonth, List<ExerciseSessionDataPoint>>>(emptyMap())
    val monthlyChartData: StateFlow<Map<YearMonth, List<ExerciseSessionDataPoint>>> = _monthlyChartData.asStateFlow()

    private val _setDetailsBySession = MutableStateFlow<Map<Long, List<ExerciseSetDetail>>>(emptyMap())
    val setDetailsBySession: StateFlow<Map<Long, List<ExerciseSetDetail>>> = _setDetailsBySession.asStateFlow()

    init {
        viewModelScope.launch {
            val exerciseStats =
                repository.getExerciseDetailStats(
                    exerciseHistoryKey
                )

            _stats.value = exerciseStats

            val points =
                repository.getExerciseSessionDataPoints(
                    exerciseHistoryKey
                )

            when (exerciseStats?.trackingType) {
                ExerciseTrackingType.TIME -> {
                    val timedSessions = points
                        .filter { it.avgDurationSec != null }
                        .sortedByDescending { it.startedAt }

                    if (timedSessions.size >= 6) {
                        val recent3 = timedSessions
                            .take(3)
                            .mapNotNull { it.avgDurationSec }
                            .average()

                        val earlier3 = timedSessions
                            .drop(3)
                            .take(3)
                            .mapNotNull { it.avgDurationSec }
                            .average()

                        _percentChange.value =
                            if (earlier3 == 0.0) {
                                0.0
                            } else {
                                ((recent3 - earlier3) / earlier3) * 100.0
                            }
                    }
                }

                ExerciseTrackingType.REPS -> {
                    val volumes = repository
                        .getAllExerciseSessionVolumes()
                        .filter {
                            it.exerciseHistoryKey ==
                                    exerciseHistoryKey
                        }
                        .sortedByDescending { it.startedAt }

                    if (volumes.size >= 6) {
                        val recent3 = volumes
                            .take(3)
                            .map { it.sessionVolume }
                            .average()

                        val earlier3 = volumes
                            .drop(3)
                            .take(3)
                            .map { it.sessionVolume }
                            .average()

                        _percentChange.value =
                            if (earlier3 == 0.0) {
                                0.0
                            } else {
                                ((recent3 - earlier3) / earlier3) * 100.0
                            }
                    }
                }

                null -> Unit
            }
            val grouped = points.groupBy { point ->
                val localDate = Instant.ofEpochMilli(point.startedAt).atZone(ZoneId.systemDefault()).toLocalDate()
                YearMonth.from(localDate)
            }

            _monthlyChartData.value = if (grouped.isEmpty()) {
                emptyMap()
            } else {
                val currentRealMonth = YearMonth.now()
                val firstMonth = minOf(grouped.keys.min(), currentRealMonth).minusMonths(1)
                val lastMonth = maxOf(grouped.keys.max(), currentRealMonth).plusMonths(1)
                val allMonths = generateSequence(firstMonth) { it.plusMonths(1) }
                    .takeWhile { !it.isAfter(lastMonth) }
                allMonths.associateWith { month -> grouped[month].orEmpty() }
            }

            _setDetailsBySession.value = repository.getExerciseSetDetails(
                exerciseHistoryKey
            )
                .groupBy { it.sessionLogId }
        }
    }
}