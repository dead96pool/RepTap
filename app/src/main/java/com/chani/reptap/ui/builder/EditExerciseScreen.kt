package com.chani.reptap.ui.builder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chani.reptap.data.local.entity.ExerciseTrackingType
import com.chani.reptap.ui.components.RepTapTopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton

@Composable
fun EditExerciseScreen(
    viewModel: EditExerciseViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit
) {
    val state by
    viewModel.uiState
        .collectAsStateWithLifecycle()

    val muscleGroups by
    viewModel.muscleGroups
        .collectAsStateWithLifecycle()

    var muscleMenuExpanded by
    remember {
        mutableStateOf(false)
    }

    var showDeleteDialog by
    remember {
        mutableStateOf(false)
    }

    val selectedMuscle =
        muscleGroups.firstOrNull {
            it.id ==
                    state.primaryMuscleId
        }

    LaunchedEffect(
        state.saveComplete
    ) {
        if (state.saveComplete) {
            onSaved()
        }
    }

    LaunchedEffect(
        state.deleteComplete
    ) {
        if (state.deleteComplete) {
            onDeleted()
        }
    }

    Scaffold(
        topBar = {
            RepTapTopAppBar(
                title = "Edit Exercise",
                onBack = onBack
            )
        }
    ) { innerPadding ->

        if (state.isLoading) {

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            innerPadding
                        ),
                contentAlignment =
                    Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            return@Scaffold
        }

        LazyColumn(
            contentPadding =
                PaddingValues(
                    start = 12.dp,
                    top =
                        innerPadding
                            .calculateTopPadding() +
                                8.dp,
                    end = 12.dp,
                    bottom = 16.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {

            item {
                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                ) {
                    Column(
                        modifier =
                            Modifier.padding(
                                16.dp
                            ),
                        verticalArrangement =
                            Arrangement.spacedBy(
                                16.dp
                            )
                    ) {

                        Text(
                            text =
                                "Exercise Definition",
                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium,
                            fontWeight =
                                FontWeight.Bold
                        )

                        OutlinedTextField(
                            value =
                                state.name,
                            onValueChange =
                                viewModel::
                                setName,
                            label = {
                                Text(
                                    "Exercise name"
                                )
                            },
                            singleLine = true,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                        )

                        Column(
                            verticalArrangement =
                                Arrangement
                                    .spacedBy(
                                        6.dp
                                    )
                        ) {

                            Text(
                                text =
                                    "Primary muscle",
                                fontWeight =
                                    FontWeight.Medium
                            )

                            Box {

                                OutlinedButton(
                                    onClick = {
                                        muscleMenuExpanded =
                                            true
                                    },
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                ) {
                                    Text(
                                        selectedMuscle
                                            ?.name
                                            ?: "Select muscle"
                                    )
                                }

                                DropdownMenu(
                                    expanded =
                                        muscleMenuExpanded,
                                    onDismissRequest = {
                                        muscleMenuExpanded =
                                            false
                                    }
                                ) {
                                    muscleGroups
                                        .forEach {
                                                muscle ->

                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        muscle.name
                                                    )
                                                },
                                                onClick = {

                                                    viewModel
                                                        .setPrimaryMuscleId(
                                                            muscle.id
                                                        )

                                                    muscleMenuExpanded =
                                                        false
                                                }
                                            )
                                        }
                                }
                            }
                        }

                        Column(
                            verticalArrangement =
                                Arrangement.spacedBy(
                                    4.dp
                                )
                        ) {
                            Text(
                                text =
                                    "Tracking type",
                                fontWeight =
                                    FontWeight.Medium
                            )

                            Text(
                                text =
                                    when (
                                        state.trackingType
                                    ) {
                                        ExerciseTrackingType.REPS ->
                                            "Reps"

                                        ExerciseTrackingType.TIME ->
                                            "Time"
                                    },
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyLarge
                            )

                            Text(
                                text =
                                    "Tracking type cannot be changed in place because reps and time use different historical measurements.",
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
            }

            item {
                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                ) {
                    Column(
                        modifier =
                            Modifier.padding(
                                16.dp
                            ),
                        verticalArrangement =
                            Arrangement.spacedBy(
                                16.dp
                            )
                    ) {

                        Text(
                            text =
                                "Exercise Properties",
                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement
                                    .SpaceBetween,
                            verticalAlignment =
                                Alignment
                                    .CenterVertically
                        ) {
                            Column(
                                modifier =
                                    Modifier
                                        .weight(1f)
                            ) {
                                Text(
                                    text =
                                        "Bodyweight",
                                    fontWeight =
                                        FontWeight.Medium
                                )

                                Text(
                                    text =
                                        "Exercise does not require external load",
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
                                    state.isBodyweight,
                                onCheckedChange =
                                    viewModel::
                                    setIsBodyweight
                            )
                        }

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement
                                    .SpaceBetween,
                            verticalAlignment =
                                Alignment
                                    .CenterVertically
                        ) {
                            Column(
                                modifier =
                                    Modifier
                                        .weight(1f)
                            ) {
                                Text(
                                    text =
                                        "Performed per side",
                                    fontWeight =
                                        FontWeight.Medium
                                )

                                Text(
                                    text =
                                        "Track the exercise as left/right or each side",
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
                                    state.isPerSide,
                                onCheckedChange =
                                    viewModel::
                                    setIsPerSide
                            )
                        }
                    }
                }
            }

            state.errorMessage?.let {
                    error ->

                item {
                    Text(
                        text = error,
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
                                !state.isDeleting,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier =
                                Modifier.height(
                                    22.dp
                                ),
                            strokeWidth = 2.dp,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onPrimary
                        )
                    } else {
                        Text(
                            "Save Changes"
                        )
                    }
                }
            }

            item {

                OutlinedButton(
                    onClick = {
                        showDeleteDialog =
                            true
                    },

                    enabled =
                        !state.isSaving &&
                                !state.isDeleting,

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp),

                    colors =
                        ButtonDefaults
                            .outlinedButtonColors(
                                contentColor =
                                    MaterialTheme
                                        .colorScheme
                                        .error
                            )
                ) {

                    if (state.isDeleting) {

                        CircularProgressIndicator(
                            modifier =
                                Modifier.height(
                                    22.dp
                                ),
                            strokeWidth = 2.dp
                        )

                    } else {

                        Text(
                            "Delete Exercise"
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {

        val isInUse =
            state.workoutUsageNames
                .isNotEmpty()

        AlertDialog(
            onDismissRequest = {
                if (!state.isDeleting) {
                    showDeleteDialog =
                        false
                }
            },

            title = {
                Text(
                    if (isInUse) {
                        "Exercise is in use"
                    } else {
                        "Delete exercise?"
                    }
                )
            },

            text = {

                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {

                    if (isInUse) {

                        Text(
                            "This exercise is currently used by:"
                        )

                        state.workoutUsageNames
                            .forEach {
                                    workoutName ->

                                Text(
                                    text =
                                        "• $workoutName",
                                    fontWeight =
                                        FontWeight.Medium
                                )
                            }

                        Text(
                            text =
                                "Remove it from these workouts before deleting it from the Exercise Library.",
                            style =
                                MaterialTheme
                                    .typography
                                    .bodyMedium
                        )

                    } else {

                        Text(
                            "This removes the current exercise from the Exercise Library."
                        )

                        Text(
                            text =
                                "Completed workout history and Progress data will remain available.",
                            style =
                                MaterialTheme
                                    .typography
                                    .bodyMedium
                        )
                    }
                }
            },

            confirmButton = {

                if (isInUse) {

                    TextButton(
                        onClick = {
                            showDeleteDialog =
                                false
                        }
                    ) {
                        Text("OK")
                    }

                } else {

                    TextButton(
                        onClick = {

                            showDeleteDialog =
                                false

                            viewModel.delete()
                        }
                    ) {
                        Text(
                            text = "Delete",
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .error
                        )
                    }
                }
            },

            dismissButton = {

                if (!isInUse) {

                    TextButton(
                        onClick = {
                            showDeleteDialog =
                                false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}