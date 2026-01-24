@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@file:Suppress("OPT_IN_USAGE", "EXPERIMENTAL_API_USAGE")

package com.momentummm.app.ui.screen.tutorial

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.momentummm.app.R
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TutorialStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val tips: List<String> = emptyList(),
    val content: (@Composable () -> Unit)? = null,
    val requiresConfirmation: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppTutorialScreen(
    onCompleted: () -> Unit,
    existingDobIso: String? = null,
    initialLivedColor: String? = null,
    initialFutureColor: String? = null,
    onBirthDateSelected: (LocalDate) -> Unit = {},
    onColorPreferencesSelected: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    // Estados para DOB y colores
    var selectedBirthDate by remember { mutableStateOf<LocalDate?>(existingDobIso?.let { runCatching { LocalDate.parse(it) }.getOrNull() }) }
    var livedColor by remember { mutableStateOf(initialLivedColor ?: "#FF6B6B") }
    var futureColor by remember { mutableStateOf(initialFutureColor ?: "#E8E8E8") }

    // Construcción dinámica de pasos: incluye DOB solo si no existe
    val steps = buildList<TutorialStep> {
        add(
            TutorialStep(
                title = stringResource(R.string.tutorial_step_welcome_title),
                description = stringResource(R.string.tutorial_step_welcome_desc),
                icon = Icons.Default.Info,
                tips = listOf(
                    stringResource(R.string.tutorial_step_welcome_tip_1),
                    stringResource(R.string.tutorial_step_welcome_tip_2)
                )
            )
        )
        if (selectedBirthDate == null) {
            add(
                TutorialStep(
                    title = stringResource(R.string.tutorial_step_birthdate_title),
                    description = stringResource(R.string.tutorial_step_birthdate_desc),
                    icon = Icons.Default.CalendarMonth,
                    content = {
                        BirthDateStepContent(
                            selectedDate = selectedBirthDate,
                            onDateSelected = { newDate ->
                                selectedBirthDate = newDate
                                // Guardar inmediatamente la DOB
                                onBirthDateSelected(newDate)
                            }
                        )
                    },
                    requiresConfirmation = true,
                    tips = listOf(
                        stringResource(R.string.tutorial_step_birthdate_tip_1),
                        stringResource(R.string.tutorial_step_birthdate_tip_2)
                    )
                )
            )
        }
        add(
            TutorialStep(
                title = stringResource(R.string.tutorial_step_colors_title),
                description = stringResource(R.string.tutorial_step_colors_desc),
                icon = Icons.Default.Palette,
                content = {
                    ColorCustomizationStepContent(
                        livedColor = livedColor,
                        futureColor = futureColor,
                        onLivedColorChanged = { livedColor = it },
                        onFutureColorChanged = { futureColor = it }
                    )
                },
                tips = listOf(
                    stringResource(R.string.tutorial_step_colors_tip_1),
                    stringResource(R.string.tutorial_step_colors_tip_2)
                )
            )
        )
        add(
            TutorialStep(
                title = stringResource(R.string.tutorial_step_done_title),
                description = stringResource(R.string.tutorial_step_done_desc),
                icon = Icons.Default.CheckCircle,
                tips = listOf(
                    stringResource(R.string.tutorial_step_done_tip_1),
                    stringResource(R.string.tutorial_step_done_tip_2),
                    stringResource(R.string.tutorial_step_done_tip_3)
                )
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { steps.size })
    
    // Animación para el progreso
    val animatedProgress by animateFloatAsState(
        targetValue = (pagerState.currentPage + 1).toFloat() / steps.size,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                )
            )
    ) {
        TopAppBar(
            title = { 
                Text(
                    stringResource(R.string.tutorial_topbar_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                ) 
            },
            actions = {
                TextButton(
                    onClick = onCompleted,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) { 
                    Text(
                        stringResource(R.string.tutorial_skip),
                        style = MaterialTheme.typography.labelLarge
                    ) 
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Indicador de progreso mejorado con animación
        Column {
            LinearProgressIndicator(
                progress = animatedProgress,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            // Texto de progreso
            Text(
                text = stringResource(
                    R.string.tutorial_step_progress,
                    pagerState.currentPage + 1,
                    steps.size
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            pageSpacing = 16.dp
        ) { page ->
            val step = steps[page]
            
            // Animación de entrada para cada página
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(400)) + 
                        slideInHorizontally(animationSpec = tween(400)) { it / 3 },
                exit = fadeOut(animationSpec = tween(200))
            ) {
                if (step.content != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp)
                    ) { step.content.invoke() }
                } else {
                    TutorialStepContent(
                        step = com.momentummm.app.ui.screen.tutorial.TutorialStep(
                            title = step.title,
                            description = step.description,
                            icon = step.icon,
                            tips = step.tips
                        ),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        TutorialNavigationBar(
            pagerState = pagerState,
            totalSteps = steps.size,
            onCompleted = {
                // Persistir preferencia de colores
                onColorPreferencesSelected(livedColor, futureColor)
                // Ya se guardó DOB al seleccionarla (si aplicaba)
                onCompleted()
            },
            isCurrentStepConfirmable = !steps[pagerState.currentPage].requiresConfirmation || selectedBirthDate != null
        )
    }
}

// Reutilización: contenido del paso básico
@Composable
private fun TutorialStepContent(
    step: com.momentummm.app.ui.screen.tutorial.TutorialStep,
    modifier: Modifier = Modifier
) {
    // Animación de escala para el icono
    var iconVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        iconVisible = true
    }
    
    val iconScale by animateFloatAsState(
        targetValue = if (iconVisible) 1f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "icon_scale"
    )
    
    LazyColumn(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        item {
            // Icon con animación
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .size(96.dp)
                        .scale(iconScale),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = step.icon,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
        }
        
        item {
            // Title con mejor espaciado
            Text(
                text = step.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }

        item {
            // Description con mejor legibilidad
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.3f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )
        }

        if (step.tips.isNotEmpty()) {
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.tutorial_tips_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(4.dp)) }
            items(step.tips) { tip ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .align(Alignment.CenterVertically)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = tip,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TutorialNavigationBar(
    pagerState: PagerState,
    totalSteps: Int,
    onCompleted: () -> Unit,
    isCurrentStepConfirmable: Boolean
) {
    val coroutineScope = rememberCoroutineScope()
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (pagerState.currentPage > 0) {
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            animateToPage(pagerState, pagerState.currentPage - 1)
                        }
                    },
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 1.5.dp
                    )
                ) {
                    Icon(
                        Icons.Default.ArrowBack, 
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.tutorial_previous),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(80.dp))
            }

            PageIndicators(
                total = totalSteps,
                current = pagerState.currentPage
            )

            if (pagerState.currentPage < totalSteps - 1) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            animateToPage(pagerState, pagerState.currentPage + 1)
                        }
                    },
                    enabled = isCurrentStepConfirmable,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 4.dp,
                        disabledElevation = 0.dp
                    )
                ) {
                    Text(
                        stringResource(R.string.tutorial_next),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.ArrowForward, 
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Button(
                    onClick = onCompleted,
                    enabled = isCurrentStepConfirmable,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 3.dp,
                        pressedElevation = 6.dp,
                        disabledElevation = 0.dp
                    )
                ) {
                    Text(
                        stringResource(R.string.tutorial_start),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.Check, 
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PageIndicators(total: Int, current: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            val isSelected = index == current
            
            // Animación de tamaño
            val width by animateDpAsState(
                targetValue = if (isSelected) 24.dp else 8.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "indicator_width"
            )
            
            val height by animateDpAsState(
                targetValue = if (isSelected) 10.dp else 8.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "indicator_height"
            )
            
            Box(
                modifier = Modifier
                    .width(width)
                    .height(height)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        }
                    )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorCustomizationStepContent(
    livedColor: String,
    futureColor: String,
    onLivedColorChanged: (String) -> Unit,
    onFutureColorChanged: (String) -> Unit
) {
    val colorOptions = listOf(
        "#FF6B6B" to R.string.tutorial_color_red,
        "#4ECDC4" to R.string.tutorial_color_turquoise,
        "#45B7D1" to R.string.tutorial_color_blue,
        "#96CEB4" to R.string.tutorial_color_green,
        "#FFA07A" to R.string.tutorial_color_coral,
        "#DDA0DD" to R.string.tutorial_color_purple
    )
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                text = "🎨", 
                style = MaterialTheme.typography.displayLarge
            )
        }
        
        item {
            Text(
                text = stringResource(R.string.tutorial_customize_experience),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        
        item { Spacer(modifier = Modifier.height(8.dp)) }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Circle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.tutorial_lived_weeks_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colorOptions.chunked(3).first().forEach { (color, nameRes) ->
                            FilterChip(
                                selected = livedColor == color,
                                onClick = { onLivedColorChanged(color) },
                                label = { 
                                    Text(
                                        stringResource(nameRes),
                                        style = MaterialTheme.typography.labelMedium
                                    ) 
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colorOptions.chunked(3).last().forEach { (color, nameRes) ->
                            FilterChip(
                                selected = livedColor == color,
                                onClick = { onLivedColorChanged(color) },
                                label = { 
                                    Text(
                                        stringResource(nameRes),
                                        style = MaterialTheme.typography.labelMedium
                                    ) 
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(4.dp)) }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Circle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.tutorial_future_weeks_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colorOptions.chunked(3).first().forEach { (color, nameRes) ->
                            FilterChip(
                                selected = futureColor == color,
                                onClick = { onFutureColorChanged(color) },
                                label = { 
                                    Text(
                                        stringResource(nameRes),
                                        style = MaterialTheme.typography.labelMedium
                                    ) 
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colorOptions.chunked(3).last().forEach { (color, nameRes) ->
                            FilterChip(
                                selected = futureColor == color,
                                onClick = { onFutureColorChanged(color) },
                                label = { 
                                    Text(
                                        stringResource(nameRes),
                                        style = MaterialTheme.typography.labelMedium
                                    ) 
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun BirthDateStepContent(
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                text = "📅", 
                style = MaterialTheme.typography.displayLarge
            )
        }
        
        item {
            Text(
                text = stringResource(R.string.tutorial_birthdate_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        
        item {
            Text(
                text = stringResource(R.string.tutorial_birthdate_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        item { Spacer(modifier = Modifier.height(8.dp)) }
        
        item {
            Button(
                onClick = { showDatePicker = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedDate != null) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = selectedDate?.let {
                        val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
                        it.format(formatter)
                    } ?: stringResource(R.string.tutorial_birthdate_select_button),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        
        if (selectedDate != null) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val age = LocalDate.now().year - selectedDate.year
                        val weeksLived = age * 52
                        val estimatedWeeksTotal = 90 * 52
                        val percentageLived = (weeksLived.toFloat() / estimatedWeeksTotal * 100).toInt()
                        
                        Text(
                            text = stringResource(R.string.tutorial_birthdate_progress_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Divider(
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$age",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.tutorial_birthdate_years_label),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "~$weeksLived",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.tutorial_birthdate_weeks_lived_label),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        
                        LinearProgressIndicator(
                            progress = percentageLived / 100f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        
                        Text(
                            text = stringResource(
                                R.string.tutorial_birthdate_life_percent,
                                percentageLived
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
    
    if (showDatePicker) {
        com.momentummm.app.ui.component.SimpleDatePicker(
            selectedDate = selectedDate,
            onDateSelected = { onDateSelected(it); showDatePicker = false },
            onDismiss = { showDatePicker = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
private suspend fun animateToPage(pagerState: PagerState, page: Int) {
    pagerState.animateScrollToPage(page)
}
