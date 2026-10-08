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

@Composable
fun RestoreExerciseScreen(
    viewModel: RestoreExerciseViewModel,
    onBack: () -> Unit,
    onRestored: (Long) -> Unit
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

    val selectedMuscle =
        muscleGroups.firstOrNull {
            it.id ==
                    state.primaryMuscleId
        }

    LaunchedEffect(
        state.restoredExerciseId
    ) {
        state.restoredExerciseId
            ?.let {
                    exerciseId ->

                onRestored(
                    exerciseId
                )
            }
    }

    Scaffold(
        topBar = {
            RepTapTopAppBar(
                title = "Restore Exercise",
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
                        Modifier.fillMaxWidth()
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
                                "Restore Previous Exercise",

                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium,

                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                "This will reconnect the exercise to its existing Progress history.",

                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )

                        OutlinedTextField(
                            value =
                                state.name,

                            onValueChange =
                                viewModel::setName,

                            label = {
                                Text(
                                    "Exercise name"
                                )
                            },

                            singleLine = true,

                            modifier =
                                Modifier.fillMaxWidth()
                        )

                        Column(
                            verticalArrangement =
                                Arrangement.spacedBy(
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
                                        Modifier.fillMaxWidth()
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

                                    muscleGroups.forEach {
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

                            if (
                                selectedMuscle == null &&
                                state.historicalMuscleName
                                    .isNotBlank()
                            ) {

                                Text(
                                    text =
                                        "Previously: ${state.historicalMuscleName}",

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
                                    }
                            )

                            Text(
                                text =
                                    "Tracking type is preserved when restoring this exercise identity.",

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
                        Modifier.fillMaxWidth()
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
                        viewModel.restore()
                    },

                    enabled =
                        !state.isSaving,

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
                            "Restore Exercise"
                        )
                    }
                }
            }
        }
    }
}