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
import com.chani.reptap.ui.components.RepTapTopAppBar
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

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
    val currentSessionRecap by viewModel.currentSessionRecap.collectAsState()
    val timeFormat = remember { SimpleDateFormat("EEE, MMM d - h:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            RepTapTopAppBar(
                title = dayName,
                onBack = onBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 12.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                end = 12.dp,
                bottom = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                RecapCard(currentSessionRecap ?: currentSession, previousSession, timeFormat)
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
                val durationDiff = (duration - prevDuration).toInt()
                val repsDiff = current.totalReps - previous.totalReps
                val setsDiff = current.totalSets - previous.totalSets
                val weightDiff = current.avgWeightKg - previous.avgWeightKg
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Text("Compared to last time", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DeltaStat(repsDiff, "reps")
                    DeltaStat(setsDiff, "sets")
                    DeltaStat(durationDiff, "min", invertColor = true)
                    DeltaStatFloat(weightDiff, "kg")
                }
            }
        }
    }
}

@Composable
private fun DeltaStat(delta: Int, unit: String, invertColor: Boolean = false) {
    val isPositive = if (invertColor) delta < 0 else delta > 0
    val isNegative = if (invertColor) delta > 0 else delta < 0
    val color = when {
        isPositive -> androidx.compose.ui.graphics.Color(0xFF2E7D32)
        isNegative -> androidx.compose.ui.graphics.Color(0xFFC62828)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val sign = if (delta > 0) "+" else ""
    Text("$sign$delta $unit", color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun DeltaStatFloat(delta: Float, unit: String) {
    val rounded = (delta * 10).roundToInt() / 10f
    val color = when {
        rounded > 0f -> androidx.compose.ui.graphics.Color(0xFF2E7D32)
        rounded < 0f -> androidx.compose.ui.graphics.Color(0xFFC62828)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val sign = if (rounded > 0) "+" else ""
    Text("$sign$rounded $unit", color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
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

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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
                val performanceText =
                    if (set.durationCompletedSec != null) {
                        "${set.durationCompletedSec} sec"
                    } else {
                        "${set.repsCompleted} reps"
                    }

                val weightText =
                    set.weightKg?.let { weight ->
                        val formattedWeight =
                            if (weight == weight.toInt().toFloat()) {
                                weight.toInt().toString()
                            } else {
                                weight.toString()
                            }

                        " @ ${formattedWeight}kg"
                    } ?: ""

                val setText =
                    performanceText + weightText

                Text(
                    text = setText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}