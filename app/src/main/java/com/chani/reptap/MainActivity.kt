package com.chani.reptap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chani.reptap.data.local.db.AppDatabase
import com.chani.reptap.data.repository.CycleRepository
import com.chani.reptap.data.repository.WorkoutRepository
import com.chani.reptap.ui.details.WorkoutDetailsScreen
import com.chani.reptap.ui.details.WorkoutDetailsViewModel
import com.chani.reptap.ui.home.HomeScreen
import com.chani.reptap.ui.home.HomeViewModel
import com.chani.reptap.ui.theme.RepTapTheme

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
                    val navController = rememberNavController()
                    val homeViewModel = remember { HomeViewModel(workoutRepository, cycleRepository) }

                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                viewModel = homeViewModel,
                                onStartSession = { day ->
                                    // wired to Active Workout screen next step
                                },
                                onViewDetails = { day ->
                                    navController.navigate("details/${day.id}/${day.name}")
                                }
                            )
                        }
                        composable(
                            route = "details/{dayId}/{dayName}",
                            arguments = listOf(
                                navArgument("dayId") { type = NavType.LongType },
                                navArgument("dayName") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val dayId = backStackEntry.arguments?.getLong("dayId") ?: 0L
                            val dayName = backStackEntry.arguments?.getString("dayName") ?: ""
                            val detailsViewModel = remember(dayId) {
                                WorkoutDetailsViewModel(workoutRepository, dayId)
                            }
                            WorkoutDetailsScreen(
                                dayName = dayName,
                                viewModel = detailsViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}