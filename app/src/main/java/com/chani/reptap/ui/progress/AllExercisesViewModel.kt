package com.chani.reptap.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chani.reptap.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class ExerciseListItem(
    val historyKey: String,
    val name: String,

    /*
     * True when historical performance exists for this
     * logical exercise, but there is currently no Exercise
     * row in the active Exercise Library.
     */
    val isHistoricalOnly: Boolean
)

class AllExercisesViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _groupedExercises =
        MutableStateFlow<
                Map<Char, List<ExerciseListItem>>
                >(
            emptyMap()
        )

    val groupedExercises:
            StateFlow<
                    Map<Char, List<ExerciseListItem>>
                    > =
        _groupedExercises.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.exercises,
                repository.historicalExerciseIdentities
            ) {
                    currentExercises,
                    historicalRows ->

                /*
                 * Current Exercise rows are authoritative
                 * for the current display name.
                 */
                val currentByHistoryKey =
                    currentExercises.associateBy {
                        it.historyKey
                    }

                val currentItems =
                    currentExercises.map { exercise ->

                        ExerciseListItem(
                            historyKey =
                                exercise.historyKey,

                            name =
                                exercise.name,

                            isHistoricalOnly =
                                false
                        )
                    }

                /*
                 * historicalRows is ordered newest-first.
                 *
                 * distinctBy therefore keeps the most recent
                 * historical name for an exercise whose
                 * current Exercise row no longer exists.
                 */
                val historicalOnlyItems =
                    historicalRows
                        .asSequence()
                        .filter {
                            it.exerciseHistoryKey !in
                                    currentByHistoryKey
                        }
                        .distinctBy {
                            it.exerciseHistoryKey
                        }
                        .map {
                            ExerciseListItem(
                                historyKey =
                                    it.exerciseHistoryKey,

                                name =
                                    it.exerciseName,

                                isHistoricalOnly =
                                    true
                            )
                        }
                        .toList()

                (
                        currentItems +
                                historicalOnlyItems
                        )
                    .sortedBy {
                        it.name.lowercase()
                    }
                    .groupBy { item ->
                        item.name
                            .firstOrNull()
                            ?.uppercaseChar()
                            ?: '#'
                    }
                    .toSortedMap()
            }
                .collect {
                        grouped ->

                    _groupedExercises.value =
                        grouped
                }
        }
    }
}