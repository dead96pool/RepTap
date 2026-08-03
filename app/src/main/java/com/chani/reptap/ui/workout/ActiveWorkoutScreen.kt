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
            TopAppBar(
                title = { Text(state.currentExercise?.exerciseName ?: "Workout") },
                actions = {
                    TextButton(onClick = { showQuitConfirm = true }) {
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
                    RepTapArea(
                        exerciseName = state.currentExercise?.exerciseName ?: "",
                        setNumber = state.currentSetNumber,
                        totalSets = state.currentExercise?.targetSets ?: 1,
                        repsTapped = state.repsTapped,
                        repMin = state.currentExercise?.repRangeMin ?: 0,
                        repMax = state.currentExercise?.repRangeMax ?: 0,
                        isPerSide = state.currentExercise?.isPerSide ?: false,
                        onTap = { viewModel.tapRep() },
                        onUndo = { viewModel.undoTap() },
                        onFinishSet = { viewModel.finishSet() }
                    )
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
        Spacer(Modifier.height(24.dp))
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
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp)
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
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun RestTimerArea(
    secondsRemaining: Int,
    isBeforeNextExercise: Boolean,
    onSkipRest: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
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