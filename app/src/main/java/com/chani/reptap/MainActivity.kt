package com.chani.reptap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.chani.reptap.data.local.db.AppDatabase
import com.chani.reptap.data.repository.WorkoutRepository
import com.chani.reptap.ui.home.HomeScreen
import com.chani.reptap.ui.home.HomeViewModel
import com.chani.reptap.ui.theme.RepTapTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getInstance(applicationContext)
        val repository = WorkoutRepository(db)

        lifecycleScope.launch {
            repository.seedIfEmpty()
        }

        val homeViewModel = HomeViewModel(repository)

        setContent {
            RepTapTheme {
                HomeScreen(
                    viewModel = homeViewModel,
                    onDayClick = { day ->
                        // we'll wire real navigation to the workout screen next step
                    }
                )
            }
        }
    }
}