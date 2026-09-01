package com.chani.reptap.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chani.reptap.data.local.dao.WorkoutExerciseWithName
import com.chani.reptap.data.local.entity.WorkoutDay
import com.chani.reptap.data.local.entity.ExerciseTrackingType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onStartSession: (WorkoutDay) -> Unit
) {
    val today by viewModel.todayWorkout.collectAsStateWithLifecycle()
    val allDays by viewModel.allDays.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val exercises by viewModel.todayExercises.collectAsStateWithLifecycle()

    var showDayMenu by remember { mutableStateOf(false) }
    var showDayPicker by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            val day = today
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 12.dp,
                        top = 8.dp,
                        end = 12.dp,
                        bottom = 16.dp
                    )
            ) {
                val nextDate by viewModel.nextWorkoutDate.collectAsStateWithLifecycle()
                if (day != null && !day.isRecovery && nextDate != null) {
                    Text(
                        text = "Next workout: ${viewModel.formatNextWorkoutDate(nextDate!!)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header row: day title + three-dot menu
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = 8.dp
                                ),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = day?.name ?: "No day selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Box {
                                IconButton(onClick = { showDayMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Day options")
                                }
                                DropdownMenu(expanded = showDayMenu, onDismissRequest = { showDayMenu = false }) {
                                    DropdownMenuItem(text = { Text("Select training day") }, onClick = {
                                        showDayMenu = false
                                        showDayPicker = true
                                    })
                                }
                            }
                        }



                        HorizontalDivider()

                        // Exercise list body, fills remaining space
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            when {
                                day == null -> {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("No workout data found")
                                    }
                                }
                                day.isRecovery -> {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            "Recovery day - No workouts today",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                else -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = 12.dp,
                                                vertical = 8.dp
                                            ),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        exercises.forEach { ex ->
                                            ExerciseRow(ex)
                                        }
                                    }
                                }
                            }
                        }

                        // Start Session button, part of the same card
                        if (day != null && !day.isRecovery) {
                            HorizontalDivider()
                            Button(
                                onClick = { onStartSession(day) },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(
                                    topStart = 0.dp,
                                    topEnd = 0.dp,
                                    bottomStart = 12.dp,
                                    bottomEnd = 12.dp
                                ),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues()
                            ) {
                                Text("Start Session", fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDayPicker) {
        AlertDialog(
            onDismissRequest = { showDayPicker = false },
            title = { Text("Select a workout day") },
            text = {
                Column {
                    allDays.forEach { d ->
                        TextButton(onClick = {
                            viewModel.selectDayManually(d)
                            showDayPicker = false
                        }) {
                            Text(d.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDayPicker = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ExerciseRow(ex: WorkoutExerciseWithName) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = ex.exerciseName,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )

        Spacer(Modifier.width(8.dp))

        val targetText =
            when (ex.trackingType) {
                ExerciseTrackingType.REPS -> {
                    "${ex.targetSets} sets · ${ex.targetReps} reps"
                }

                ExerciseTrackingType.TIME -> {
                    val targetSec =
                        ex.targetDurationSec ?: 0

                    "${ex.targetSets} sets · $targetSec sec"
                }
            }

        val loadText =
            ex.defaultWeightKg?.let { weight ->
                if (weight == weight.toInt().toFloat()) {
                    "${weight.toInt()}kg"
                } else {
                    "${weight}kg"
                }
            } ?: "Bodyweight"

        Text(
            text = "$targetText · $loadText",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}