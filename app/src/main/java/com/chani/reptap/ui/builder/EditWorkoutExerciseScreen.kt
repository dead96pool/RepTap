package com.chani.reptap.ui.builder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chani.reptap.data.local.entity.ExerciseTrackingType
import com.chani.reptap.ui.components.RepTapTopAppBar
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun EditWorkoutExerciseScreen(
    viewModel: EditWorkoutExerciseViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val state by
    viewModel.uiState
        .collectAsStateWithLifecycle()

    var showRemoveConfirm by
    remember {
        mutableStateOf(false)
    }

    LaunchedEffect(
        state.saveComplete,
        state.removeComplete
    ) {
        if (
            state.saveComplete ||
            state.removeComplete
        ) {
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            RepTapTopAppBar(
                title =
                    if (state.exerciseName.isNotBlank()) {
                        state.exerciseName
                    } else {
                        "Edit Exercise"
                    },
                onBack = onBack
            )
        }
    ) { innerPadding ->

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment =
                    Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 12.dp,
                top =
                    innerPadding.calculateTopPadding() +
                            8.dp,
                end = 12.dp,
                bottom = 16.dp
            ),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            item {
                ExerciseTypeCard(
                    trackingType =
                        state.trackingType,
                    isPerSide =
                        state.isPerSide
                )
            }

            item {
                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier =
                            Modifier.padding(16.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Workout Settings",
                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium,
                            fontWeight =
                                FontWeight.Bold
                        )

                        OutlinedTextField(
                            value =
                                state.targetSets,
                            onValueChange =
                                viewModel::setTargetSets,
                            label = {
                                Text("Sets")
                            },
                            singleLine = true,
                            keyboardOptions =
                                KeyboardOptions(
                                    keyboardType =
                                        KeyboardType.Number
                                ),
                            modifier =
                                Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value =
                                state.targetValue,
                            onValueChange =
                                viewModel::setTargetValue,
                            label = {
                                Text(
                                    when (
                                        state.trackingType
                                    ) {
                                        ExerciseTrackingType.REPS ->
                                            "Target reps"

                                        ExerciseTrackingType.TIME ->
                                            "Target duration (sec)"
                                    }
                                )
                            },
                            singleLine = true,
                            keyboardOptions =
                                KeyboardOptions(
                                    keyboardType =
                                        KeyboardType.Number
                                ),
                            modifier =
                                Modifier.fillMaxWidth()
                        )

                        HorizontalDivider()

                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.SpaceBetween,
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Column(
                                modifier =
                                    Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "External load",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodyLarge,
                                    fontWeight =
                                        FontWeight.Medium
                                )

                                Text(
                                    text =
                                        if (
                                            state.hasExternalLoad
                                        ) {
                                            "Record weight for this exercise"
                                        } else {
                                            "No external weight"
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

                            Switch(
                                checked =
                                    state.hasExternalLoad,
                                onCheckedChange =
                                    viewModel::
                                    setHasExternalLoad
                            )
                        }

                        if (
                            state.hasExternalLoad
                        ) {
                            OutlinedTextField(
                                value =
                                    state.externalLoadKg,
                                onValueChange =
                                    viewModel::
                                    setExternalLoadKg,
                                label = {
                                    Text(
                                        "Default weight (kg)"
                                    )
                                },
                                singleLine = true,
                                keyboardOptions =
                                    KeyboardOptions(
                                        keyboardType =
                                            KeyboardType.Decimal
                                    ),
                                modifier =
                                    Modifier.fillMaxWidth()
                            )
                        }

                        HorizontalDivider()

                        OutlinedTextField(
                            value =
                                state.restBetweenSetsSec,
                            onValueChange =
                                viewModel::
                                setRestBetweenSetsSec,
                            label = {
                                Text(
                                    "Rest between sets (sec)"
                                )
                            },
                            singleLine = true,
                            keyboardOptions =
                                KeyboardOptions(
                                    keyboardType =
                                        KeyboardType.Number
                                ),
                            modifier =
                                Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value =
                                state.restBeforeNextSec,
                            onValueChange =
                                viewModel::
                                setRestBeforeNextSec,
                            label = {
                                Text(
                                    "Rest before next exercise (sec)"
                                )
                            },
                            singleLine = true,
                            keyboardOptions =
                                KeyboardOptions(
                                    keyboardType =
                                        KeyboardType.Number
                                ),
                            modifier =
                                Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            state.errorMessage?.let {
                    error ->
                item {
                    Text(
                        text = error,
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,
                        color =
                            MaterialTheme
                                .colorScheme
                                .error
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        viewModel.save()
                    },
                    enabled =
                        !state.isSaving &&
                                !state.isRemoving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier =
                                Modifier.height(22.dp),
                            strokeWidth = 2.dp,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onPrimary
                        )
                    } else {
                        Text("Save Changes")
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        showRemoveConfirm = true
                    },
                    enabled =
                        !state.isSaving &&
                                !state.isRemoving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors =
                        ButtonDefaults
                            .outlinedButtonColors(
                                contentColor =
                                    MaterialTheme
                                        .colorScheme
                                        .error
                            ),
                    border =
                        BorderStroke(
                            width = 1.dp,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .error
                        )
                ) {
                    Text(
                        "Remove from Workout"
                    )
                }
            }
        }
    }

    if (showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = {
                if (!state.isRemoving) {
                    showRemoveConfirm = false
                }
            },
            title = {
                Text(
                    "Remove exercise?"
                )
            },
            text = {
                Text(
                    "Remove ${state.exerciseName} " +
                            "from this workout? " +
                            "Existing workout history " +
                            "will be preserved."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveConfirm = false

                        viewModel
                            .removeFromWorkout()
                    },
                    colors =
                        ButtonDefaults
                            .textButtonColors(
                                contentColor =
                                    MaterialTheme
                                        .colorScheme
                                        .error
                            )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRemoveConfirm = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ExerciseTypeCard(
    trackingType: ExerciseTrackingType,
    isPerSide: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 12.dp
            )
        ) {
            Text(
                text =
                    when (trackingType) {
                        ExerciseTrackingType.REPS ->
                            "Rep Exercise"

                        ExerciseTrackingType.TIME ->
                            "Timed Exercise"
                    },
                style =
                    MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            if (isPerSide) {
                Spacer(
                    modifier =
                        Modifier.height(2.dp)
                )

                Text(
                    text = "Performed each side",
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }
        }
    }
}