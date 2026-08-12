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
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.style.TextAlign
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import androidx.compose.ui.graphics.toArgb
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    exerciseName: String,
    viewModel: ExerciseDetailViewModel,
    onBack: () -> Unit
) {
    val stats by viewModel.stats.collectAsState()
    val percentChange by viewModel.percentChange.collectAsState()
    val chartData by viewModel.chartData.collectAsState()
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }

    val xToDateMapKey = remember { ExtraStore.Key<Map<Double, Long>>() }
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(chartData) {
        if (chartData.isNotEmpty()) {
            val xToDate = chartData.mapIndexed { index, point -> index.toDouble() to point.startedAt }.toMap()
            modelProducer.runTransaction {
                lineSeries { series(chartData.map { it.avgWeightKg.toDouble() }) }
                extras { it[xToDateMapKey] = xToDate }
            }
        }
    }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d") }
    val dateAxisFormatter = remember {
        CartesianValueFormatter { context, x, _ ->
            val millis = context.model.extraStore[xToDateMapKey]?.get(x)
            millis?.let {
                java.time.Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
            } ?: ""
        }
    }

    val marker = rememberDefaultCartesianMarker(label = rememberTextComponent())

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

            if (chartData.size >= 2) {
                Text(
                    "Weight Over Time",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                )
                Card(modifier = Modifier.fillMaxWidth()) {
                    CartesianChartHost(
                        chart = rememberCartesianChart(
                            rememberLineCartesianLayer(),
                            startAxis = VerticalAxis.rememberStart(
                                title = "Weight (kg)",
                                titleComponent = rememberAxisLabelComponent()
                            ),
                            bottomAxis = HorizontalAxis.rememberBottom(
                                valueFormatter = dateAxisFormatter
                            ),
                            marker = marker
                        ),
                        modelProducer = modelProducer,
                        zoomState = rememberVicoZoomState(zoomEnabled = false),
                        modifier = Modifier.fillMaxWidth().height(240.dp).padding(8.dp)
                    )
                }
            } else if (stats != null) {
                Text(
                    "Log at least 2 sessions to see weight trends",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                    textAlign = TextAlign.Center
                )
            }
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