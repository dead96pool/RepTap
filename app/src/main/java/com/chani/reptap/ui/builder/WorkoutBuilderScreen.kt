package com.chani.reptap.ui.builder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chani.reptap.data.local.dao.WorkoutExerciseWithName
import com.chani.reptap.data.local.entity.ExerciseTrackingType
import com.chani.reptap.ui.components.RepTapTopAppBar
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon

@Composable
fun WorkoutBuilderScreen(
    viewModel: WorkoutBuilderViewModel,
    onBack: () -> Unit,
    onExerciseClick:
        (WorkoutExerciseWithName) -> Unit,
    onAddExercise: () -> Unit
) {
    val workoutDay by
    viewModel.workoutDay.collectAsStateWithLifecycle()

    val exercises by
    viewModel.exercises.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            RepTapTopAppBar(
                title = workoutDay?.name ?: "Workout Builder",
                onBack = onBack
            )
        }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 12.dp,
                top =
                    innerPadding.calculateTopPadding() +
                            8.dp,
                end = 12.dp,
                bottom = 12.dp
            ),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Workout Exercises",
                    style =
                        MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                FilledTonalButton(
                    onClick = onAddExercise,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector =
                            Icons.Default.Add,
                        contentDescription = null
                    )

                    Spacer(
                        modifier =
                            Modifier.width(8.dp)
                    )

                    Text("Add Exercise")
                }
            }

            items(
                items = exercises,
                key = { it.id }
            ) { exercise ->
                WorkoutBuilderExerciseCard(
                    exercise = exercise,
                    onClick = {
                        onExerciseClick(exercise)
                    }
                )
            }

            if (
                workoutDay != null &&
                exercises.isEmpty()
            ) {
                item {
                    Text(
                        text = "No exercises in this workout",
                        style =
                            MaterialTheme.typography.bodyMedium,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutBuilderExerciseCard(
    exercise: WorkoutExerciseWithName,
    onClick: () -> Unit
) {
    val targetText =
        when (exercise.trackingType) {
            ExerciseTrackingType.REPS -> {
                "${exercise.targetSets} sets · " +
                        "${exercise.targetReps} reps"
            }

            ExerciseTrackingType.TIME -> {
                "${exercise.targetSets} sets · " +
                        "${exercise.targetDurationSec ?: 0} sec"
            }
        }

    val loadText =
        exercise.defaultWeightKg
            ?.let { weight ->
                if (
                    weight ==
                    weight.toInt().toFloat()
                ) {
                    "${weight.toInt()}kg"
                } else {
                    "${weight}kg"
                }
            }
            ?: "Bodyweight"

    val sideText =
        if (exercise.isPerSide) {
            " · each side"
        } else {
            ""
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick
            )
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 14.dp
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    text =
                        "${exercise.orderIndex}. " +
                                exercise.exerciseName,
                    style =
                        MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector =
                        Icons.AutoMirrored.Filled
                            .KeyboardArrowRight,
                    contentDescription = null,
                    tint =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text =
                    "$targetText · $loadText$sideText",
                style =
                    MaterialTheme.typography.bodyMedium,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )

            Text(
                text =
                    "Rest: " +
                            "${exercise.restBetweenSetsSec}s between sets" +
                            " · " +
                            "${exercise.restBeforeNextSec}s before next",
                style =
                    MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }
    }
}