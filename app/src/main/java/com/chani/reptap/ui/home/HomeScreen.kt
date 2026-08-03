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
import com.chani.reptap.data.local.dao.WorkoutExerciseWithName
import com.chani.reptap.data.local.entity.WorkoutDay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onStartSession: (WorkoutDay) -> Unit,
    onViewDetails: (WorkoutDay) -> Unit,
    onViewHistory: () -> Unit,
    onViewProgress: () -> Unit
) {
    val today by viewModel.todayWorkout.collectAsState()
    val allDays by viewModel.allDays.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val exercises by viewModel.todayExercises.collectAsState()

    var showTopMenu by remember { mutableStateOf(false) }
    var showDayMenu by remember { mutableStateOf(false) }
    var showDayPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RepTap") },
                actions = {
                    IconButton(onClick = { showTopMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = showTopMenu, onDismissRequest = { showTopMenu = false }) {
                        DropdownMenuItem(text = { Text("History") }, onClick = {
                            showTopMenu = false
                            onViewHistory()
                        })
                        DropdownMenuItem(text = { Text("Progress") }, onClick = {
                            showTopMenu = false
                            onViewProgress()
                        })
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                val day = today
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Header row: day title + three-dot menu
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
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

                            val nextDate by viewModel.nextWorkoutDate.collectAsState()
                            if (day != null && !day.isRecovery && nextDate != null) {
                                Text(
                                    text = "Next workout: ${viewModel.formatNextWorkoutDate(nextDate!!)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                                )
                            }

                            HorizontalDivider()

                            // Exercise list body, fills remaining space
                            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
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
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                            verticalArrangement = Arrangement.SpaceEvenly
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = ex.exerciseName,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        Spacer(Modifier.width(8.dp))
        val weightText = if (ex.defaultWeightKg == ex.defaultWeightKg.toInt().toFloat())
            "${ex.defaultWeightKg.toInt()}kg" else "${ex.defaultWeightKg}kg"
        Text(
            text = "${ex.targetSets} sets · ${ex.repRangeMin} reps · $weightText",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}