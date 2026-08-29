package com.chani.reptap.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import kotlin.math.ceil
import kotlin.math.floor

enum class TopLevelDestination(
    val pageIndex: Int,
    val label: String
) {
    HOME(
        pageIndex = 0,
        label = "Home"
    ),
    PROGRESS(
        pageIndex = 1,
        label = "Progress"
    ),
    HISTORY(
        pageIndex = 2,
        label = "History"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepTapTopLevelBar(
    pagerState: PagerState,
    onDestinationSelected: (TopLevelDestination) -> Unit
) {
    val destinations = TopLevelDestination.entries

    Column {
        TopAppBar(
            title = {
                Text("RepTap")
            }
        )

        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            edgePadding = 16.dp
        ) {
            destinations.forEach { destination ->
                Tab(
                    selected = pagerState.currentPage == destination.pageIndex,
                    onClick = {
                        onDestinationSelected(destination)
                    },
                    text = {
                        Text(destination.label)
                    }
                )
            }
        }
    }
}