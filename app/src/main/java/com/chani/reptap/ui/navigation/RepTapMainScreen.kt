package com.chani.reptap.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.chani.reptap.data.local.dao.SessionRecap
import com.chani.reptap.data.local.entity.WorkoutDay
import com.chani.reptap.ui.history.HistoryScreen
import com.chani.reptap.ui.history.HistoryViewModel
import com.chani.reptap.ui.home.HomeScreen
import com.chani.reptap.ui.home.HomeViewModel
import com.chani.reptap.ui.progress.ExerciseImprovement
import com.chani.reptap.ui.progress.ProgressScreen
import com.chani.reptap.ui.progress.ProgressViewModel
import kotlinx.coroutines.launch


private const val TOP_LEVEL_PAGE_COUNT = 3
private const val SINGLE_PAGE_ANIMATION_MS = 220
private const val TWO_PAGE_ANIMATION_MS = 320

@Composable
fun RepTapMainScreen(
    homeViewModel: HomeViewModel,
    progressViewModel: ProgressViewModel,
    historyViewModel: HistoryViewModel,
    onStartSession: (WorkoutDay) -> Unit,
    onExerciseClick: (ExerciseImprovement) -> Unit,
    onViewAllExercises: () -> Unit,
    onSessionClick: (SessionRecap) -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = TopLevelDestination.HOME.pageIndex,
        pageCount = { TOP_LEVEL_PAGE_COUNT }
    )

    val coroutineScope = rememberCoroutineScope()

    fun animateToPage(pageIndex: Int) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(
                page = pageIndex
            )
        }
    }

    BackHandler(
        enabled = pagerState.currentPage != TopLevelDestination.HOME.pageIndex
    ) {
        animateToPage(TopLevelDestination.HOME.pageIndex)
    }

    Scaffold(
        topBar = {
            RepTapTopLevelBar(
                pagerState = pagerState,
                onDestinationSelected = { destination ->
                    if (destination.pageIndex != pagerState.currentPage) {
                        animateToPage(destination.pageIndex)
                    }
                }
            )
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            beyondViewportPageCount = 2
        ) { page ->
            when (page) {
                TopLevelDestination.HOME.pageIndex -> {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onStartSession = onStartSession
                    )
                }

                TopLevelDestination.PROGRESS.pageIndex -> {
                    ProgressScreen(
                        viewModel = progressViewModel,
                        onExerciseClick = onExerciseClick,
                        onViewAllExercises = onViewAllExercises
                    )
                }

                TopLevelDestination.HISTORY.pageIndex -> {
                    HistoryScreen(
                        viewModel = historyViewModel,
                        onSessionClick = onSessionClick
                    )
                }
            }
        }
    }
}