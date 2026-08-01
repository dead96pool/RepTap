package com.chani.reptap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
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
import com.chani.reptap.ui.history.HistoryScreen
import com.chani.reptap.ui.history.HistoryViewModel
import com.chani.reptap.ui.home.HomeScreen
import com.chani.reptap.ui.home.HomeViewModel
import com.chani.reptap.ui.progress.ProgressScreen
import com.chani.reptap.ui.progress.ProgressViewModel
import com.chani.reptap.ui.theme.RepTapTheme
import com.chani.reptap.ui.workout.ActiveWorkoutScreen
import com.chani.reptap.ui.workout.ActiveWorkoutViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)

        val db = AppDatabase.getInstance(applicationContext)
        val workoutRepository = WorkoutRepository(db)
        val cycleRepository = CycleRepository(db)

        setContent {
            RepTapTheme {
                val view = androidx.compose.ui.platform.LocalView.current
                val darkIcons = !isSystemInDarkTheme()
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as android.app.Activity).window
                        androidx.core.view.WindowCompat.getInsetsController(window, view)
                            .isAppearanceLightStatusBars = darkIcons
                    }
                }

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

                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            val homeViewModel: HomeViewModel = viewModel(
                                factory = viewModelFactory {
                                    initializer { HomeViewModel(workoutRepository, cycleRepository) }
                                }
                            )
                            LaunchedEffect(Unit) {
                                homeViewModel.loadToday()
                            }
                            HomeScreen(
                                viewModel = homeViewModel,
                                onStartSession = { day -> navController.navigate("workout/${day.id}") },
                                onViewDetails = { day -> navController.navigate("details/${day.id}/${day.name}") },
                                onViewHistory = { navController.navigate("history") },
                                onViewProgress = { navController.navigate("progress") }
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
                            val detailsViewModel: WorkoutDetailsViewModel = viewModel(
                                key = "details_$dayId",
                                factory = viewModelFactory {
                                    initializer { WorkoutDetailsViewModel(workoutRepository, dayId) }
                                }
                            )
                            WorkoutDetailsScreen(
                                dayName = dayName,
                                viewModel = detailsViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "workout/{dayId}",
                            arguments = listOf(navArgument("dayId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val dayId = backStackEntry.arguments?.getLong("dayId") ?: 0L
                            val workoutViewModel: ActiveWorkoutViewModel = viewModel(
                                key = "workout_$dayId",
                                factory = viewModelFactory {
                                    initializer { ActiveWorkoutViewModel(workoutRepository, cycleRepository, dayId) }
                                }
                            )
                            ActiveWorkoutScreen(
                                viewModel = workoutViewModel,
                                onFinish = { navController.popBackStack("home", inclusive = false) },
                                onQuit = { navController.popBackStack("home", inclusive = false) }
                            )
                        }

                        composable("history") {
                            val historyViewModel: HistoryViewModel = viewModel(
                                factory = viewModelFactory {
                                    initializer { HistoryViewModel(workoutRepository) }
                                }
                            )
                            HistoryScreen(viewModel = historyViewModel, onBack = { navController.popBackStack() })
                        }

                        composable("progress") {
                            val progressViewModel: ProgressViewModel = viewModel(
                                factory = viewModelFactory {
                                    initializer { ProgressViewModel(workoutRepository) }
                                }
                            )
                            ProgressScreen(viewModel = progressViewModel, onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}