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

@Composable
fun AddWorkoutExerciseScreen(
    viewModel: AddWorkoutExerciseViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val state by
    viewModel.uiState
        .collectAsStateWithLifecycle()

    LaunchedEffect(
        state.saveComplete
    ) {
        if (state.saveComplete) {
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            RepTapTopAppBar(
                title =
                    if (
                        state.exerciseName
                            .isNotBlank()
                    ) {
                        state.exerciseName
                    } else {
                        "Configure Exercise"
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 12.dp,
                    top =
                        innerPadding
                            .calculateTopPadding() +
                                8.dp,
                    end = 12.dp,
                    bottom = 16.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier =
                        Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 12.dp
                        )
                ) {
                    Text(
                        text =
                            when (
                                state.trackingType
                            ) {
                                ExerciseTrackingType.REPS ->
                                    "Rep Exercise"

                                ExerciseTrackingType.TIME ->
                                    "Timed Exercise"
                            },
                        style =
                            MaterialTheme
                                .typography
                                .titleSmall,
                        fontWeight =
                            FontWeight.Bold
                    )

                    if (state.isPerSide) {
                        Text(
                            text =
                                "Performed each side",
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
                }
            }

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
                            viewModel::
                            setTargetSets,
                        label = {
                            Text("Sets")
                        },
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Number
                            ),
                        singleLine = true,
                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value =
                            state.targetValue,
                        onValueChange =
                            viewModel::
                            setTargetValue,
                        label = {
                            Text(
                                if (
                                    state.trackingType ==
                                    ExerciseTrackingType.REPS
                                ) {
                                    "Target reps"
                                } else {
                                    "Target duration (sec)"
                                }
                            )
                        },
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Number
                            ),
                        singleLine = true,
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
                                "External load",
                                fontWeight =
                                    FontWeight.Medium
                            )

                            Text(
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

                    if (state.hasExternalLoad) {
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
                            keyboardOptions =
                                KeyboardOptions(
                                    keyboardType =
                                        KeyboardType.Decimal
                                ),
                            singleLine = true,
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
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Number
                            ),
                        singleLine = true,
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
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Number
                            ),
                        singleLine = true,
                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }
            }

            state.errorMessage?.let {
                Text(
                    text = it,
                    color =
                        MaterialTheme
                            .colorScheme
                            .error
                )
            }

            Button(
                onClick = {
                    viewModel.save()
                },
                enabled =
                    !state.isSaving,
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
                    Text("Add to Workout")
                }
            }
        }
    }
}