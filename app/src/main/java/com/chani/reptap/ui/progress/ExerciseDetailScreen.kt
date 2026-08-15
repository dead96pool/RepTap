package com.chani.reptap.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.chani.reptap.data.local.dao.ExerciseSessionDataPoint
import com.chani.reptap.data.local.dao.ExerciseSetDetail
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

private fun niceAxisMaximum(rawMax: Float): Pair<Float, Float> {
    if (rawMax <= 0f) return 5f to 1f
    val step = rawMax / 4f
    val magnitude = Math.pow(10.0, Math.floor(Math.log10(step.toDouble()))).toFloat()
    val normalized = step / magnitude
    val niceStep = when {
        normalized <= 1f -> 1f
        normalized <= 2f -> 2f
        normalized <= 5f -> 5f
        else -> 10f
    } * magnitude
    var axisMax = niceStep * 4f
    while (axisMax < rawMax) axisMax += niceStep
    axisMax += niceStep
    return axisMax to niceStep
}

private fun formatDecimal(value: Float): String {
    return if (value == value.roundToInt().toFloat()) {
        value.roundToInt().toString()
    } else {
        "%.1f".format(value)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    exerciseName: String,
    viewModel: ExerciseDetailViewModel,
    onBack: () -> Unit
) {
    val stats by viewModel.stats.collectAsState()
    val percentChange by viewModel.percentChange.collectAsState()
    val monthlyChartData by viewModel.monthlyChartData.collectAsState()
    val setDetailsBySession by viewModel.setDetailsBySession.collectAsState()
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exerciseName) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(16.dp)
        ) {
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

            if (monthlyChartData.isNotEmpty()) {
                Text(
                    "Weight Over Time",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                )

                val sortedMonths = remember(monthlyChartData) { monthlyChartData.keys.sorted() }

                val globalMaxWeight = remember(monthlyChartData) {
                    monthlyChartData.values.flatten().maxOfOrNull { it.avgWeightKg } ?: 1f
                }
                val (niceAxisMax, axisStep) = remember(globalMaxWeight) { niceAxisMaximum(globalMaxWeight) }

                var currentMonthIndex by remember(sortedMonths) {
                    val nowIndex = sortedMonths.indexOf(YearMonth.now())
                    mutableStateOf(if (nowIndex >= 0) nowIndex else (sortedMonths.size - 1).coerceAtLeast(0))
                }

                if (sortedMonths.isNotEmpty()) {
                    val currentMonth = sortedMonths[currentMonthIndex]
                    val sessionCount = monthlyChartData[currentMonth]?.size ?: 0
                    val points = monthlyChartData[currentMonth].orEmpty()

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { currentMonthIndex-- },
                                    enabled = currentMonthIndex > 0,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = "Previous month",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    "${currentMonth.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())} ${currentMonth.year} · $sessionCount session${if (sessionCount == 1) "" else "s"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IconButton(
                                    onClick = { currentMonthIndex++ },
                                    enabled = currentMonthIndex < sortedMonths.size - 1,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Next month",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            MonthChart(month = currentMonth, points = points, axisMax = niceAxisMax, axisStep = axisStep)
                        }
                    }
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
private fun MonthChart(
    month: YearMonth,
    points: List<ExerciseSessionDataPoint>,
    axisMax: Float,
    axisStep: Float
) {
    val daysInMonth = month.lengthOfMonth()
    val pointsByDay = remember(points) {
        points.associateBy { p ->
            Instant.ofEpochMilli(p.startedAt).atZone(ZoneId.systemDefault()).toLocalDate().dayOfMonth
        }
    }

    if (points.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No sessions logged in ${month.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val maxWeight = axisMax
    var selectedDay by remember(month) { mutableStateOf<Int?>(null) }
    var tooltipWidthPx by remember { mutableIntStateOf(0) }

    val barColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 11.sp, color = axisColor)
    val density = LocalDensity.current

    val leftPadDp = 16.dp
    val bottomPadDp = 24.dp
    val chartHeightDp = 180.dp
    val plotHeightDp = chartHeightDp - bottomPadDp

    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(chartHeightDp)) {
        val plotWidthDp = maxWidth - leftPadDp
        val slotWidthDp = plotWidthDp / daysInMonth

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(month, daysInMonth, slotWidthDp) {
                    val leftPadPx = leftPadDp.toPx()
                    val slotWidthPx = slotWidthDp.toPx()
                    detectTapGestures { offset ->
                        val day = ((offset.x - leftPadPx) / slotWidthPx).toInt() + 1
                        val tappedValidDay = day in 1..daysInMonth && pointsByDay.containsKey(day)
                        selectedDay = if (tappedValidDay && day == selectedDay) null else if (tappedValidDay) day else selectedDay
                    }
                }
        ) {
            val leftPad = leftPadDp.toPx()
            val bottomPad = bottomPadDp.toPx()
            val plotWidth = size.width - leftPad
            val plotHeight = size.height - bottomPad
            val slotWidth = plotWidth / daysInMonth

            val ySteps = (axisMax / axisStep).roundToInt()
            for (i in 0..ySteps) {
                val value = (maxWeight * i / ySteps)
                val y = plotHeight - (plotHeight * i / ySteps)

                drawLine(
                    color = axisColor.copy(alpha = 0.15f),
                    start = Offset(leftPad, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )

                val result = textMeasurer.measure(value.roundToInt().toString(), labelStyle)
                drawText(
                    textLayoutResult = result,
                    topLeft = Offset(leftPad - result.size.width - 6.dp.toPx(), y - result.size.height / 2f)
                )
            }

            val dashLen = slotWidth * 0.5f
            for (day in 1..daysInMonth) {
                val startX = leftPad + (day - 1) * slotWidth + (slotWidth - dashLen) / 2f
                drawLine(
                    color = axisColor.copy(alpha = 0.4f),
                    start = Offset(startX, plotHeight),
                    end = Offset(startX + dashLen, plotHeight),
                    strokeWidth = 1.dp.toPx()
                )
            }

            var day = 1
            while (day <= daysInMonth) {
                val centerX = leftPad + (day - 0.5f) * slotWidth
                val result = textMeasurer.measure(day.toString(), labelStyle)
                drawText(
                    textLayoutResult = result,
                    topLeft = Offset(centerX - result.size.width / 2f, plotHeight + 4.dp.toPx())
                )
                day += 5
            }

            val barWidth = slotWidth * 0.6f
            pointsByDay.forEach { (d, point) ->
                val barHeight = (point.avgWeightKg / maxWeight) * plotHeight
                val x = leftPad + (d - 1) * slotWidth + (slotWidth - barWidth) / 2f
                val color = if (d == selectedDay) barColor.copy(alpha = 0.7f) else barColor
                drawRect(
                    color = color,
                    topLeft = Offset(x, plotHeight - barHeight),
                    size = Size(barWidth, barHeight)
                )
            }
        }

        selectedDay?.let { day ->
            val point = pointsByDay[day]
            if (point != null) {
                val barTopFraction = point.avgWeightKg / maxWeight
                val barTopDp = plotHeightDp * (1f - barTopFraction)
                val centerXDp = leftPadDp + (day - 0.5f) * slotWidthDp
                val tooltipWidthDp = with(density) { tooltipWidthPx.toDp() }

                Card(
                    modifier = Modifier
                        .offset(x = centerXDp - tooltipWidthDp / 2f, y = (barTopDp - 25.dp).coerceAtLeast(0.dp))
                        .onGloballyPositioned { coordinates ->
                            tooltipWidthPx = coordinates.size.width
                        },
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.9f)
                    )
                ) {
                    Text(
                        "${formatDecimal(point.avgWeightKg)}kg",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
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