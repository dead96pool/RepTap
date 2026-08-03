package com.chani.reptap.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chani.reptap.data.local.dao.SessionRecap
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SessionDetailScreen(
    dayName: String,
    currentSession: SessionRecap,
    viewModel: SessionDetailViewModel,
    onBack: () -> Unit
) {
    val exerciseGroups by viewModel.exerciseGroups.collectAsState()
    val previousSession by viewModel.previousSession.collectAsState()
    val timeFormat = remember { SimpleDateFormat("EEE, MMM d - h:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(dayName) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                RecapCard(currentSession, previousSession, timeFormat)
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        exerciseGroups.forEachIndexed { index, group ->
                            ExerciseRow(group)
                            if (index < exerciseGroups.size - 1) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecapCard(current: SessionRecap, previous: SessionRecap?, timeFormat: SimpleDateFormat) {
    val duration = TimeUnit.MILLISECONDS.toMinutes(current.finishedAt - current.startedAt)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Workout Recap", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            RecapRow("Started", timeFormat.format(Date(current.startedAt)))
            RecapRow("Finished", timeFormat.format(Date(current.finishedAt)))
            RecapRow("Duration", "$duration min")

            if (previous != null) {
                val prevDuration = TimeUnit.MILLISECONDS.toMinutes(previous.finishedAt - previous.startedAt)
                val durationDiff = duration - prevDuration
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Text("Compared to last time", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                val diffText = when {
                    durationDiff > 0 -> "+$durationDiff min"
                    durationDiff < 0 -> "$durationDiff min"
                    else -> "Same duration"
                }
                Text(diffText, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun RecapRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ExerciseRow(group: ExerciseGroup) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(
            group.exerciseName,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(Modifier.height(6.dp))
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            group.sets.forEach { set ->
                Text(
                    "${set.repsCompleted} reps @ ${set.weightKg}kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}