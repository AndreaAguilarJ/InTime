package com.momentummm.app.minimal

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momentummm.app.data.AppDatabase
import com.momentummm.app.util.LifeWeeksCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinimalPhoneScreen(
    minimalPhoneManager: MinimalPhoneManager,
    onSettingsClick: () -> Unit,
    onExitMinimalMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val isMinimalModeEnabled by minimalPhoneManager.isMinimalModeEnabled.collectAsState()
    var currentScreen by remember { mutableStateOf<MinimalScreen>(MinimalScreen.Home) }
    var showExitConfirm by remember { mutableStateOf(false) }
    
    if (isMinimalModeEnabled) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                when {
                    targetState == MinimalScreen.Home -> {
                        slideInHorizontally { -it } + fadeIn() togetherWith 
                        slideOutHorizontally { it } + fadeOut()
                    }
                    else -> {
                        slideInHorizontally { it } + fadeIn() togetherWith 
                        slideOutHorizontally { -it } + fadeOut()
                    }
                }.using(SizeTransform(clip = false))
            },
            label = "screen_transition"
        ) { targetScreen ->
            when (targetScreen) {
                MinimalScreen.Home -> {
                    MinimalLauncherContent(
                        minimalPhoneManager = minimalPhoneManager,
                        onSettingsClick = onSettingsClick,
                        onExitMinimalMode = onExitMinimalMode,
                        onDialerClick = { currentScreen = MinimalScreen.Dialer },
                        onAppListClick = { currentScreen = MinimalScreen.AppList },
                        modifier = modifier
                    )
                }
                MinimalScreen.Dialer -> {
                    MinimalDialerScreen(
                        onCall = { phoneNumber ->
                            minimalPhoneManager.makePhoneCall(phoneNumber)
                            currentScreen = MinimalScreen.Home
                        },
                        onBack = { currentScreen = MinimalScreen.Home },
                        modifier = modifier
                    )
                }
                MinimalScreen.AppList -> {
                    MinimalAppListScreen(
                        minimalPhoneManager = minimalPhoneManager,
                        onBack = { currentScreen = MinimalScreen.Home },
                        modifier = modifier
                    )
                }
            }
        }
    } else {
        MinimalModeToggleScreen(
            onEnableMinimalMode = {
                coroutineScope.launch {
                    minimalPhoneManager.enableMinimalMode()
                }
            },
            modifier = modifier
        )
    }
}

private enum class MinimalScreen {
    Home,
    Dialer,
    AppList
}

@Composable
private fun MinimalLauncherContent(
    minimalPhoneManager: MinimalPhoneManager,
    onSettingsClick: () -> Unit,
    onExitMinimalMode: () -> Unit,
    onDialerClick: () -> Unit,
    onAppListClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    var showExitConfirm by remember { mutableStateOf(false) }
    val customApp by minimalPhoneManager.customApp.collectAsState()
    val customAppInfo = remember(customApp) { minimalPhoneManager.getCustomAppInfo() }
    var showAppSelector by remember { mutableStateOf(false) }

    // Animación de entrada
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    // Obtener datos de semanas de vida
    val context = LocalContext.current
    var lifeWeeksData by remember { mutableStateOf<LifeWeeksCalculator.LifeWeeksData?>(null) }

    LaunchedEffect(Unit) {
        val database = AppDatabase.getDatabase(context)
        val userSettings = withContext(Dispatchers.IO) {
            database.userDao().getUserSettingsSync()
        }
        lifeWeeksData = userSettings?.birthDate?.let { birthDate ->
            LifeWeeksCalculator.calculateLifeWeeks(birthDate)
        }
    }

    val essentialApps = listOf(
        EssentialApp("Teléfono", Icons.Default.Phone) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            if (!minimalPhoneManager.openDialer()) {
                onDialerClick()
            }
        },
        EssentialApp("Mensajes", Icons.Default.Message) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            minimalPhoneManager.openMessages()
        },
        EssentialApp("Contactos", Icons.Default.Contacts) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            minimalPhoneManager.openContacts()
        },
        EssentialApp("Ajustes", Icons.Default.Settings) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            if (!minimalPhoneManager.openSettings()) {
                onSettingsClick()
            }
        }
    )

    // Fondo con gradiente sutil
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header minimalista
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(500)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = getCurrentDate(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    FilledTonalIconButton(
                        onClick = { 
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            showExitConfirm = true 
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Salir del modo mínimo",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        
            Spacer(modifier = Modifier.height(8.dp))
        
            // Reloj con barra de progreso circular de semanas de vida
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(700, delayMillis = 200)) + scaleIn(
                    initialScale = 0.8f,
                    animationSpec = tween(700, delayMillis = 200)
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(260.dp)
                    ) {
                        // Barra de progreso circular de semanas de vida
                        lifeWeeksData?.let { data ->
                            CircularLifeWeeksProgress(
                                progressPercentage = data.progressPercentage,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Reloj en el centro
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Hora actual con animación
                            var currentTime by remember { mutableStateOf(getCurrentTime()) }

                            LaunchedEffect(Unit) {
                                while (true) {
                                    currentTime = getCurrentTime()
                                    kotlinx.coroutines.delay(1000)
                                }
                            }

                            Text(
                                text = currentTime,
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = 52.sp,
                                    letterSpacing = 2.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Light
                            )

                            lifeWeeksData?.let { data ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = "${data.progressPercentage.toInt()}% de vida",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        
            // Essential apps grid - diseño mejorado
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(500, delayMillis = 400)) + slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(500, delayMillis = 400)
                )
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Grid 2x2 para apps esenciales
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        essentialApps.take(2).forEach { app ->
                            EssentialAppCard(
                                app = app,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        essentialApps.drop(2).forEach { app ->
                            EssentialAppCard(
                                app = app,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // App personalizada centrada
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (customAppInfo != null) {
                            CustomAppCard(
                                appInfo = customAppInfo,
                                onClick = { 
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    minimalPhoneManager.openCustomApp() 
                                },
                                onLongClick = { showAppSelector = true },
                                modifier = Modifier.widthIn(min = 150.dp, max = 170.dp)
                            )
                        } else {
                            AddCustomAppCard(
                                onClick = { showAppSelector = true },
                                modifier = Modifier.widthIn(min = 150.dp, max = 170.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Botón para ver todas las apps - más discreto
                    FilledTonalButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAppListClick()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Apps,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Todas las apps",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showAppSelector) {
        AppSelectorDialog(
            minimalPhoneManager = minimalPhoneManager,
            currentSelection = customApp,
            onDismiss = { showAppSelector = false },
            onAppSelected = { packageName ->
                coroutineScope.launch {
                    minimalPhoneManager.setCustomApp(packageName)
                    showAppSelector = false
                }
            }
        )
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            icon = { 
                Icon(
                    Icons.Default.Close, 
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { 
                Text(
                    "¿Salir del modo mínimo?",
                    textAlign = TextAlign.Center
                ) 
            },
            text = { 
                Text(
                    "Se restaurará la experiencia completa de Momentum con todas las funciones.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        showExitConfirm = false
                        onExitMinimalMode()
                    }
                ) {
                    Text("Sí, salir")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExitConfirm = false }) {
                    Text("Continuar aquí")
                }
            }
        )
    }
}

@Composable
private fun CircularLifeWeeksProgress(
    progressPercentage: Float,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

    // Animación suave del progreso
    val animatedProgress by animateFloatAsState(
        targetValue = progressPercentage,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "progress_animation"
    )

    Canvas(modifier = modifier) {
        val strokeWidth = 10.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val radius = diameter / 2
        val topLeft = Offset(
            x = (size.width - diameter) / 2,
            y = (size.height - diameter) / 2
        )

        // Círculo de fondo (semanas restantes)
        drawArc(
            color = surfaceVariant,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = Size(diameter, diameter),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Círculo de progreso con gradiente (semanas vividas)
        val sweepAngle = (animatedProgress / 100f) * 360f
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(primaryColor, tertiaryColor, primaryColor),
                center = Offset(size.width / 2, size.height / 2)
            ),
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = topLeft,
            size = Size(diameter, diameter),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Punto indicador brillante en el progreso actual
        val angleInRadians = Math.toRadians((sweepAngle - 90).toDouble())
        val indicatorX = (size.width / 2) + (radius * Math.cos(angleInRadians)).toFloat()
        val indicatorY = (size.height / 2) + (radius * Math.sin(angleInRadians)).toFloat()

        // Halo exterior
        drawCircle(
            color = primaryColor.copy(alpha = 0.3f),
            radius = strokeWidth,
            center = Offset(indicatorX, indicatorY)
        )
        
        // Punto central
        drawCircle(
            color = primaryColor,
            radius = strokeWidth / 2 + 2.dp.toPx(),
            center = Offset(indicatorX, indicatorY)
        )
    }
}

@Composable
private fun MinimalModeToggleScreen(
    onEnableMinimalMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📱",
            style = MaterialTheme.typography.displayLarge
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Modo Teléfono Mínimo",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Limita tu teléfono a las funciones esenciales: llamadas, mensajes y contactos.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Características:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("• Solo apps esenciales disponibles")
                Text("• Interfaz simplificada")
                Text("• Menos distracciones")
                Text("• Enfoque en comunicación")
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onEnableMinimalMode,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Activar modo mínimo")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EssentialAppCard(
    app: EssentialApp,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "card_scale"
    )

    Card(
        onClick = app.onClick,
        modifier = modifier
            .aspectRatio(1f)
            .heightIn(min = 90.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        interactionSource = interactionSource
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = app.icon,
                        contentDescription = app.name,
                        modifier = Modifier.size(26.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = app.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun AllowedAppsManagementScreen(
    minimalPhoneManager: MinimalPhoneManager,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val installedApps by remember { mutableStateOf(minimalPhoneManager.getInstalledApps()) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Gestionar apps permitidas",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Selecciona qué apps estarán disponibles en modo mínimo:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(installedApps) { appInfo ->
                AppPermissionItem(
                    appInfo = appInfo,
                    onToggle = { isAllowed ->
                        coroutineScope.launch {
                            if (isAllowed) {
                                minimalPhoneManager.addAllowedApp(appInfo.packageName)
                            } else {
                                minimalPhoneManager.removeAllowedApp(appInfo.packageName)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AppPermissionItem(
    appInfo: AppInfo,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = appInfo.appName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = appInfo.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Switch(
                checked = appInfo.isAllowed,
                onCheckedChange = onToggle
            )
        }
    }
}

private data class EssentialApp(
    val name: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomAppCard(
    appInfo: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(1f)
            .heightIn(min = 100.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 8.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "📱",
                    style = MaterialTheme.typography.displaySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = appInfo.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    textAlign = TextAlign.Center
                )
            }
            TextButton(
                onClick = onLongClick,
                contentPadding = PaddingValues(4.dp)
            ) {
                Text(
                    text = "Cambiar",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCustomAppCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(1f)
            .heightIn(min = 100.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            width = 2.dp,
            brush = androidx.compose.ui.graphics.SolidColor(
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Agregar app",
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Agregar app",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AppSelectorDialog(
    minimalPhoneManager: MinimalPhoneManager,
    currentSelection: String?,
    onDismiss: () -> Unit,
    onAppSelected: (String?) -> Unit
) {
    val installedApps = remember {
        minimalPhoneManager.getInstalledApps().filter {
            !it.isCore // Excluir apps del sistema
        }
    }
    var searchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            installedApps
        } else {
            installedApps.filter {
                it.appName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Seleccionar aplicación adicional")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Elige una app para agregar al teléfono mínimo (solo una):",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar app") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (currentSelection != null) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Quitar app actual",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.Medium
                                    )
                                    TextButton(onClick = { onAppSelected(null) }) {
                                        Text("Quitar")
                                    }
                                }
                            }
                        }
                    }

                    items(filteredApps) { appInfo ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = if (appInfo.packageName == currentSelection) {
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            } else {
                                CardDefaults.cardColors()
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = appInfo.appName,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = appInfo.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (appInfo.packageName == currentSelection) {
                                    Text(
                                        text = "✓",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    TextButton(
                                        onClick = { onAppSelected(appInfo.packageName) }
                                    ) {
                                        Text("Seleccionar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

private fun getCurrentTime(): String {
    return java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        .format(java.util.Date())
}

private fun getCurrentDate(): String {
    return java.text.SimpleDateFormat("EEE, d MMM", java.util.Locale("es", "ES"))
        .format(java.util.Date())
}