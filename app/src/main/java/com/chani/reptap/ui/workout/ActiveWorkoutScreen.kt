package com.chani.reptap.ui.workout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chani.reptap.data.local.entity.ExerciseTrackingType
import com.chani.reptap.ui.components.RepTapTopAppBar
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import com.chani.reptap.ui.theme.TextPrimary


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    viewModel: ActiveWorkoutViewModel,
    onFinish: () -> Unit,
    onQuit: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showQuitConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = state.phase != SetPhase.WORKOUT_COMPLETE) {
        showQuitConfirm = true
    }

    Scaffold(
        topBar = {
            RepTapTopAppBar(
                title = state.currentExercise?.exerciseName ?: "Workout",
                actions = {
                    TextButton(
                        onClick = { showQuitConfirm = true }
                    ) {
                        Text("Quit Session")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.isLoading -> CircularProgressIndicator()
                state.phase == SetPhase.WORKOUT_COMPLETE -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Workout Complete!", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = onFinish) { Text("Back to Home") }
                    }
                }
                state.phase == SetPhase.COUNTING_REPS -> {
                    val currentExercise = state.currentExercise

                    if (
                        currentExercise?.trackingType ==
                        ExerciseTrackingType.TIME
                    ) {
                        TimedSetArea(
                            setNumber = state.currentSetNumber,
                            totalSets = currentExercise.targetSets,
                            secondsElapsed = state.timedSecondsElapsed,
                            targetMinSec =
                                currentExercise.targetDurationMinSec ?: 0,
                            targetMaxSec =
                                currentExercise.targetDurationMaxSec ?: 0,
                            isPerSide = currentExercise.isPerSide,
                            hasStarted = state.hasTimedSetStarted,
                            isRunning = state.isTimedSetRunning,
                            onStart = {
                                viewModel.startTimedSet()
                            },
                            onPause = {
                                viewModel.pauseTimedSet()
                            },
                            onFinishSet = {
                                viewModel.finishSet()
                            }
                        )
                    } else {
                        RepTapArea(
                            exerciseName =
                                currentExercise?.exerciseName ?: "",
                            setNumber = state.currentSetNumber,
                            totalSets =
                                currentExercise?.targetSets ?: 1,
                            repsTapped = state.repsTapped,
                            repMin =
                                currentExercise?.repRangeMin ?: 0,
                            repMax =
                                currentExercise?.repRangeMax ?: 0,
                            isPerSide =
                                currentExercise?.isPerSide ?: false,
                            onTap = {
                                viewModel.tapRep()
                            },
                            onUndo = {
                                viewModel.undoTap()
                            },
                            onFinishSet = {
                                viewModel.finishSet()
                            }
                        )
                    }
                }
                else -> {
                    RestTimerArea(
                        secondsRemaining = state.secondsRemaining,
                        isBeforeNextExercise = state.phase == SetPhase.RESTING_BEFORE_NEXT_EXERCISE,
                        onSkipRest = { viewModel.skipRestToFiveSeconds() }
                    )
                }
            }
        }
    }

    if (showQuitConfirm) {
        AlertDialog(
            onDismissRequest = { showQuitConfirm = false },
            title = { Text("Quit session?") },
            text = { Text("Are you sure you want to quit the workout?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.quitSession()
                    showQuitConfirm = false
                    onQuit()
                }) { Text("Quit") }
            },
            dismissButton = {
                TextButton(onClick = { showQuitConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun RepTapArea(
    exerciseName: String,
    setNumber: Int,
    totalSets: Int,
    repsTapped: Int,
    repMin: Int,
    repMax: Int,
    isPerSide: Boolean,
    onTap: () -> Unit,
    onUndo: () -> Unit,
    onFinishSet: () -> Unit
) {
    val canFinish = repsTapped >= repMin

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(8.dp))
        Text(
            "Set $setNumber of $totalSets",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            "Target: $repMin-$repMax reps" + if (isPerSide) " each side" else "",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { onTap() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$repsTapped",
                        fontSize = 96.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider()

                Row(modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Button(
                        onClick = onUndo,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                            topStart = 0.dp, topEnd = 0.dp, bottomStart = 12.dp, bottomEnd = 0.dp
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("-1")
                    }
                    Button(
                        onClick = onFinishSet,
                        enabled = canFinish,
                        modifier = Modifier.weight(2f).fillMaxHeight(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                            topStart = 0.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 12.dp
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues()
                    ) {
                        Text("Finish Set")
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun TimedSetArea(
    setNumber: Int,
    totalSets: Int,
    secondsElapsed: Int,
    targetMinSec: Int,
    targetMaxSec: Int,
    isPerSide: Boolean,
    hasStarted: Boolean,
    isRunning: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onFinishSet: () -> Unit
) {
    val canFinish =
        secondsElapsed >= targetMinSec

    val isPaused =
        hasStarted && !isRunning

    val minutes =
        secondsElapsed / 60

    val seconds =
        secondsElapsed % 60

    val formattedTime =
        "%02d:%02d".format(
            minutes,
            seconds
        )

    /*
     * Final approved pause treatment.
     */
    val timerBlur by animateDpAsState(
        targetValue =
            if (isPaused) {
                1.9.dp
            } else {
                0.dp
            },
        animationSpec =
            tween(durationMillis = 180),
        label = "TimerBlur"
    )

    val timerAlpha by animateFloatAsState(
        targetValue =
            if (isPaused) {
                0.68f
            } else {
                1f
            },
        animationSpec =
            tween(durationMillis = 180),
        label = "TimerAlpha"
    )

    val pausedLabelAlpha by animateFloatAsState(
        targetValue =
            if (isPaused) {
                1f
            } else {
                0f
            },
        animationSpec =
            tween(durationMillis = 160),
        label = "PausedLabelAlpha"
    )

    val pausedLabelColor =
        if (isSystemInDarkTheme()) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            TextPrimary
        }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Set $setNumber of $totalSets",
            style =
                MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign =
                androidx.compose.ui.text.style.TextAlign.Center
        )

        Text(
            text = buildString {
                append("Target: ")

                if (targetMaxSec > 0) {
                    append(
                        "$targetMinSec-$targetMaxSec sec"
                    )
                } else {
                    append("$targetMinSec sec")
                }

                if (isPerSide) {
                    append(" each side")
                }
            },
            style =
                MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign =
                androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(
                            MaterialTheme
                                .colorScheme
                                .primaryContainer
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {
                    Column(
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {
                        /*
                         * This space always remains present so
                         * the counter does not move when PAUSED
                         * appears or disappears.
                         */
                        Box(
                            modifier =
                                Modifier.height(32.dp),
                            contentAlignment =
                                Alignment.Center
                        ) {
                            Text(
                                text = "PAUSED",
                                style =
                                    MaterialTheme
                                        .typography
                                        .labelLarge
                                        .copy(
                                            fontSize = 14.sp,
                                            fontWeight =
                                                FontWeight.Bold,
                                            letterSpacing = 7.sp
                                        ),
                                color = pausedLabelColor,
                                modifier =
                                    Modifier.alpha(
                                        pausedLabelAlpha
                                    )
                            )
                        }

                        /*
                         * Paused timer:
                         *
                         * 1.9dp subtle edge blur
                         * 68% opacity
                         *
                         * No card/background colour changes.
                         */
                        Text(
                            text = formattedTime,
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .blur(timerBlur)
                                .alpha(timerAlpha)
                        )
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Button(
                        onClick = {
                            if (isRunning) {
                                onPause()
                            } else {
                                onStart()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape =
                            androidx.compose.foundation.shape
                                .RoundedCornerShape(
                                    topStart = 0.dp,
                                    topEnd = 0.dp,
                                    bottomStart = 12.dp,
                                    bottomEnd = 0.dp
                                ),
                        contentPadding =
                            PaddingValues(),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    MaterialTheme
                                        .colorScheme
                                        .surfaceVariant,
                                contentColor =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )
                    ) {
                        Text(
                            text = when {
                                isRunning -> "Pause"
                                hasStarted -> "Resume"
                                else -> "Start"
                            }
                        )
                    }

                    Button(
                        onClick = onFinishSet,
                        enabled = canFinish,
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight(),
                        shape =
                            androidx.compose.foundation.shape
                                .RoundedCornerShape(
                                    topStart = 0.dp,
                                    topEnd = 0.dp,
                                    bottomStart = 0.dp,
                                    bottomEnd = 12.dp
                                ),
                        contentPadding =
                            PaddingValues()
                    ) {
                        Text("Finish Set")
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )
    }
}

@Composable
private fun RestTimerArea(
    secondsRemaining: Int,
    isBeforeNextExercise: Boolean,
    onSkipRest: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (isBeforeNextExercise) "Rest before next exercise" else "Rest",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "$secondsRemaining",
            fontSize = 120.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF3F51B5)
        )
        Text("seconds", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(32.dp))
        if (secondsRemaining > 5) {
            OutlinedButton(
                onClick = onSkipRest,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Skip Rest")
            }
        }
    }
}