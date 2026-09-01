package com.chani.reptap.ui.details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chani.reptap.ui.components.RepTapTopAppBar
import com.chani.reptap.data.local.entity.ExerciseTrackingType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailsScreen(
    dayName: String,
    viewModel: WorkoutDetailsViewModel,
    onBack: () -> Unit
) {
    val exercises by viewModel.exercises.collectAsState()

    Scaffold(
        topBar = {
            RepTapTopAppBar(
                title = dayName,
                onBack = onBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(exercises) { ex ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "${ex.orderIndex}. ${ex.exerciseName}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        val sideNote = if (ex.isPerSide) " each side" else ""
                        val targetText =
                            when (ex.trackingType) {
                                ExerciseTrackingType.REPS -> {
                                    "${ex.targetSets} sets x ${ex.targetReps} reps$sideNote"
                                }

                                ExerciseTrackingType.TIME -> {
                                    "${ex.targetSets} sets x ${ex.targetDurationSec ?: 0} sec$sideNote"
                                }
                            }

                        Text(targetText)
                        Text("Rest between sets: ${ex.restBetweenSetsSec}s")
                        if (ex.restBeforeNextSec > 0) {
                            Text("Rest before next exercise: ${ex.restBeforeNextSec}s")
                        }
                        Text("Weight: ${ex.defaultWeightKg} kg")
                    }
                }
            }
        }
    }
}