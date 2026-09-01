package com.chani.reptap.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
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
import com.chani.reptap.data.local.entity.ExerciseTrackingType
import com.chani.reptap.ui.components.RepTapTopAppBar
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
            RepTapTopAppBar(
                title = exerciseName,
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 12.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    end = 12.dp,
                    bottom = 12.dp
                )
        ) {
            stats?.let { s ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 12.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (s.trackingType) {
                            ExerciseTrackingType.REPS -> {
                                val avgReps =
                                    (s.averageReps * 10).roundToInt() / 10.0

                                DetailStatRow(
                                    "Average Reps (per set)",
                                    "$avgReps"
                                )

                                DetailStatRow(
                                    "Best Single Set",
                                    "${s.bestSingleSetReps} reps"
                                )
                            }

                            ExerciseTrackingType.TIME -> {
                                val avgDuration =
                                    s.averageDurationSec?.let {
                                        (it * 10).roundToInt() / 10.0
                                    }

                                DetailStatRow(
                                    "Average Hold",
                                    if (avgDuration != null) {
                                        "$avgDuration sec"
                                    } else {
                                        "-"
                                    }
                                )

                                DetailStatRow(
                                    "Best Hold",
                                    s.bestDurationSec?.let {
                                        "$it sec"
                                    } ?: "-"
                                )
                            }
                        }

                        DetailStatRow(
                            "Sessions Logged",
                            "${s.sessionsLogged}"
                        )

                        DetailStatRow(
                            "Last Performed",
                            dateFormat.format(
                                java.util.Date(s.lastPerformedAt)
                            )
                        )

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
                val sortedMonths = remember(monthlyChartData) { monthlyChartData.keys.sorted() }

                val globalMaxWeight = remember(monthlyChartData) {
                    monthlyChartData.values.flatten().maxOfOrNull { it.avgWeightKg } ?: 1f
                }
                val (niceAxisMax, axisStep) = remember(globalMaxWeight) { niceAxisMaximum(globalMaxWeight) }
                val globalMaxReps = remember(monthlyChartData) {
                    monthlyChartData.values.flatten().maxOfOrNull { it.avgReps.toFloat() } ?: 1f
                }
                val (niceRepsAxisMax, repsAxisStep) = remember(globalMaxReps) { niceAxisMaximum(globalMaxReps) }
                val globalMaxVolume = remember(monthlyChartData) {
                    monthlyChartData.values.flatten().maxOfOrNull { it.totalVolume.toFloat() } ?: 1f
                }
                val (niceVolumeAxisMax, volumeAxisStep) = remember(globalMaxVolume) { niceAxisMaximum(globalMaxVolume) }
                val globalMaxBestReps = remember(monthlyChartData) {
                    monthlyChartData.values.flatten().maxOfOrNull { it.maxReps.toFloat() } ?: 1f
                }
                val (niceBestRepsAxisMax, bestRepsAxisStep) = remember(globalMaxBestReps) { niceAxisMaximum(globalMaxBestReps) }

                val globalMaxSets = remember(monthlyChartData) {
                    monthlyChartData.values.flatten().maxOfOrNull { it.setsCount.toFloat() } ?: 1f
                }
                val (niceSetsAxisMax, setsAxisStep) = remember(globalMaxSets) { niceAxisMaximum(globalMaxSets) }

                val globalMaxAverageDuration = remember(monthlyChartData) {
                    monthlyChartData.values
                        .flatten()
                        .maxOfOrNull { it.avgDurationSec?.toFloat() ?: 0f }
                        ?: 1f
                }

                val (
                    niceAverageDurationAxisMax,
                    averageDurationAxisStep
                ) = remember(globalMaxAverageDuration) {
                    niceAxisMaximum(globalMaxAverageDuration)
                }

                val globalMaxBestDuration = remember(monthlyChartData) {
                    monthlyChartData.values
                        .flatten()
                        .maxOfOrNull { it.maxDurationSec?.toFloat() ?: 0f }
                        ?: 1f
                }

                val (
                    niceBestDurationAxisMax,
                    bestDurationAxisStep
                ) = remember(globalMaxBestDuration) {
                    niceAxisMaximum(globalMaxBestDuration)
                }

                val hasExternalWeight = remember(setDetailsBySession) {
                    setDetailsBySession.values
                        .flatten()
                        .any { it.weightKg != null }
                }

                val sharedTextMeasurer = rememberTextMeasurer()
                val sharedDensity = LocalDensity.current
                val sharedLeftPadDp = remember(
                    niceAxisMax,
                    niceRepsAxisMax,
                    niceVolumeAxisMax,
                    niceBestRepsAxisMax,
                    niceSetsAxisMax,
                    niceAverageDurationAxisMax,
                    niceBestDurationAxisMax
                ) {
                    val allMaxValues = listOf(
                        niceAxisMax,
                        niceRepsAxisMax,
                        niceVolumeAxisMax,
                        niceBestRepsAxisMax,
                        niceSetsAxisMax,
                        niceAverageDurationAxisMax,
                        niceBestDurationAxisMax
                    )
                    val widestPx = allMaxValues.maxOf { maxVal ->
                        sharedTextMeasurer.measure(
                            maxVal.roundToInt().toString(),
                            androidx.compose.ui.text.TextStyle(
                                fontSize = 11.sp
                            )
                        ).size.width
                    }
                    with(sharedDensity) {
                        widestPx.toDp()
                    } + 4.dp
                }

                var weightMonthIndex by remember(sortedMonths) {
                    val nowIndex = sortedMonths.indexOf(YearMonth.now())
                    mutableStateOf(if (nowIndex >= 0) nowIndex else (sortedMonths.size - 1).coerceAtLeast(0))
                }
                var repsMonthIndex by remember(sortedMonths) {
                    val nowIndex = sortedMonths.indexOf(YearMonth.now())
                    mutableStateOf(if (nowIndex >= 0) nowIndex else (sortedMonths.size - 1).coerceAtLeast(0))
                }
                var volumeMonthIndex by remember(sortedMonths) {
                    val nowIndex = sortedMonths.indexOf(YearMonth.now())
                    mutableStateOf(if (nowIndex >= 0) nowIndex else (sortedMonths.size - 1).coerceAtLeast(0))
                }
                var bestRepsMonthIndex by remember(sortedMonths) {
                    val nowIndex = sortedMonths.indexOf(YearMonth.now())
                    mutableStateOf(if (nowIndex >= 0) nowIndex else (sortedMonths.size - 1).coerceAtLeast(0))
                }
                var setsMonthIndex by remember(sortedMonths) {
                    val nowIndex = sortedMonths.indexOf(YearMonth.now())
                    mutableStateOf(if (nowIndex >= 0) nowIndex else (sortedMonths.size - 1).coerceAtLeast(0))
                }

                if (sortedMonths.isNotEmpty()) {
                    val weightMonth = sortedMonths[weightMonthIndex]
                    val weightSessionCount = monthlyChartData[weightMonth]?.size ?: 0
                    val weightPoints = monthlyChartData[weightMonth].orEmpty()

                    val repsMonth = sortedMonths[repsMonthIndex]
                    val repsSessionCount = monthlyChartData[repsMonth]?.size ?: 0
                    val repsPoints = monthlyChartData[repsMonth].orEmpty()

                    val volumeMonth = sortedMonths[volumeMonthIndex]
                    val volumeSessionCount = monthlyChartData[volumeMonth]?.size ?: 0
                    val volumePoints = monthlyChartData[volumeMonth].orEmpty()

                    val bestRepsMonth = sortedMonths[bestRepsMonthIndex]
                    val bestRepsSessionCount = monthlyChartData[bestRepsMonth]?.size ?: 0
                    val bestRepsPoints = monthlyChartData[bestRepsMonth].orEmpty()

                    val setsMonth = sortedMonths[setsMonthIndex]
                    val setsSessionCount = monthlyChartData[setsMonth]?.size ?: 0
                    val setsPoints = monthlyChartData[setsMonth].orEmpty()

                    if (stats?.trackingType == ExerciseTrackingType.REPS) {

                        Text(
                            "Weight Over Time",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(4.dp)) {
                                MonthSelectorRow(
                                    label = "${
                                        weightMonth.month.getDisplayName(
                                            java.time.format.TextStyle.FULL,
                                            Locale.getDefault()
                                        )
                                    } ${weightMonth.year} · $weightSessionCount session${if (weightSessionCount == 1) "" else "s"}",
                                    canGoPrevious = weightMonthIndex > 0,
                                    canGoNext = weightMonthIndex < sortedMonths.size - 1,
                                    onPrevious = { weightMonthIndex-- },
                                    onNext = { weightMonthIndex++ }
                                )
                                MonthChart(
                                    month = weightMonth,
                                    points = weightPoints,
                                    axisMax = niceAxisMax,
                                    axisStep = axisStep,
                                    valueSelector = { it.avgWeightKg },
                                    unitLabel = "kg",
                                    leftPadDp = sharedLeftPadDp,
                                    setDetailsBySession = setDetailsBySession
                                )
                            }
                        }

                        Text(
                            "Reps Over Time",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(4.dp)) {
                                MonthSelectorRow(
                                    label = "${
                                        repsMonth.month.getDisplayName(
                                            java.time.format.TextStyle.FULL,
                                            Locale.getDefault()
                                        )
                                    } ${repsMonth.year} · $repsSessionCount session${if (repsSessionCount == 1) "" else "s"}",
                                    canGoPrevious = repsMonthIndex > 0,
                                    canGoNext = repsMonthIndex < sortedMonths.size - 1,
                                    onPrevious = { repsMonthIndex-- },
                                    onNext = { repsMonthIndex++ }
                                )
                                MonthChart(
                                    month = repsMonth,
                                    points = repsPoints,
                                    axisMax = niceRepsAxisMax,
                                    axisStep = repsAxisStep,
                                    valueSelector = { it.avgReps.toFloat() },
                                    unitLabel = "",
                                    leftPadDp = sharedLeftPadDp,
                                    setDetailsBySession = setDetailsBySession
                                )
                            }
                        }

                        Text(
                            "Volume Over Time",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(4.dp)) {
                                MonthSelectorRow(
                                    label = "${
                                        volumeMonth.month.getDisplayName(
                                            java.time.format.TextStyle.FULL,
                                            Locale.getDefault()
                                        )
                                    } ${volumeMonth.year} · $volumeSessionCount session${if (volumeSessionCount == 1) "" else "s"}",
                                    canGoPrevious = volumeMonthIndex > 0,
                                    canGoNext = volumeMonthIndex < sortedMonths.size - 1,
                                    onPrevious = { volumeMonthIndex-- },
                                    onNext = { volumeMonthIndex++ }
                                )
                                MonthChart(
                                    month = volumeMonth,
                                    points = volumePoints,
                                    axisMax = niceVolumeAxisMax,
                                    axisStep = volumeAxisStep,
                                    valueSelector = { it.totalVolume.toFloat() },
                                    unitLabel = "",
                                    leftPadDp = sharedLeftPadDp,
                                    setDetailsBySession = setDetailsBySession
                                )
                            }
                        }

                        Text(
                            "Best Single-Set Reps Over Time",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(4.dp)) {
                                MonthSelectorRow(
                                    label = "${
                                        bestRepsMonth.month.getDisplayName(
                                            java.time.format.TextStyle.FULL,
                                            Locale.getDefault()
                                        )
                                    } ${bestRepsMonth.year} · $bestRepsSessionCount session${if (bestRepsSessionCount == 1) "" else "s"}",
                                    canGoPrevious = bestRepsMonthIndex > 0,
                                    canGoNext = bestRepsMonthIndex < sortedMonths.size - 1,
                                    onPrevious = { bestRepsMonthIndex-- },
                                    onNext = { bestRepsMonthIndex++ }
                                )
                                MonthChart(
                                    month = bestRepsMonth,
                                    points = bestRepsPoints,
                                    axisMax = niceBestRepsAxisMax,
                                    axisStep = bestRepsAxisStep,
                                    valueSelector = { it.maxReps.toFloat() },
                                    unitLabel = "",
                                    leftPadDp = sharedLeftPadDp,
                                    setDetailsBySession = setDetailsBySession
                                )
                            }
                        }

                        Text(
                            "Sets Per Session Over Time",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(4.dp)) {
                                MonthSelectorRow(
                                    label = "${
                                        setsMonth.month.getDisplayName(
                                            java.time.format.TextStyle.FULL,
                                            Locale.getDefault()
                                        )
                                    } ${setsMonth.year} · $setsSessionCount session${if (setsSessionCount == 1) "" else "s"}",
                                    canGoPrevious = setsMonthIndex > 0,
                                    canGoNext = setsMonthIndex < sortedMonths.size - 1,
                                    onPrevious = { setsMonthIndex-- },
                                    onNext = { setsMonthIndex++ }
                                )
                                MonthChart(
                                    month = setsMonth,
                                    points = setsPoints,
                                    axisMax = niceSetsAxisMax,
                                    axisStep = setsAxisStep,
                                    valueSelector = { it.setsCount.toFloat() },
                                    unitLabel = "",
                                    leftPadDp = sharedLeftPadDp,
                                    setDetailsBySession = setDetailsBySession
                                )
                            }
                        }
                    }

                    if (stats?.trackingType == ExerciseTrackingType.TIME) {
                        Text(
                            "Average Hold Over Time",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                top = 16.dp,
                                bottom = 8.dp
                            )
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(4.dp)
                            ) {
                                MonthSelectorRow(
                                    label =
                                        "${repsMonth.month.getDisplayName(
                                            java.time.format.TextStyle.FULL,
                                            Locale.getDefault()
                                        )} ${repsMonth.year} · " +
                                                "$repsSessionCount session" +
                                                if (repsSessionCount == 1) "" else "s",
                                    canGoPrevious = repsMonthIndex > 0,
                                    canGoNext =
                                        repsMonthIndex < sortedMonths.size - 1,
                                    onPrevious = {
                                        repsMonthIndex--
                                    },
                                    onNext = {
                                        repsMonthIndex++
                                    }
                                )

                                MonthChart(
                                    month = repsMonth,
                                    points = repsPoints,
                                    axisMax = niceAverageDurationAxisMax,
                                    axisStep = averageDurationAxisStep,
                                    valueSelector = {
                                        it.avgDurationSec?.toFloat() ?: 0f
                                    },
                                    unitLabel = " sec",
                                    leftPadDp = sharedLeftPadDp,
                                    setDetailsBySession = setDetailsBySession,
                                    setDetailFormatter = ::formatTimedSetDetail
                                )
                            }
                        }

                        Text(
                            "Best Hold Over Time",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                top = 16.dp,
                                bottom = 8.dp
                            )
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(4.dp)
                            ) {
                                MonthSelectorRow(
                                    label =
                                        "${bestRepsMonth.month.getDisplayName(
                                            java.time.format.TextStyle.FULL,
                                            Locale.getDefault()
                                        )} ${bestRepsMonth.year} · " +
                                                "$bestRepsSessionCount session" +
                                                if (bestRepsSessionCount == 1) "" else "s",
                                    canGoPrevious = bestRepsMonthIndex > 0,
                                    canGoNext =
                                        bestRepsMonthIndex < sortedMonths.size - 1,
                                    onPrevious = {
                                        bestRepsMonthIndex--
                                    },
                                    onNext = {
                                        bestRepsMonthIndex++
                                    }
                                )

                                MonthChart(
                                    month = bestRepsMonth,
                                    points = bestRepsPoints,
                                    axisMax = niceBestDurationAxisMax,
                                    axisStep = bestDurationAxisStep,
                                    valueSelector = {
                                        it.maxDurationSec?.toFloat() ?: 0f
                                    },
                                    unitLabel = " sec",
                                    leftPadDp = sharedLeftPadDp,
                                    setDetailsBySession = setDetailsBySession,
                                    setDetailFormatter = ::formatTimedSetDetail
                                )
                            }
                        }

                        if (hasExternalWeight) {
                            Text(
                                "External Weight Over Time",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(
                                    top = 16.dp,
                                    bottom = 8.dp
                                )
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(4.dp)
                                ) {
                                    MonthSelectorRow(
                                        label =
                                            "${weightMonth.month.getDisplayName(
                                                java.time.format.TextStyle.FULL,
                                                Locale.getDefault()
                                            )} ${weightMonth.year} · " +
                                                    "$weightSessionCount session" +
                                                    if (weightSessionCount == 1) "" else "s",
                                        canGoPrevious = weightMonthIndex > 0,
                                        canGoNext =
                                            weightMonthIndex < sortedMonths.size - 1,
                                        onPrevious = {
                                            weightMonthIndex--
                                        },
                                        onNext = {
                                            weightMonthIndex++
                                        }
                                    )

                                    MonthChart(
                                        month = weightMonth,
                                        points = weightPoints,
                                        axisMax = niceAxisMax,
                                        axisStep = axisStep,
                                        valueSelector = {
                                            it.avgWeightKg
                                        },
                                        unitLabel = "kg",
                                        leftPadDp = sharedLeftPadDp,
                                        setDetailsBySession =
                                            setDetailsBySession,
                                        setDetailFormatter =
                                            ::formatTimedSetDetail
                                    )
                                }
                            }
                        }

                        Text(
                            "Sets Per Session Over Time",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                top = 16.dp,
                                bottom = 8.dp
                            )
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(4.dp)
                            ) {
                                MonthSelectorRow(
                                    label =
                                        "${setsMonth.month.getDisplayName(
                                            java.time.format.TextStyle.FULL,
                                            Locale.getDefault()
                                        )} ${setsMonth.year} · " +
                                                "$setsSessionCount session" +
                                                if (setsSessionCount == 1) "" else "s",
                                    canGoPrevious = setsMonthIndex > 0,
                                    canGoNext =
                                        setsMonthIndex < sortedMonths.size - 1,
                                    onPrevious = {
                                        setsMonthIndex--
                                    },
                                    onNext = {
                                        setsMonthIndex++
                                    }
                                )

                                MonthChart(
                                    month = setsMonth,
                                    points = setsPoints,
                                    axisMax = niceSetsAxisMax,
                                    axisStep = setsAxisStep,
                                    valueSelector = {
                                        it.setsCount.toFloat()
                                    },
                                    unitLabel = "",
                                    leftPadDp = sharedLeftPadDp,
                                    setDetailsBySession =
                                        setDetailsBySession,
                                    setDetailFormatter =
                                        ::formatTimedSetDetail
                                )
                            }
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
private fun MonthSelectorRow(
    label: String,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPrevious,
            enabled = canGoPrevious,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous month",
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        IconButton(
            onClick = onNext,
            enabled = canGoNext,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next month",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun formatTimedSetDetail(
    set: ExerciseSetDetail
): String {
    val duration =
        set.durationCompletedSec ?: 0

    val weightText =
        set.weightKg?.let { weight ->
            " @ ${formatDecimal(weight)}kg"
        } ?: ""

    return "$duration sec$weightText"
}

@Composable
private fun MonthChart(
    month: YearMonth,
    points: List<ExerciseSessionDataPoint>,
    axisMax: Float,
    axisStep: Float,
    valueSelector: (ExerciseSessionDataPoint) -> Float,
    unitLabel: String,
    leftPadDp: androidx.compose.ui.unit.Dp,
    setDetailsBySession: Map<Long, List<ExerciseSetDetail>>,
    setDetailFormatter: (ExerciseSetDetail) -> String = { set ->
        val weightText =
            set.weightKg?.let { weight ->
                " @ ${formatDecimal(weight)}kg"
            } ?: ""

        "${set.repsCompleted} reps$weightText"
    }
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
    var tooltipHeightPx by remember { mutableIntStateOf(0) }

    val barColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 11.sp, color = axisColor)
    val density = LocalDensity.current

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

                    fun dayAt(x: Float): Int = ((x - leftPadPx) / slotWidthPx).toInt() + 1

                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val downDay = dayAt(down.position.x)
                        val downValid = downDay in 1..daysInMonth && pointsByDay.containsKey(downDay)

                        selectedDay = when {
                            downValid && downDay == selectedDay -> null
                            downValid -> downDay
                            else -> null
                        }

                        drag(down.id) { change ->
                            val day = dayAt(change.position.x)
                            val valid = day in 1..daysInMonth && pointsByDay.containsKey(day)
                            if (valid) selectedDay = day
                            change.consume()
                        }
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
                val barHeight = (valueSelector(point) / maxWeight) * plotHeight
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
                val barTopFraction = valueSelector(point) / maxWeight
                val barTopDp = plotHeightDp * (1f - barTopFraction)
                val centerXDp = leftPadDp + (day - 0.5f) * slotWidthDp
                val tooltipWidthDp = with(density) { tooltipWidthPx.toDp() }
                val tooltipHeightDp = with(density) { tooltipHeightPx.toDp() }
                val gapDp = 6.dp
                val rawXDp = centerXDp - tooltipWidthDp / 2f
                val minXDp = leftPadDp
                val maxXDp = (leftPadDp + plotWidthDp - tooltipWidthDp).coerceAtLeast(leftPadDp)
                val clampedXDp = rawXDp.coerceIn(minXDp, maxXDp)
                val clampedYDp = (barTopDp - tooltipHeightDp - gapDp).coerceAtLeast(0.dp)
                val sets = setDetailsBySession[point.sessionId].orEmpty().sortedBy { it.setNumber }
                Card(
                    modifier = Modifier
                        .offset(x = clampedXDp, y = clampedYDp)
                        .onGloballyPositioned { coordinates ->
                            tooltipWidthPx = coordinates.size.width
                            tooltipHeightPx = coordinates.size.height
                        },
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            formatDecimal(valueSelector(point)) + unitLabel,
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        sets.forEach { set ->
                            Text(
                                text = setDetailFormatter(set),
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailStatRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TrendStatRow(percentChange: Double?) {
    val displayText: String
    val valueColor: Color

    if (percentChange == null) {
        displayText = "-"
        valueColor = MaterialTheme.colorScheme.onSurface
    } else {
        val displayPercent =
            (percentChange * 10).roundToInt() / 10.0

        displayText =
            "${if (displayPercent > 0) "+" else ""}$displayPercent%"

        valueColor = when {
            displayPercent > 0.0 -> Color(0xFF2E7D32)
            displayPercent < 0.0 -> Color(0xFFC62828)
            else -> MaterialTheme.colorScheme.onSurface
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Recent Trend",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = displayText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}