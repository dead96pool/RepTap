package com.chani.reptap.ui.progress

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt
import androidx.compose.material3.Button
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel,
    onExerciseClick: (ExerciseImprovement) -> Unit,
    onViewAllExercises: () -> Unit
) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val improvements by viewModel.exerciseImprovements.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            Button(onClick = { scope.launch { viewModel.seedFakeDataForTesting() } }) {
                Text("Seed Fake Data (TEMP)")
            }
        }

        item {
            summary?.let { s ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ProgressStatRow("Total Sessions", "${s.totalSessions}")
                        ProgressStatRow("Average Duration", "${s.averageDurationMillis / 60000} min")
                        ProgressStatRow("Sessions per Week", "${(s.averageSessionsPerWeek * 10).roundToInt() / 10.0}")
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Exercise Trends",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onViewAllExercises) {
                    Text("View All")
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    improvements.forEachIndexed { index, item ->
                        ExerciseImprovementRow(item, onClick = { onExerciseClick(item) })
                        if (index < improvements.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                    if (improvements.isEmpty()) {
                        Text(
                            "Log at least 6 sessions of an exercise to see trends",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun ProgressStatRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ExerciseImprovementRow(item: ExerciseImprovement, onClick: () -> Unit) {
    val color = when {
        item.percentChange > 0.0 -> Color(0xFF2E7D32)
        item.percentChange < 0.0 -> Color(0xFFC62828)
        else -> MaterialTheme.colorScheme.onSurface
    }

    val displayPercent = (item.percentChange * 10).roundToInt() / 10.0

    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(item.exerciseName, style = MaterialTheme.typography.bodyMedium)
        Text(
            "${if (displayPercent > 0) "+" else ""}$displayPercent%",
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}