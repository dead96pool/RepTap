package com.chani.reptap.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    var showDayPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("RepTap") }) }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    val day = today
                    if (day == null) {
                        Text("No workout data found", style = MaterialTheme.typography.titleMedium)
                    } else if (day.isRecovery) {
                        Text("No workouts today", style = MaterialTheme.typography.headlineSmall)
                        Text("Recovery day — enjoy a walk or rest", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text(day.name, style = MaterialTheme.typography.headlineSmall)

                        Button(
                            onClick = { onViewDetails(day) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("View Workout Details")
                        }

                        Button(
                            onClick = { onStartSession(day) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Start Session")
                        }
                    }

                    OutlinedButton(
                        onClick = { showDayPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Select Workout Day Manually")
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onViewHistory, modifier = Modifier.weight(1f)) {
                            Text("History")
                        }
                        OutlinedButton(onClick = onViewProgress, modifier = Modifier.weight(1f)) {
                            Text("Progress")
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