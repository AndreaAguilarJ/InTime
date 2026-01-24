package com.momentummm.app.ui.screen.settings

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import com.momentummm.app.MomentumApplication
import com.momentummm.app.data.UserPreferencesRepository
import com.momentummm.app.util.LifeWeeksCalculator
import com.momentummm.app.widget.LifeWeeksWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeWeeksSettingsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as MomentumApplication
    val coroutineScope = rememberCoroutineScope()
    
    // Estados
    var birthDate by remember { mutableStateOf<Date?>(null) }
    var birthDateText by remember { mutableStateOf("No configurada") }
    var livedWeeksColor by remember { mutableStateOf("#6366F1") }
    var futureWeeksColor by remember { mutableStateOf("#E5E7EB") }
    var backgroundColor by remember { mutableStateOf("#1F2937") }
    var lifeWeeksData by remember { mutableStateOf<LifeWeeksCalculator.LifeWeeksData?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showLivedColorPicker by remember { mutableStateOf(false) }
    var showFutureColorPicker by remember { mutableStateOf(false) }
    var showBackgroundColorPicker by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var lifeExpectancy by remember { mutableStateOf(80) }
    
    // Cargar datos iniciales
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val settings = application.userRepository.getUserSettingsSync()
            settings?.let {
                birthDate = it.birthDate
                livedWeeksColor = it.livedWeeksColor ?: "#6366F1"
                futureWeeksColor = it.futureWeeksColor ?: "#E5E7EB"
                backgroundColor = it.backgroundColor ?: "#1F2937"
                
                it.birthDate?.let { date ->
                    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    birthDateText = formatter.format(date)
                    lifeWeeksData = LifeWeeksCalculator.calculateLifeWeeks(date, lifeExpectancy)
                }
            }
        }
    }
    
    // DatePicker Dialog
    if (showDatePicker) {
        val calendar = Calendar.getInstance()
        birthDate?.let { calendar.time = it }
        
        DatePickerDialog(
            context,
            { _, year, month, day ->
                calendar.set(year, month, day)
                birthDate = calendar.time
                val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                birthDateText = formatter.format(calendar.time)
                lifeWeeksData = LifeWeeksCalculator.calculateLifeWeeks(calendar.time, lifeExpectancy)
                showDatePicker = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
            datePicker.minDate = Calendar.getInstance().apply { 
                set(1900, 0, 1) 
            }.timeInMillis
            setOnCancelListener { showDatePicker = false }
            show()
        }
        showDatePicker = false
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Mi Vida en Semanas",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    // Botón de guardar
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                isSaving = true
                                try {
                                    // Guardar en repositorio local
                                    birthDate?.let { date ->
                                        application.userRepository.setBirthDate(date)
                                    }
                                    application.userRepository.updateColors(
                                        livedWeeksColor,
                                        futureWeeksColor,
                                        backgroundColor
                                    )
                                    
                                    // Guardar en Appwrite si hay usuario logueado
                                    val userId = application.appwriteService.currentUser.value?.id
                                    if (userId != null) {
                                        val iso = birthDate?.let {
                                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)
                                        }
                                        // Crear nuevo settings con los colores actualizados
                                        val updatedSettings = com.momentummm.app.data.appwrite.models.AppwriteUserSettings(
                                            userId = userId,
                                            birthDate = iso ?: "",
                                            livedWeeksColor = livedWeeksColor,
                                            futureWeeksColor = futureWeeksColor,
                                            isOnboardingCompleted = true
                                        )
                                        application.appwriteUserRepository.updateUserSettings(userId, updatedSettings)
                                    }
                                    
                                    // Actualizar preferencias
                                    birthDate?.let { date ->
                                        val iso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
                                        UserPreferencesRepository.setDobIso(context, iso)
                                    }
                                    UserPreferencesRepository.setWidgetColors(context, livedWeeksColor, futureWeeksColor)
                                    
                                    // Actualizar widgets
                                    LifeWeeksWidget().updateAll(context)
                                    
                                    Toast.makeText(context, "✅ Configuración guardada", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isSaving = false
                                }
                            }
                        },
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Save, "Guardar")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // === ESTADÍSTICAS ACTUALES ===
            item {
                lifeWeeksData?.let { data ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "📊 Tu Vida en Números",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatItem(
                                    value = "${data.weeksLived}",
                                    label = "Semanas Vividas",
                                    color = Color(android.graphics.Color.parseColor(livedWeeksColor))
                                )
                                StatItem(
                                    value = "${data.weeksRemaining}",
                                    label = "Semanas Restantes",
                                    color = Color(android.graphics.Color.parseColor(futureWeeksColor))
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            LinearProgressIndicator(
                                progress = data.progressPercentage / 100f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Color(android.graphics.Color.parseColor(livedWeeksColor)),
                                trackColor = Color(android.graphics.Color.parseColor(futureWeeksColor))
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "${String.format("%.1f", data.progressPercentage)}% de tu vida",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Edad: ${data.currentAge} años",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            // === FECHA DE NACIMIENTO ===
            item {
                SettingSectionCard(
                    title = "📅 Fecha de Nacimiento",
                    icon = Icons.Default.Cake
                ) {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = birthDateText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                if (birthDate != null) {
                                    Text(
                                        text = "Toca para cambiar",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            
            // === EXPECTATIVA DE VIDA ===
            item {
                SettingSectionCard(
                    title = "⏳ Expectativa de Vida",
                    icon = Icons.Default.Timeline
                ) {
                    Column {
                        Text(
                            text = "$lifeExpectancy años",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Slider(
                            value = lifeExpectancy.toFloat(),
                            onValueChange = { 
                                lifeExpectancy = it.toInt()
                                birthDate?.let { date ->
                                    lifeWeeksData = LifeWeeksCalculator.calculateLifeWeeks(date, lifeExpectancy)
                                }
                            },
                            valueRange = 60f..120f,
                            steps = 59,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("60", style = MaterialTheme.typography.bodySmall)
                            Text("90", style = MaterialTheme.typography.bodySmall)
                            Text("120", style = MaterialTheme.typography.bodySmall)
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Ajusta según tu salud y expectativas personales",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            // === COLORES ===
            item {
                SettingSectionCard(
                    title = "🎨 Personalización de Colores",
                    icon = Icons.Default.Palette
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Color de semanas vividas
                        ColorPickerItem(
                            label = "Semanas Vividas",
                            currentColor = livedWeeksColor,
                            onColorSelected = { livedWeeksColor = it }
                        )
                        
                        Divider()
                        
                        // Color de semanas futuras
                        ColorPickerItem(
                            label = "Semanas Futuras",
                            currentColor = futureWeeksColor,
                            onColorSelected = { futureWeeksColor = it }
                        )
                        
                        Divider()
                        
                        // Color de fondo (para wallpaper)
                        ColorPickerItem(
                            label = "Fondo del Wallpaper",
                            currentColor = backgroundColor,
                            onColorSelected = { backgroundColor = it }
                        )
                    }
                }
            }
            
            // === PREVISUALIZACIÓN ===
            item {
                SettingSectionCard(
                    title = "👁️ Vista Previa",
                    icon = Icons.Default.Preview
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.5f),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(android.graphics.Color.parseColor(backgroundColor))
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            MiniLifeWeeksPreview(
                                weeksLived = lifeWeeksData?.weeksLived ?: 1560,
                                livedColor = Color(android.graphics.Color.parseColor(livedWeeksColor)),
                                futureColor = Color(android.graphics.Color.parseColor(futureWeeksColor))
                            )
                        }
                    }
                }
            }
            
            // === ACCIONES ===
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    birthDate?.let { date ->
                                        val bitmap = com.momentummm.app.util.WallpaperGenerator.generateLifeWeeksWallpaper(
                                            context = context,
                                            weeksLived = lifeWeeksData?.weeksLived ?: 0,
                                            livedColor = android.graphics.Color.parseColor(livedWeeksColor),
                                            futureColor = android.graphics.Color.parseColor(futureWeeksColor),
                                            backgroundColor = android.graphics.Color.parseColor(backgroundColor)
                                        )
                                        com.momentummm.app.util.WallpaperGenerator.saveToGallery(context, bitmap)
                                        Toast.makeText(context, "✅ Guardado en galería", Toast.LENGTH_SHORT).show()
                                    } ?: run {
                                        Toast.makeText(context, "Configura tu fecha de nacimiento primero", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardar como Imagen")
                    }
                    
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    birthDate?.let { date ->
                                        val bitmap = com.momentummm.app.util.WallpaperGenerator.generateLifeWeeksWallpaper(
                                            context = context,
                                            weeksLived = lifeWeeksData?.weeksLived ?: 0,
                                            livedColor = android.graphics.Color.parseColor(livedWeeksColor),
                                            futureColor = android.graphics.Color.parseColor(futureWeeksColor),
                                            backgroundColor = android.graphics.Color.parseColor(backgroundColor)
                                        )
                                        com.momentummm.app.util.WallpaperGenerator.setAsWallpaper(context, bitmap)
                                        Toast.makeText(context, "✅ Fondo de pantalla establecido", Toast.LENGTH_SHORT).show()
                                    } ?: run {
                                        Toast.makeText(context, "Configura tu fecha de nacimiento primero", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Wallpaper, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Establecer como Fondo")
                    }
                }
            }
            
            // Espacio al final
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun SettingSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}

@Composable
private fun ColorPickerItem(
    label: String,
    currentColor: String,
    onColorSelected: (String) -> Unit
) {
    val presetColors = listOf(
        "#6366F1" to "Índigo",
        "#8B5CF6" to "Violeta",
        "#EC4899" to "Rosa",
        "#EF4444" to "Rojo",
        "#F97316" to "Naranja",
        "#EAB308" to "Amarillo",
        "#22C55E" to "Verde",
        "#14B8A6" to "Turquesa",
        "#3B82F6" to "Azul",
        "#1F2937" to "Gris oscuro",
        "#374151" to "Gris",
        "#E5E7EB" to "Gris claro"
    )
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(currentColor)))
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(presetColors) { (color, name) ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(color)))
                        .border(
                            width = if (color == currentColor) 3.dp else 1.dp,
                            color = if (color == currentColor) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(color) }
                ) {
                    if (color == currentColor) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = if (color == "#E5E7EB" || color == "#EAB308") 
                                Color.Black 
                            else 
                                Color.White,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniLifeWeeksPreview(
    weeksLived: Int,
    livedColor: Color,
    futureColor: Color
) {
    val weeksPerRow = 52
    val rows = 10 // Mostrar solo 10 años de preview
    
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(rows) { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                repeat(weeksPerRow) { col ->
                    val weekIndex = row * weeksPerRow + col
                    val color = if (weekIndex < weeksLived % (rows * weeksPerRow)) livedColor else futureColor
                    
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(color, RoundedCornerShape(1.dp))
                    )
                }
            }
        }
    }
}
