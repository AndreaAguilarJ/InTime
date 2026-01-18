package com.momentummm.app.ui.screen

import android.app.Activity
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
import com.momentummm.app.MomentumApplication
import com.momentummm.app.R
import com.momentummm.app.ui.viewmodel.DashboardViewModelFactory
import com.momentummm.app.ui.viewmodel.LifeWeeksViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import com.momentummm.app.ui.screen.auth.WelcomeScreen
import com.momentummm.app.ui.screen.auth.SignUpScreen
import com.momentummm.app.ui.screen.auth.SignInScreen
import com.momentummm.app.minimal.MinimalPhoneScreen
import kotlinx.coroutines.launch
import com.momentummm.app.data.UserPreferencesRepository
import java.time.format.DateTimeFormatter
import androidx.glance.appwidget.updateAll
import com.momentummm.app.widget.LifeWeeksWidget
import com.momentummm.app.ui.screen.onboarding.EnhancedOnboardingScreen
import com.momentummm.app.ui.screen.tutorial.AppTutorialScreen
import com.momentummm.app.data.appwrite.models.AppwriteUserSettings
import com.momentummm.app.ui.settings.NotificationSettingsScreen
import com.momentummm.app.ui.viewmodel.OnboardingViewModel
import com.momentummm.app.ui.viewmodel.OnboardingViewModelFactory

sealed class Screen(val route: String, val icon: ImageVector, val titleRes: Int) {
    object Today : Screen("today", Icons.Filled.Home, R.string.nav_today)
    object MyLife : Screen("my_life", Icons.Filled.Person, R.string.nav_my_life)
    object Analytics : Screen("analytics", Icons.Filled.Analytics, R.string.nav_analytics)
    object Focus : Screen("focus", Icons.Filled.Psychology, R.string.nav_focus)
    object Settings : Screen("settings", Icons.Filled.Settings, R.string.nav_settings)
    object MinimalPhone : Screen("minimal_phone", Icons.Filled.PhoneAndroid, R.string.nav_minimal_phone)
    object Subscription : Screen("subscription", Icons.Filled.Star, R.string.nav_subscription)
}

enum class AuthState {
    Loading,
    NotAuthenticated,
    Authenticated
}

enum class AuthScreen {
    Welcome,
    SignUp,
    SignIn
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentumApp() {
    val context = LocalContext.current
    val application = context.applicationContext as MomentumApplication
    val coroutineScope = rememberCoroutineScope()

    // Authentication state
    val isLoggedIn by application.appwriteService.isLoggedIn.collectAsState()
    val isAuthReady by application.appwriteService.isAuthReady.collectAsState()
    var authState by remember { mutableStateOf(AuthState.Loading) }
    var cachedSettings by remember { mutableStateOf<AppwriteUserSettings?>(null) }
    val onboardingCompleted by UserPreferencesRepository
        .isOnboardingCompletedFlow(context)
        .collectAsState(initial = false)

    LaunchedEffect(isAuthReady, isLoggedIn) {
        authState = if (!isAuthReady) {
            AuthState.Loading
        } else if (isLoggedIn) {
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
            // Sync settings for authenticated users
            LaunchedEffect(Unit) {
                val currentUser = application.appwriteService.currentUser.value
                currentUser?.let { user ->
                    val appwriteSettingsFlow = application.appwriteUserRepository.getUserSettings(user.id)
                    val localSettingsFlow = application.userRepository.getUserSettings()

                    kotlinx.coroutines.flow.combine(appwriteSettingsFlow, localSettingsFlow) { appwrite, local ->
                        appwrite to local
                    }.collect { (appwrite, local) ->
                        cachedSettings = appwrite
                        val appwriteCompleted = appwrite?.isOnboardingCompleted ?: false
                        val localCompleted = local?.isOnboardingCompleted ?: false

                        if (appwriteCompleted != localCompleted) {
                            if (appwriteCompleted) {
                                application.userRepository.completeOnboarding()
                            } else if (localCompleted) {
                                application.appwriteUserRepository.completeOnboarding(user.id)
                            }
                        }

                        if (appwriteCompleted || localCompleted) {
                            UserPreferencesRepository.setOnboardingCompleted(context, true)
                        }

                        val dobIso = appwrite?.birthDate
                        if (!dobIso.isNullOrBlank()) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                UserPreferencesRepository.setDobIso(context, dobIso)
                            }
                        }
                    }
                }
            }

            val startDestination = if (onboardingCompleted) "home" else "onboarding"
            androidx.compose.runtime.key(startDestination) {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
                    composable("onboarding") {
                        val onboardingViewModel: OnboardingViewModel = viewModel(
                            factory = OnboardingViewModelFactory(context, application.userRepository)
                        )
                        EnhancedOnboardingScreen(
                            viewModel = onboardingViewModel,
                            onCompleted = {
                                navController.navigate("home") {
                                    popUpTo("onboarding") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("home") {
                        MainAppContent(
                            application = application,
                            cachedSettings = cachedSettings
                        )
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
    var currentScreen by remember { mutableStateOf(AuthScreen.Welcome) }
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
                                errorMessage = com.momentummm.app.util.ErrorHandler.getErrorMessage(
                                    result.exceptionOrNull() ?: Exception("Error desconocido"),
                                    context
                                )
                            }
                        } catch (e: Exception) {
                            errorMessage = com.momentummm.app.util.ErrorHandler.getErrorMessage(e, context)
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
                                errorMessage = com.momentummm.app.util.ErrorHandler.getErrorMessage(
                                    result.exceptionOrNull() ?: Exception("Error desconocido"),
                                    context
                                )
                            }
                        } catch (e: Exception) {
                            errorMessage = com.momentummm.app.util.ErrorHandler.getErrorMessage(e, context)
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
private fun MainAppContent(
    application: MomentumApplication,
    cachedSettings: AppwriteUserSettings?
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val minimalPhoneManager = application.minimalPhoneManager
    val isMinimalModeEnabled by minimalPhoneManager.isMinimalModeEnabled.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    if (isMinimalModeEnabled) {
        MinimalPhoneScreen(
            minimalPhoneManager = minimalPhoneManager,
            onSettingsClick = {
                coroutineScope.launch {
                    minimalPhoneManager.disableMinimalMode()
                }
                navController.navigate(Screen.Settings.route)
            },
            onExitMinimalMode = {
                coroutineScope.launch {
                    minimalPhoneManager.disableMinimalMode()
                }
            }
        )
    } else {
        val screens = listOf(
            Screen.Today,
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
                    val viewModel: com.momentummm.app.ui.viewmodel.DashboardViewModel = viewModel(
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
                    val viewModel: com.momentummm.app.ui.viewmodel.LifeWeeksViewModel = viewModel(
                        factory = LifeWeeksViewModelFactory(application.userRepository)
                    )
                    LifeWeeksScreen(viewModel = viewModel)
                }
                composable(Screen.Analytics.route) {
                    com.momentummm.app.ui.screen.analytics.AdvancedAnalyticsScreen(
                        isPremiumUser = application.subscriptionRepository.isPremiumUser(),
                        onUpgradeClick = {
                            navController.navigate(Screen.Subscription.route)
                        }
                    )
                }
                composable(Screen.Focus.route) {
                    com.momentummm.app.ui.screen.focus.FocusSessionScreen(
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
                        },
                        onExitMinimalMode = {
                            coroutineScope.launch {
                                minimalPhoneManager.disableMinimalMode()
                            }
                            navController.navigate(Screen.Today.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = false
                                }
                            }
                        }
                    )
                }
                composable(Screen.Subscription.route) {
                    com.momentummm.app.ui.screen.subscription.SubscriptionScreen(
                        onSubscriptionSelected = { _, _ -> },
                        onStartTrial = { },
                        onBackClick = { navController.popBackStack() },
                        isTrialAvailable = application.subscriptionRepository.isTrialAvailable(),
                        remainingTrialDays = application.subscriptionRepository.getRemainingTrialDays()
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onNavigateToScreen = { screen ->
                            when (screen) {
                                "password_protection" -> navController.navigate("password_protection")
                                "mi_vida_en_semanas" -> navController.navigate("mi_vida_en_semanas")
                                "theme_settings" -> navController.navigate("theme_settings")
                                "backup_settings" -> navController.navigate("backup_settings")
                                "sync_settings" -> navController.navigate("sync_settings")
                                "notification_settings" -> navController.navigate("notification_settings")
                                "account_settings" -> navController.navigate("account_settings")
                                "permissions_settings" -> navController.navigate("permissions_settings")
                                "tutorial" -> navController.navigate("tutorial")
                                "about" -> navController.navigate("about")
                                "launcher_settings" -> navController.navigate("launcher_settings")
                                "app_limits" -> navController.navigate("app_limits")
                                "in_app_blocking" -> navController.navigate("in_app_blocking")
                                "website_blocks" -> navController.navigate("website_blocks")
                            }
                        }
                    )
                }

                composable("theme_settings") {
                    com.momentummm.app.ui.screen.settings.ThemeSettingsScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable("mi_vida_en_semanas") {
                    val viewModel: com.momentummm.app.ui.viewmodel.LifeWeeksViewModel = viewModel(
                        factory = LifeWeeksViewModelFactory(application.userRepository)
                    )
                    LifeWeeksScreen(viewModel = viewModel)
                }

                composable("backup_settings") {
                    com.momentummm.app.ui.screen.settings.BackupSettingsScreen(
                        onBackClick = { navController.popBackStack() },
                        backupSyncManager = application.backupSyncManager,
                        exportManager = application.exportManager
                    )
                }

                composable("sync_settings") {
                    com.momentummm.app.ui.screen.settings.SyncSettingsScreen(
                        autoSyncManager = application.autoSyncManager,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("tutorial") {
                    AppTutorialScreen(
                        existingDobIso = cachedSettings?.birthDate,
                        initialLivedColor = cachedSettings?.livedWeeksColor,
                        initialFutureColor = cachedSettings?.futureWeeksColor,
                        onBirthDateSelected = { birthDate ->
                            val userId = application.appwriteService.currentUser.value?.id ?: return@AppTutorialScreen
                            val iso = birthDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                            coroutineScope.launch {
                                // Guardar en Appwrite
                                val existing = cachedSettings ?: AppwriteUserSettings(userId = userId, birthDate = "")
                                val updated = existing.copy(birthDate = iso)
                                application.appwriteUserRepository.updateUserSettings(userId, updated)
                                
                                // Guardar en preferencias del usuario
                                UserPreferencesRepository.setDobIso(context, iso)
                                
                                // Guardar en base de datos local de Room
                                val dateFormatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                val date = dateFormatter.parse(iso)
                                if (date != null) {
                                    application.userRepository.setBirthDate(date)
                                }
                                
                                // Actualizar widgets
                                LifeWeeksWidget().updateAll(context)
                            }
                        },
                        onColorPreferencesSelected = { livedColor, futureColor ->
                            val userId = application.appwriteService.currentUser.value?.id ?: return@AppTutorialScreen
                            coroutineScope.launch {
                                val existing = cachedSettings ?: AppwriteUserSettings(userId = userId, birthDate = cachedSettings?.birthDate ?: "")
                                val updated = existing.copy(livedWeeksColor = livedColor, futureWeeksColor = futureColor)
                                application.appwriteUserRepository.updateUserSettings(userId, updated)
                                UserPreferencesRepository.setWidgetColors(context, livedColor, futureColor)
                                LifeWeeksWidget().updateAll(context)
                            }
                        },
                        onCompleted = {
                            coroutineScope.launch {
                                application.userRepository.markTutorialAsSeen()
                            }
                            navController.popBackStack()
                        }
                    )
                }

                composable("app_limits") {
                    com.momentummm.app.ui.screen.applimits.AppLimitsScreen(
                        onBackClick = { navController.popBackStack() },
                        onNavigateToWhitelist = { navController.navigate("app_whitelist") }
                    )
                }

                composable("in_app_blocking") {
                    com.momentummm.app.ui.inappblock.InAppBlockScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("app_whitelist") {
                    com.momentummm.app.ui.screen.applimits.AppWhitelistScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("website_blocks") {
                    com.momentummm.app.ui.screen.websiteblock.WebsiteBlockScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable("account_settings") {
                    com.momentummm.app.ui.screen.settings.AccountSettingsScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable("permissions_settings") {
                    com.momentummm.app.ui.screen.settings.PermissionsSettingsScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable("notification_settings") {
                    NotificationSettingsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("about") {
                    com.momentummm.app.ui.screen.settings.AboutScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable("password_protection") {
                    com.momentummm.app.ui.password.PasswordProtectionManageScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToSetup = { navController.navigate("password_setup") }
                    )
                }

                composable("password_setup") {
                    com.momentummm.app.ui.password.PasswordProtectionSetupScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
