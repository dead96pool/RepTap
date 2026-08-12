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
import com.chani.reptap.ui.progress.ExerciseDetailScreen
import com.chani.reptap.ui.progress.ExerciseDetailViewModel
import com.chani.reptap.ui.theme.RepTapTheme
import com.chani.reptap.ui.workout.ActiveWorkoutScreen
import com.chani.reptap.ui.workout.ActiveWorkoutViewModel
import com.chani.reptap.ui.history.SessionDetailScreen
import com.chani.reptap.ui.history.SessionDetailViewModel
import com.chani.reptap.ui.progress.AllExercisesScreen
import com.chani.reptap.ui.progress.AllExercisesViewModel

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
                            HistoryScreen(
                                viewModel = historyViewModel,
                                onBack = { navController.popBackStack() },
                                onSessionClick = { session ->
                                    navController.navigate("session_detail/${session.id}/${session.workoutDayId}/${session.workoutDayName}/${session.startedAt}/${session.finishedAt}")
                                }
                            )
                        }
                        composable(
                            route = "session_detail/{sessionId}/{workoutDayId}/{dayName}/{startedAt}/{finishedAt}",
                            arguments = listOf(
                                navArgument("sessionId") { type = NavType.LongType },
                                navArgument("workoutDayId") { type = NavType.LongType },
                                navArgument("dayName") { type = NavType.StringType },
                                navArgument("startedAt") { type = NavType.LongType },
                                navArgument("finishedAt") { type = NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
                            val workoutDayId = backStackEntry.arguments?.getLong("workoutDayId") ?: 0L
                            val dayName = backStackEntry.arguments?.getString("dayName") ?: ""
                            val startedAt = backStackEntry.arguments?.getLong("startedAt") ?: 0L
                            val finishedAt = backStackEntry.arguments?.getLong("finishedAt") ?: 0L

                            val currentSession = com.chani.reptap.data.local.dao.SessionRecap(
                                id = sessionId, workoutDayId = workoutDayId, workoutDayName = dayName,
                                startedAt = startedAt, finishedAt = finishedAt, totalSets = 0, totalReps = 0
                            )

                            val detailViewModel: SessionDetailViewModel = viewModel(
                                key = "session_detail_$sessionId",
                                factory = viewModelFactory {
                                    initializer { SessionDetailViewModel(workoutRepository, sessionId, workoutDayId, startedAt) }
                                }
                            )

                            SessionDetailScreen(
                                dayName = dayName,
                                currentSession = currentSession,
                                viewModel = detailViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("progress") {
                            val progressViewModel: ProgressViewModel = viewModel(
                                factory = viewModelFactory {
                                    initializer { ProgressViewModel(workoutRepository) }
                                }
                            )
                            ProgressScreen(
                                viewModel = progressViewModel,
                                onBack = { navController.popBackStack() },
                                onExerciseClick = { improvement ->
                                    navController.navigate("exercise_detail/${improvement.exerciseId}/${improvement.exerciseName}")
                                },
                                onViewAllExercises = { navController.navigate("all_exercises") }
                            )
                        }

                        composable("all_exercises") {
                            val allExercisesViewModel: AllExercisesViewModel = viewModel(
                                factory = viewModelFactory {
                                    initializer { AllExercisesViewModel(workoutRepository) }
                                }
                            )
                            AllExercisesScreen(
                                viewModel = allExercisesViewModel,
                                onBack = { navController.popBackStack() },
                                onExerciseClick = { item ->
                                    navController.navigate("exercise_detail/${item.id}/${item.name}")
                                }
                            )
                        }

                        composable(
                            route = "exercise_detail/{exerciseId}/{exerciseName}",
                            arguments = listOf(
                                navArgument("exerciseId") { type = NavType.LongType },
                                navArgument("exerciseName") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val exerciseId = backStackEntry.arguments?.getLong("exerciseId") ?: 0L
                            val exerciseName = backStackEntry.arguments?.getString("exerciseName") ?: ""
                            val exerciseDetailViewModel: ExerciseDetailViewModel = viewModel(
                                key = "exercise_detail_$exerciseId",
                                factory = viewModelFactory {
                                    initializer { ExerciseDetailViewModel(workoutRepository, exerciseId) }
                                }
                            )
                            ExerciseDetailScreen(
                                exerciseName = exerciseName,
                                viewModel = exerciseDetailViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}