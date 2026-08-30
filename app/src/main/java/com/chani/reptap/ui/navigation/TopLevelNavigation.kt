package com.chani.reptap.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chani.reptap.ui.components.RepTapTopAppBar

enum class TopLevelDestination(
    val pageIndex: Int,
    val label: String
) {
    PROGRESS(0, "Progress"),
    HOME(1, "Home"),
    HISTORY(2, "History")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepTapTopLevelBar(
    pagerState: PagerState,
    onDestinationSelected: (TopLevelDestination) -> Unit
) {
    val destinations = TopLevelDestination.entries

    Column {
        RepTapTopAppBar(
            title = "RepTap"
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
                    modifier = Modifier.height(40.dp),
                    text = {
                        Text(destination.label)
                    }
                )
            }
        }
    }
}