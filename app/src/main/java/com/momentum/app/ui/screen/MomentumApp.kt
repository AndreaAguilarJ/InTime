package com.momentum.app.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.momentum.app.MomentumApplication
import com.momentum.app.R
import com.momentum.app.ui.viewmodel.DashboardViewModelFactory
import com.momentum.app.ui.viewmodel.LifeWeeksViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel

sealed class Screen(val route: String, val icon: ImageVector, val titleRes: Int) {
    object Today : Screen("today", Icons.Filled.Home, R.string.nav_today)
    object MyLife : Screen("my_life", Icons.Filled.Person, R.string.nav_my_life)
    object Settings : Screen("settings", Icons.Filled.Settings, R.string.nav_settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentumApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val application = context.applicationContext as MomentumApplication
    
    // Check if onboarding is completed
    val userRepository = application.userRepository
    var isOnboardingCompleted by remember { mutableStateOf<Boolean?>(null) }
    
    LaunchedEffect(Unit) {
        userRepository.getUserSettings().collect { settings ->
            isOnboardingCompleted = settings?.isOnboardingCompleted ?: false
        }
    }
    
    when (isOnboardingCompleted) {
        null -> {
            // Loading state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        false -> {
            // Show onboarding
            val onboardingViewModel: com.momentum.app.ui.viewmodel.OnboardingViewModel = viewModel(
                factory = com.momentum.app.ui.viewmodel.OnboardingViewModelFactory(application.userRepository)
            )
            OnboardingScreen(
                viewModel = onboardingViewModel,
                onCompleted = { isOnboardingCompleted = true }
            )
        }
        true -> {
            // Show main app
            MainAppContent(application)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainAppContent(application: MomentumApplication) {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    val screens = listOf(
        Screen.Today,
        Screen.MyLife,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(stringResource(screen.titleRes)) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Today.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Today.route) {
                val viewModel: com.momentum.app.ui.viewmodel.DashboardViewModel = viewModel(
                    factory = DashboardViewModelFactory(
                        application.userRepository,
                        application.usageStatsRepository,
                        application.quotesRepository,
                        context
                    )
                )
                DashboardScreen(viewModel = viewModel)
            }
            composable(Screen.MyLife.route) {
                val viewModel: com.momentum.app.ui.viewmodel.LifeWeeksViewModel = viewModel(
                    factory = LifeWeeksViewModelFactory(application.userRepository)
                )
                LifeWeeksScreen(viewModel = viewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}