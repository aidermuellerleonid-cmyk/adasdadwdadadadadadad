package de.fitapp.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.fitapp.app.ui.AppViewModel
import de.fitapp.app.ui.ProfileState
import de.fitapp.app.ui.diary.DiaryScreen
import de.fitapp.app.ui.onboarding.OnboardingScreen
import de.fitapp.app.ui.profile.ProfileScreen
import de.fitapp.app.ui.progress.ProgressScreen
import de.fitapp.app.ui.sleep.SleepScreen
import de.fitapp.app.ui.theme.FitAppTheme
import de.fitapp.app.ui.training.TrainingScreen

private sealed class Dest(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Diary : Dest("diary", "Tagebuch", Icons.Filled.RestaurantMenu)
    object Training : Dest("training", "Training", Icons.Filled.FitnessCenter)
    object Progress : Dest("progress", "Fortschritt", Icons.Filled.Insights)
    object Sleep : Dest("sleep", "Schlaf", Icons.Filled.Bedtime)
    object Profile : Dest("profile", "Profil", Icons.Filled.Person)
}

private val bottomDestinations = listOf(Dest.Diary, Dest.Training, Dest.Progress, Dest.Sleep, Dest.Profile)

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitAppTheme {
                val state by viewModel.profileState.collectAsState()

                when (val s = state) {
                    is ProfileState.Loading -> {
                        // Ladezustand, während das Profil aus der Datenbank gelesen wird.
                        Surface(modifier = Modifier) {}
                    }
                    is ProfileState.Loaded -> {
                        val profile = s.profile
                        if (profile == null || !profile.onboardingCompleted) {
                            OnboardingScreen(viewModel = viewModel)
                        } else {
                            MainScaffold(viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainScaffold(viewModel: AppViewModel) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination
                bottomDestinations.forEach { dest ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Dest.Diary.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Dest.Diary.route) { DiaryScreen(viewModel) }
            composable(Dest.Training.route) { TrainingScreen(viewModel) }
            composable(Dest.Progress.route) { ProgressScreen(viewModel) }
            composable(Dest.Sleep.route) { SleepScreen(viewModel) }
            composable(Dest.Profile.route) { ProfileScreen(viewModel) }
        }
    }
}
