package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.StudySplitDatabase
import com.example.data.repository.StudySplitRepository
import com.example.ui.screens.GroupDashboardScreen
import com.example.ui.screens.GroupSelectionScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.StudySplitViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Local constructor DI setup
        val database = StudySplitDatabase.getDatabase(applicationContext)
        val repository = StudySplitRepository(database.dao())
        val viewModelFactory = StudySplitViewModel.Factory(application, repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[StudySplitViewModel::class.java]

        setContent {
            MyApplicationTheme {
                val currentProfile by viewModel.currentProfile.collectAsState()
                val currentGroup by viewModel.currentGroup.collectAsState()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Crossfade(
                        targetState = Pair(currentProfile, currentGroup),
                        label = "MainScreenNavigation"
                    ) { (profile, group) ->
                        when {
                            profile == null -> {
                                OnboardingScreen(
                                    viewModel = viewModel,
                                    onOnboardingComplete = {
                                        // Auto-transitions since profile is reactively updated
                                    }
                                )
                            }
                            group == null -> {
                                GroupSelectionScreen(
                                    viewModel = viewModel,
                                    onGroupSelected = { selectedGroup ->
                                        viewModel.selectGroup(selectedGroup)
                                    }
                                )
                            }
                            else -> {
                                GroupDashboardScreen(
                                    group = group,
                                    viewModel = viewModel,
                                    onBack = {
                                        viewModel.selectGroup(null)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
