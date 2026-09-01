package com.chani.reptap

import android.os.Bundle
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import com.chani.reptap.ui.history.HistoryViewModel
import com.chani.reptap.ui.home.HomeViewModel
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
import com.chani.reptap.ui.navigation.RepTapMainScreen
import com.chani.reptap.ui.builder.WorkoutBuilderScreen
import com.chani.reptap.ui.builder.WorkoutBuilderViewModel
import com.chani.reptap.ui.builder.EditWorkoutExerciseScreen
import com.chani.reptap.ui.builder.EditWorkoutExerciseViewModel

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

                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        enterTransition = { EnterTransition.None },
                        exitTransition = { ExitTransition.None },
                        popEnterTransition = { EnterTransition.None },
                        popExitTransition = { ExitTransition.None }
                    ) {
                        composable("home") {
                            val homeViewModel: HomeViewModel = viewModel(
                                key = "home",
                                factory = viewModelFactory {
                                    initializer {
                                        HomeViewModel(
                                            workoutRepository,
                                            cycleRepository
                                        )
                                    }
                                }
                            )

                            val progressViewModel: ProgressViewModel = viewModel(
                                key = "progress",
                                factory = viewModelFactory {
                                    initializer {
                                        ProgressViewModel(workoutRepository)
                                    }
                                }
                            )

                            val historyViewModel: HistoryViewModel = viewModel(
                                key = "history",
                                factory = viewModelFactory {
                                    initializer {
                                        HistoryViewModel(workoutRepository)
                                    }
                                }
                            )

                            RepTapMainScreen(
                                homeViewModel = homeViewModel,
                                progressViewModel = progressViewModel,
                                historyViewModel = historyViewModel,

                                onStartSession = { day ->
                                    navController.navigate("workout/${day.id}")
                                },

                                onExerciseClick = { improvement ->
                                    navController.navigate(
                                        "exercise_detail/${improvement.exerciseId}/${improvement.exerciseName}"
                                    )
                                },

                                onViewAllExercises = {
                                    navController.navigate("all_exercises")
                                },

                                onSessionClick = { session ->
                                    navController.navigate(
                                        "session_detail/${session.id}/${session.workoutDayId}/${session.workoutDayName}/${session.startedAt}/${session.finishedAt}/${session.totalSets}/${session.totalReps}"
                                    )
                                },

                                onEditWorkout = { day ->
                                    navController.navigate(
                                        "workout_builder/${day.id}"
                                    )
                                }
                            )
                        }

                        composable(
                            route = "workout_builder/{dayId}",
                            arguments = listOf(
                                navArgument("dayId") {
                                    type = NavType.LongType
                                }
                            )
                        ) { backStackEntry ->

                            val dayId =
                                backStackEntry.arguments
                                    ?.getLong("dayId")
                                    ?: 0L

                            val workoutBuilderViewModel:
                                    WorkoutBuilderViewModel =
                                viewModel(
                                    key = "workout_builder_$dayId",
                                    factory = viewModelFactory {
                                        initializer {
                                            WorkoutBuilderViewModel(
                                                repository =
                                                    workoutRepository,
                                                dayId = dayId
                                            )
                                        }
                                    }
                                )

                            WorkoutBuilderScreen(
                                viewModel =
                                    workoutBuilderViewModel,
                                onBack = {
                                    navController.popBackStack()
                                },
                                onExerciseClick = { exercise ->
                                    navController.navigate(
                                        "edit_workout_exercise/${exercise.id}"
                                    )
                                }
                            )
                        }

                        composable(
                            route =
                                "edit_workout_exercise/{workoutExerciseId}",
                            arguments = listOf(
                                navArgument(
                                    "workoutExerciseId"
                                ) {
                                    type = NavType.LongType
                                }
                            )
                        ) { backStackEntry ->

                            val workoutExerciseId =
                                backStackEntry.arguments
                                    ?.getLong(
                                        "workoutExerciseId"
                                    )
                                    ?: 0L

                            val editViewModel:
                                    EditWorkoutExerciseViewModel =
                                viewModel(
                                    key =
                                        "edit_workout_exercise_" +
                                                workoutExerciseId,
                                    factory =
                                        viewModelFactory {
                                            initializer {
                                                EditWorkoutExerciseViewModel(
                                                    repository =
                                                        workoutRepository,
                                                    workoutExerciseId =
                                                        workoutExerciseId
                                                )
                                            }
                                        }
                                )

                            EditWorkoutExerciseScreen(
                                viewModel = editViewModel,
                                onBack = {
                                    navController.popBackStack()
                                },
                                onSaved = {
                                    navController.popBackStack()
                                }
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

                        composable(
                            route = "session_detail/{sessionId}/{workoutDayId}/{dayName}/{startedAt}/{finishedAt}/{totalSets}/{totalReps}",
                            arguments = listOf(
                                navArgument("sessionId") { type = NavType.LongType },
                                navArgument("workoutDayId") { type = NavType.LongType },
                                navArgument("dayName") { type = NavType.StringType },
                                navArgument("startedAt") { type = NavType.LongType },
                                navArgument("finishedAt") { type = NavType.LongType },
                                navArgument("totalSets") { type = NavType.IntType },
                                navArgument("totalReps") { type = NavType.IntType }
                            )
                        ) { backStackEntry ->
                            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
                            val workoutDayId = backStackEntry.arguments?.getLong("workoutDayId") ?: 0L
                            val dayName = backStackEntry.arguments?.getString("dayName") ?: ""
                            val startedAt = backStackEntry.arguments?.getLong("startedAt") ?: 0L
                            val finishedAt = backStackEntry.arguments?.getLong("finishedAt") ?: 0L
                            val totalSets = backStackEntry.arguments?.getInt("totalSets") ?: 0
                            val totalReps = backStackEntry.arguments?.getInt("totalReps") ?: 0

                            val currentSession = com.chani.reptap.data.local.dao.SessionRecap(
                                id = sessionId,
                                workoutDayId = workoutDayId,
                                workoutDayName = dayName,
                                startedAt = startedAt,
                                finishedAt = finishedAt,
                                totalSets = totalSets,
                                totalReps = totalReps,
                                avgWeightKg = 0f
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