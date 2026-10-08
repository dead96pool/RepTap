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
import androidx.compose.material3.OutlinedTextField
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
    onCreateExercise: (String) -> Unit,
    onManageExercises: () -> Unit,
    onHistoricalExerciseClick:
        (String) -> Unit
) {
    val searchQuery by
    viewModel.searchQuery
        .collectAsStateWithLifecycle()

    val items by
    viewModel.searchItems
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
                }
            )
        }
    ) { innerPadding ->

        LazyColumn(
            modifier =
                Modifier.fillMaxSize(),

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
                    0.dp
                )
        ) {

            item {

                OutlinedTextField(
                    value =
                        searchQuery,

                    onValueChange =
                        viewModel::
                        setSearchQuery,

                    label = {
                        Text(
                            "Search exercises"
                        )
                    },

                    placeholder = {
                        Text(
                            "e.g. cable curl"
                        )
                    },

                    singleLine = true,

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                bottom = 8.dp
                            )
                )
            }

            items(
                items = items,
                key = {
                    it.key
                }
            ) { item ->

                when (
                    item.kind
                ) {

                    AddExerciseSearchItemKind
                        .CURRENT -> {

                        CurrentExerciseRow(
                            item = item,

                            onClick = {

                                item.exercise
                                    ?.let(
                                        onExerciseClick
                                    )
                            }
                        )
                    }

                    AddExerciseSearchItemKind
                        .HISTORICAL_ONLY -> {

                        HistoricalExerciseRow(
                            item = item,

                            onClick = {
                                onHistoricalExerciseClick(
                                    item.key
                                        .removePrefix(
                                            "historical:"
                                        )
                                )
                            }
                        )
                    }

                    AddExerciseSearchItemKind
                        .CREATE_NEW -> {

                        CreateExerciseRow(
                            typedName =
                                item.name,

                            onClick = {
                                onCreateExercise(
                                    item.name
                                )
                            }
                        )
                    }
                }

                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun CurrentExerciseRow(
    item: AddExerciseSearchItem,
    onClick: () -> Unit
) {
    val exercise =
        item.exercise
            ?: return

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    enabled =
                        !item
                            .isAlreadyInWorkout,

                    onClick =
                        onClick
                )
                .padding(
                    horizontal = 4.dp,
                    vertical = 14.dp
                ),

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
                text =
                    exercise.name,

                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,

                fontWeight =
                    FontWeight.Medium
            )

            Text(
                text =
                    buildString {

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

                        if (
                            exercise.isBodyweight
                        ) {
                            append(
                                " · bodyweight"
                            )
                        }

                        if (
                            exercise.isPerSide
                        ) {
                            append(
                                " · each side"
                            )
                        }

                        if (
                            item.isAlreadyInWorkout
                        ) {
                            append(
                                " · already in workout"
                            )
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

        if (
            item.isAlreadyInWorkout
        ) {

            Text(
                text = "Added",

                style =
                    MaterialTheme
                        .typography
                        .labelMedium,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

        } else {

            Icon(
                imageVector =
                    Icons.AutoMirrored
                        .Filled
                        .KeyboardArrowRight,

                contentDescription =
                    "Configure exercise",

                tint =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HistoricalExerciseRow(
    item: AddExerciseSearchItem,
    onClick: () -> Unit
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onClick
                )
                .padding(
                    horizontal = 4.dp,
                    vertical = 14.dp
                ),

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
                text = item.name,

                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,

                fontWeight =
                    FontWeight.Medium
            )

            Text(
                text =
                    "Previously used · restore history",

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
                Icons.AutoMirrored
                    .Filled
                    .KeyboardArrowRight,

            contentDescription =
                "Restore exercise",

            tint =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
    }
}

@Composable
private fun CreateExerciseRow(
    typedName: String,
    onClick: () -> Unit
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onClick
                )
                .padding(
                    horizontal = 4.dp,
                    vertical = 16.dp
                ),

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
                text =
                    if (
                        typedName.isBlank()
                    ) {
                        "Create new exercise"
                    } else {
                        "Create \"$typedName\""
                    },

                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,

                fontWeight =
                    FontWeight.Bold,

                color =
                    MaterialTheme
                        .colorScheme
                        .primary
            )

            Text(
                text =
                    "Create a new exercise identity",

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
                Icons.AutoMirrored
                    .Filled
                    .KeyboardArrowRight,

            contentDescription =
                "Create exercise"
        )
    }
}