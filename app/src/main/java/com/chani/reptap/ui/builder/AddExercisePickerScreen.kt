package com.chani.reptap.ui.builder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chani.reptap.data.local.entity.Exercise
import com.chani.reptap.data.local.entity.ExerciseTrackingType
import com.chani.reptap.ui.components.RepTapTopAppBar

@Composable
fun AddExercisePickerScreen(
    viewModel: AddExercisePickerViewModel,
    onBack: () -> Unit,
    onExerciseClick: (Exercise) -> Unit,
    onCreateExercise: () -> Unit,
    onManageExercises: () -> Unit
) {
    val exercises by
    viewModel.availableExercises
        .collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            RepTapTopAppBar(
                title = "Add Exercise",
                onBack = onBack,
                actions = {

                    TextButton(
                        onClick =
                            onManageExercises
                    ) {
                        Text("Manage")
                    }

                    TextButton(
                        onClick =
                            onCreateExercise
                    ) {
                        Text("New")
                    }
                }
            )
        }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top =
                    innerPadding
                        .calculateTopPadding() +
                            8.dp,
                bottom = 12.dp
            )
        ) {
            items(
                items = exercises,
                key = { it.id }
            ) { exercise ->

                ExercisePickerRow(
                    exercise = exercise,
                    onClick = {
                        onExerciseClick(exercise)
                    }
                )

                HorizontalDivider(
                    modifier =
                        Modifier.padding(
                            horizontal = 12.dp
                        )
                )
            }

            if (exercises.isEmpty()) {
                item {
                    Text(
                        text =
                            "All exercises are already in this workout",
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant,
                        modifier =
                            Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExercisePickerRow(
    exercise: Exercise,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick
            )
            .padding(
                horizontal = 12.dp,
                vertical = 14.dp
            ),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = exercise.name,
                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,
                fontWeight =
                    FontWeight.Medium
            )

            Text(
                text = buildString {
                    append(
                        when (
                            exercise.trackingType
                        ) {
                            ExerciseTrackingType.REPS ->
                                "Rep exercise"

                            ExerciseTrackingType.TIME ->
                                "Timed exercise"
                        }
                    )

                    if (exercise.isPerSide) {
                        append(" · each side")
                    }
                },
                style =
                    MaterialTheme
                        .typography
                        .bodySmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }

        Icon(
            imageVector =
                Icons.AutoMirrored.Filled
                    .KeyboardArrowRight,
            contentDescription = null,
            tint =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
    }
}