package com.chani.reptap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.chani.reptap.data.local.db.AppDatabase
import com.chani.reptap.data.repository.CycleRepository
import com.chani.reptap.data.repository.WorkoutRepository
import com.chani.reptap.ui.home.HomeScreen
import com.chani.reptap.ui.home.HomeViewModel
import com.chani.reptap.ui.theme.RepTapTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getInstance(applicationContext)
        val workoutRepository = WorkoutRepository(db)
        val cycleRepository = CycleRepository(db)

        setContent {
            RepTapTheme {
                var isSeeded by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    workoutRepository.seedIfEmpty()
                    isSeeded = true
                }

                if (!isSeeded) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val homeViewModel = remember { HomeViewModel(workoutRepository, cycleRepository) }
                    HomeScreen(
                        viewModel = homeViewModel,
                        onStartSession = { day ->
                            // wired to Active Workout screen next step
                        },
                        onViewDetails = { day ->
                            // wired to Workout Details screen next step
                        }
                    )
                }
            }
        }
    }
}