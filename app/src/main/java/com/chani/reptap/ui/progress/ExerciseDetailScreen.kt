package com.chani.reptap.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    exerciseName: String,
    viewModel: ExerciseDetailViewModel,
    onBack: () -> Unit
) {
    val stats by viewModel.stats.collectAsState()
    val percentChange by viewModel.percentChange.collectAsState()
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exerciseName) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
            stats?.let { s ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        val avgReps = (s.averageReps * 10).roundToInt() / 10.0
                        DetailStatRow("Average Reps (per set)", "$avgReps")
                        DetailStatRow("Best Single Set", "${s.bestSingleSetReps} reps")
                        DetailStatRow("Sessions Logged", "${s.sessionsLogged}")
                        DetailStatRow("Total Reps (all-time)", "${s.totalReps}")
                        DetailStatRow("Last Performed", dateFormat.format(java.util.Date(s.lastPerformedAt)))
                        TrendStatRow(percentChange)
                    }
                }
            } ?: Text(
                "No data logged for this exercise yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun DetailStatRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TrendStatRow(percentChange: Double?) {
    Column {
        Text("Recent Trend", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (percentChange == null) {
            Text("-", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        } else {
            val displayPercent = (percentChange * 10).roundToInt() / 10.0
            val color = when {
                displayPercent > 0.0 -> Color(0xFF2E7D32)
                displayPercent < 0.0 -> Color(0xFFC62828)
                else -> MaterialTheme.colorScheme.onSurface
            }
            Text(
                "${if (displayPercent > 0) "+" else ""}$displayPercent%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}