package com.chani.reptap.ui.builder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chani.reptap.data.local.entity.Exercise
import com.chani.reptap.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class AddExerciseSearchItemKind {
    CURRENT,
    HISTORICAL_ONLY,
    CREATE_NEW
}

data class AddExerciseSearchItem(
    val key: String,
    val kind: AddExerciseSearchItemKind,
    val name: String,
    val exercise: Exercise? = null,
    val isAlreadyInWorkout: Boolean = false
)

class AddExercisePickerViewModel(
    repository: WorkoutRepository,
    dayId: Long
) : ViewModel() {

    private val _searchQuery =
        MutableStateFlow("")

    val searchQuery:
            StateFlow<String> =
        _searchQuery.asStateFlow()

    val searchItems:
            StateFlow<List<AddExerciseSearchItem>> =
        combine(
            _searchQuery,
            repository.exercises,
            repository
                .getExercisesForDayWithNames(
                    dayId
                ),
            repository
                .historicalExerciseIdentities
        ) {
                query,
                currentExercises,
                workoutExercises,
                historicalRows ->

            val trimmedQuery =
                query.trim()

            val normalizedQuery =
                trimmedQuery.lowercase()

            val workoutExerciseIds =
                workoutExercises
                    .map {
                        it.exerciseId
                    }
                    .toSet()

            val currentHistoryKeys =
                currentExercises
                    .map {
                        it.historyKey
                    }
                    .toSet()

            fun matches(
                name: String
            ): Boolean {

                if (
                    normalizedQuery
                        .isBlank()
                ) {
                    return true
                }

                return name
                    .lowercase()
                    .contains(
                        normalizedQuery
                    )
            }

            fun matchRank(
                name: String
            ): Int {

                if (
                    normalizedQuery
                        .isBlank()
                ) {
                    return 0
                }

                return if (
                    name
                        .lowercase()
                        .startsWith(
                            normalizedQuery
                        )
                ) {
                    0
                } else {
                    1
                }
            }

            val currentItems =
                currentExercises
                    .asSequence()
                    .filter {
                        matches(
                            it.name
                        )
                    }
                    .map {
                            exercise ->

                        AddExerciseSearchItem(
                            key =
                                "current:" +
                                        exercise.id,

                            kind =
                                AddExerciseSearchItemKind
                                    .CURRENT,

                            name =
                                exercise.name,

                            exercise =
                                exercise,

                            isAlreadyInWorkout =
                                exercise.id in
                                        workoutExerciseIds
                        )
                    }
                    .toList()

            /*
             * historicalRows is newest-first.
             *
             * distinctBy therefore keeps the newest
             * historical display name for each deleted
             * logical Exercise identity.
             */
            val historicalOnlyItems =
                historicalRows
                    .asSequence()
                    .filter {
                        it.exerciseHistoryKey !in
                                currentHistoryKeys
                    }
                    .distinctBy {
                        it.exerciseHistoryKey
                    }
                    .filter {
                        matches(
                            it.exerciseName
                        )
                    }
                    .map {
                            historical ->

                        AddExerciseSearchItem(
                            key =
                                "historical:" +
                                        historical
                                            .exerciseHistoryKey,

                            kind =
                                AddExerciseSearchItemKind
                                    .HISTORICAL_ONLY,

                            name =
                                historical
                                    .exerciseName
                        )
                    }
                    .toList()

            val matchedItems =
                (
                        currentItems +
                                historicalOnlyItems
                        )
                    .sortedWith(
                        compareBy<
                                AddExerciseSearchItem
                                > {
                            matchRank(
                                it.name
                            )
                        }
                            .thenBy {
                                it.name
                                    .lowercase()
                            }
                            .thenBy {
                                when (
                                    it.kind
                                ) {
                                    AddExerciseSearchItemKind
                                        .CURRENT ->
                                        0

                                    AddExerciseSearchItemKind
                                        .HISTORICAL_ONLY ->
                                        1

                                    AddExerciseSearchItemKind
                                        .CREATE_NEW ->
                                        2
                                }
                            }
                    )

            /*
             * Creation is always an explicit choice.
             *
             * Even if this text exactly matches a current
             * or historical Exercise name, Create New must
             * remain available and will receive a fresh
             * historyKey.
             */
            val createItem =
                AddExerciseSearchItem(
                    key =
                        "create:" +
                                trimmedQuery,

                    kind =
                        AddExerciseSearchItemKind
                            .CREATE_NEW,

                    name =
                        trimmedQuery
                )

            matchedItems +
                    createItem
        }
            .stateIn(
                scope = viewModelScope,

                started =
                    SharingStarted
                        .WhileSubscribed(
                            5_000
                        ),

                initialValue =
                    emptyList()
            )

    fun setSearchQuery(
        value: String
    ) {
        _searchQuery.value =
            value
    }
}