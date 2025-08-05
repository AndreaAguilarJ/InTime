package com.momentum.app.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
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
import com.momentum.app.ui.screen.auth.WelcomeScreen
import com.momentum.app.ui.screen.auth.SignUpScreen
import com.momentum.app.ui.screen.auth.SignInScreen
import com.momentum.app.ui.screen.onboarding.EnhancedOnboardingScreen
import com.momentum.app.minimal.MinimalPhoneScreen
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed class Screen(val route: String, val icon: ImageVector, val titleRes: Int) {
    object Today : Screen("today", Icons.Filled.Home, R.string.nav_today)
    object MyLife : Screen("my_life", Icons.Filled.Person, R.string.nav_my_life)
    object Analytics : Screen("analytics", Icons.Filled.Analytics, R.string.nav_analytics)
    object Focus : Screen("focus", Icons.Filled.Psychology, R.string.nav_focus)
    object Settings : Screen("settings", Icons.Filled.Settings, R.string.nav_settings)
    object MinimalPhone : Screen("minimal_phone", Icons.Filled.PhoneAndroid, R.string.nav_minimal_phone)
    object Subscription : Screen("subscription", Icons.Filled.Star, R.string.nav_subscription)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentumApp() {
    val context = LocalContext.current
    val application = context.applicationContext as MomentumApplication
    
    // Authentication state
    val isLoggedIn by application.appwriteService.isLoggedIn.collectAsState()
    var authState by remember { mutableStateOf<AuthState>(AuthState.Loading) }
    var isOnboardingCompleted by remember { mutableStateOf<Boolean?>(null) }
    
    LaunchedEffect(isLoggedIn) {
        authState = if (isLoggedIn) {
            // Check onboarding status
            AuthState.Authenticated
        } else {
            AuthState.NotAuthenticated
        }
    }
    
    when (authState) {
        AuthState.Loading -> {
            LoadingScreen()
        }
        AuthState.NotAuthenticated -> {
            AuthenticationFlow(
                application = application,
                onAuthSuccess = { authState = AuthState.Authenticated }
            )
        }
        AuthState.Authenticated -> {
            // Check onboarding status for authenticated users
            LaunchedEffect(Unit) {
                val currentUser = application.appwriteService.currentUser.value
                currentUser?.let { user ->
                    application.appwriteUserRepository.getUserSettings(user.id).collect { settings ->
                        isOnboardingCompleted = settings?.isOnboardingCompleted ?: false
                    }
                }
            }
            
            when (isOnboardingCompleted) {
                null -> LoadingScreen()
                false -> {
                    EnhancedOnboardingScreen(
                        onCompleted = { isOnboardingCompleted = true },
                        onBirthDateSelected = { birthDate ->
                            // Save birth date to Appwrite
                        },
                        onColorPreferencesSelected = { livedColor, futureColor ->
                            // Save color preferences to Appwrite
                        }
                    )
                }
                true -> {
                    // Check if user has seen the tutorial
                    var showTutorial by remember { mutableStateOf(false) }
                    
                    LaunchedEffect(Unit) {
                        application.userRepository.getUserSettings().collect { settings ->
                            showTutorial = !(settings?.hasSeenTutorial ?: false)
                        }
                    }
                    
                    if (showTutorial) {
                        com.momentum.app.ui.screen.tutorial.AppTutorialScreen(
                            onCompleted = { 
                                showTutorial = false
                                // Mark tutorial as seen
                                kotlinx.coroutines.GlobalScope.launch {
                                    application.userRepository.markTutorialAsSeen()
                                }
                            }
                        )
                    } else {
                        MainAppContent(application)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun AuthenticationFlow(
    application: MomentumApplication,
    onAuthSuccess: () -> Unit
) {
    var currentScreen by remember { mutableStateOf<AuthScreen>(AuthScreen.Welcome) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    when (currentScreen) {
        AuthScreen.Welcome -> {
            WelcomeScreen(
                onSignUpClick = { currentScreen = AuthScreen.SignUp },
                onSignInClick = { currentScreen = AuthScreen.SignIn }
            )
        }
        AuthScreen.SignUp -> {
            SignUpScreen(
                onSignUp = { name, email, password ->
                    isLoading = true
                    errorMessage = null
                    coroutineScope.launch {
                        try {
                            val result = application.appwriteService.createAccount(email, password, name)
                            if (result.isSuccess) {
                                onAuthSuccess()
                            } else {
                                errorMessage = com.momentum.app.util.ErrorHandler.getErrorMessage(
                                    result.exceptionOrNull() ?: Exception("Error desconocido"), 
                                    context
                                )
                            }
                        } catch (e: Exception) {
                            errorMessage = com.momentum.app.util.ErrorHandler.getErrorMessage(e, context)
                        } finally {
                            isLoading = false
                        }
                    }
                },
                onBackToWelcome = { 
                    currentScreen = AuthScreen.Welcome
                    errorMessage = null
                },
                isLoading = isLoading,
                errorMessage = errorMessage
            )
        }
        AuthScreen.SignIn -> {
            SignInScreen(
                onSignIn = { email, password ->
                    isLoading = true
                    errorMessage = null
                    coroutineScope.launch {
                        try {
                            val result = application.appwriteService.login(email, password)
                            if (result.isSuccess) {
                                onAuthSuccess()
                            } else {
                                errorMessage = com.momentum.app.util.ErrorHandler.getErrorMessage(
                                    result.exceptionOrNull() ?: Exception("Error desconocido"), 
                                    context
                                )
                            }
                        } catch (e: Exception) {
                            errorMessage = com.momentum.app.util.ErrorHandler.getErrorMessage(e, context)
                        } finally {
                            isLoading = false
                        }
                    }
                },
                onBackToWelcome = { 
                    currentScreen = AuthScreen.Welcome
                    errorMessage = null
                },
                isLoading = isLoading,
                errorMessage = errorMessage
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainAppContent(application: MomentumApplication) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val minimalPhoneManager = application.minimalPhoneManager
    val isMinimalModeEnabled by minimalPhoneManager.isMinimalModeEnabled.collectAsState()
    
    if (isMinimalModeEnabled) {
        // Show minimal phone interface
        MinimalPhoneScreen(
            minimalPhoneManager = minimalPhoneManager,
            onSettingsClick = {
                navController.navigate(Screen.Settings.route)
            }
        )
    } else {
        // Show normal app interface
        val screens = listOf(
            Screen.Today,
            Screen.MyLife,
            Screen.Analytics,
            Screen.Focus,
            Screen.MinimalPhone,
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
                    DashboardScreen(
                        viewModel = viewModel,
                        isPremiumUser = application.subscriptionRepository.isPremiumUser(),
                        onUpgradeClick = {
                            navController.navigate(Screen.Subscription.route)
                        }
                    )
                }
                composable(Screen.MyLife.route) {
                    val viewModel: com.momentum.app.ui.viewmodel.LifeWeeksViewModel = viewModel(
                        factory = LifeWeeksViewModelFactory(application.userRepository)
                    )
                    LifeWeeksScreen(viewModel = viewModel)
                }
                composable(Screen.Analytics.route) {
                    com.momentum.app.ui.screen.analytics.AdvancedAnalyticsScreen(
                        isPremiumUser = application.subscriptionRepository.isPremiumUser(),
                        onUpgradeClick = {
                            navController.navigate(Screen.Subscription.route)
                        }
                    )
                }
                composable(Screen.Focus.route) {
                    com.momentum.app.ui.screen.focus.FocusSessionScreen(
                        isPremiumUser = application.subscriptionRepository.isPremiumUser(),
                        onUpgradeClick = {
                            navController.navigate(Screen.Subscription.route)
                        }
                    )
                }
                composable(Screen.MinimalPhone.route) {
                    MinimalPhoneScreen(
                        minimalPhoneManager = minimalPhoneManager,
                        onSettingsClick = {
                            navController.navigate(Screen.Settings.route)
                        }
                    )
                }
                composable(Screen.Subscription.route) {
                    com.momentum.app.ui.screen.subscription.SubscriptionScreen(
                        onSubscriptionSelected = { planId, isYearly ->
                            // Handle subscription purchase
                        },
                        onStartTrial = {
                            // Handle trial start
                        },
                        onBackClick = {
                            navController.popBackStack()
                        },
                        isTrialAvailable = application.subscriptionRepository.isTrialAvailable(),
                        remainingTrialDays = application.subscriptionRepository.getRemainingTrialDays()
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onNavigateToScreen = { screen ->
                            when (screen) {
                                "theme_settings" -> navController.navigate("theme_settings")
                                "backup_settings" -> navController.navigate("backup_settings")
                                "notification_settings" -> navController.navigate("notification_settings")
                                "account_settings" -> navController.navigate("account_settings")
                                "tutorial" -> navController.navigate("tutorial")
                                "about" -> navController.navigate("about")
                            }
                        }
                    )
                }
                
                // Theme Settings
                composable("theme_settings") {
                    com.momentum.app.ui.screen.settings.ThemeSettingsScreen(
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }
                
                // Backup Settings  
                composable("backup_settings") {
                    com.momentum.app.ui.screen.settings.BackupSettingsScreen(
                        onBackClick = {
                            navController.popBackStack()
                        },
                        backupSyncManager = application.backupSyncManager,
                        exportManager = application.exportManager
                    )
                }
                
                // Tutorial
                composable("tutorial") {
                    com.momentum.app.ui.screen.tutorial.AppTutorialScreen(
                        onCompleted = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}

private enum class AuthState {
    Loading,
    NotAuthenticated,
    Authenticated
}

private enum class AuthScreen {
    Welcome,
    SignUp,
    SignIn
}