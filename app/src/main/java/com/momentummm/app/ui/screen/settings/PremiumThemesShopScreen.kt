package com.momentummm.app.ui.screen.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momentummm.app.data.manager.GamificationManager
import com.momentummm.app.data.manager.GamificationState
import com.momentummm.app.data.manager.ThemeManager
import com.momentummm.app.ui.component.TimeCoinBalance
import kotlinx.coroutines.launch

/**
 * Tema premium que se puede comprar con TimeCoins
 */
data class PremiumTheme(
    val id: String,
    val name: String,
    val description: String,
    val primaryColor: String,
    val secondaryColor: String,
    val accentColor: String,
    val price: Int,
    val isLocked: Boolean,
    val requiredLevel: Int = 1,
    val emoji: String = "🎨"
)

/**
 * Lista de temas premium disponibles
 */
val premiumThemes = listOf(
    PremiumTheme(
        id = "neon_purple",
        name = "Neon Purple",
        description = "Vibrante y moderno",
        primaryColor = "#8B5CF6",
        secondaryColor = "#A855F7",
        accentColor = "#C4B5FD",
        price = 0, // Gratis
        isLocked = false,
        emoji = "💜"
    ),
    PremiumTheme(
        id = "ocean_blue",
        name = "Ocean Blue",
        description = "Calma y productividad",
        primaryColor = "#0EA5E9",
        secondaryColor = "#38BDF8",
        accentColor = "#7DD3FC",
        price = 100,
        isLocked = true,
        requiredLevel = 2,
        emoji = "🌊"
    ),
    PremiumTheme(
        id = "forest_green",
        name = "Forest Green",
        description = "Natural y relajante",
        primaryColor = "#10B981",
        secondaryColor = "#34D399",
        accentColor = "#6EE7B7",
        price = 150,
        isLocked = true,
        requiredLevel = 3,
        emoji = "🌲"
    ),
    PremiumTheme(
        id = "sunset_orange",
        name = "Sunset Orange",
        description = "Energético y cálido",
        primaryColor = "#F97316",
        secondaryColor = "#FB923C",
        accentColor = "#FDBA74",
        price = 200,
        isLocked = true,
        requiredLevel = 4,
        emoji = "🌅"
    ),
    PremiumTheme(
        id = "cherry_blossom",
        name = "Cherry Blossom",
        description = "Elegante y sutil",
        primaryColor = "#EC4899",
        secondaryColor = "#F472B6",
        accentColor = "#F9A8D4",
        price = 250,
        isLocked = true,
        requiredLevel = 5,
        emoji = "🌸"
    ),
    PremiumTheme(
        id = "midnight_gold",
        name = "Midnight Gold",
        description = "Lujoso y exclusivo",
        primaryColor = "#FBBF24",
        secondaryColor = "#F59E0B",
        accentColor = "#FCD34D",
        price = 500,
        isLocked = true,
        requiredLevel = 7,
        emoji = "✨"
    ),
    PremiumTheme(
        id = "cyber_red",
        name = "Cyber Red",
        description = "Intenso y poderoso",
        primaryColor = "#EF4444",
        secondaryColor = "#F87171",
        accentColor = "#FCA5A5",
        price = 350,
        isLocked = true,
        requiredLevel = 6,
        emoji = "🔴"
    ),
    PremiumTheme(
        id = "arctic_ice",
        name = "Arctic Ice",
        description = "Fresco y limpio",
        primaryColor = "#06B6D4",
        secondaryColor = "#22D3EE",
        accentColor = "#67E8F9",
        price = 300,
        isLocked = true,
        requiredLevel = 5,
        emoji = "❄️"
    ),
    PremiumTheme(
        id = "galaxy_dark",
        name = "Galaxy Dark",
        description = "Misterioso y profundo",
        primaryColor = "#4C1D95",
        secondaryColor = "#6D28D9",
        accentColor = "#8B5CF6",
        price = 750,
        isLocked = true,
        requiredLevel = 8,
        emoji = "🌌"
    ),
    PremiumTheme(
        id = "legendary_rainbow",
        name = "Legendary Rainbow",
        description = "El tema más exclusivo",
        primaryColor = "#FF6B6B",
        secondaryColor = "#4ECDC4",
        accentColor = "#FFE66D",
        price = 1000,
        isLocked = true,
        requiredLevel = 10,
        emoji = "🌈"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumThemesShopScreen(
    gamificationState: GamificationState?,
    gamificationManager: GamificationManager,
    themeManager: ThemeManager = ThemeManager(LocalContext.current),
    onBackClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var unlockedThemes by remember { mutableStateOf(setOf("neon_purple")) }
    var selectedTheme by remember { mutableStateOf<PremiumTheme?>(null) }
    var showPurchaseDialog by remember { mutableStateOf(false) }
    var showInsufficientCoinsDialog by remember { mutableStateOf(false) }
    var showLevelRequiredDialog by remember { mutableStateOf(false) }
    var purchaseSuccess by remember { mutableStateOf(false) }
    
    val currentCoins = gamificationState?.timeCoins ?: 0
    val currentLevel = gamificationState?.level ?: 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TopBar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver"
                    )
                }
                
                Text(
                    text = "Tienda de Temas",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                )

                // Badge de nivel
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = gamificationState?.levelEmoji ?: "🌟",
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Nv.${currentLevel}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Balance de TimeCoins
            item {
                TimeCoinBalance(coins = currentCoins)
            }

            // Título de sección
            item {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Temas Premium",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Desbloquea temas exclusivos con tus TimeCoins",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // Grid de temas
            items(premiumThemes.chunked(2)) { rowThemes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowThemes.forEach { theme ->
                        val isUnlocked = unlockedThemes.contains(theme.id) || !theme.isLocked
                        val meetsLevelRequirement = currentLevel >= theme.requiredLevel
                        val canAfford = currentCoins >= theme.price
                        
                        ThemeCard(
                            theme = theme,
                            isUnlocked = isUnlocked,
                            meetsLevelRequirement = meetsLevelRequirement,
                            canAfford = canAfford,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedTheme = theme
                                when {
                                    isUnlocked -> {
                                        // Aplicar tema
                                        scope.launch {
                                            themeManager.setCustomPrimaryColor(theme.primaryColor)
                                        }
                                    }
                                    !meetsLevelRequirement -> {
                                        showLevelRequiredDialog = true
                                    }
                                    !canAfford -> {
                                        showInsufficientCoinsDialog = true
                                    }
                                    else -> {
                                        showPurchaseDialog = true
                                    }
                                }
                            }
                        )
                    }
                    // Espaciador si hay número impar de temas
                    if (rowThemes.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Información de cómo ganar TimeCoins
            item {
                HowToEarnCoinsCard()
            }
        }
    }

    // Diálogo de compra
    if (showPurchaseDialog && selectedTheme != null) {
        AlertDialog(
            onDismissRequest = { showPurchaseDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(selectedTheme!!.emoji, fontSize = 28.sp)
                    Text("Desbloquear ${selectedTheme!!.name}")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("¿Deseas comprar este tema por ${selectedTheme!!.price} TimeCoins?")
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tu balance:")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🪙", fontSize = 16.sp)
                            Text(
                                text = "$currentCoins",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD700)
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Después de compra:")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🪙", fontSize = 16.sp)
                            Text(
                                text = "${currentCoins - selectedTheme!!.price}",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val success = gamificationManager.spendCoins(selectedTheme!!.price)
                            if (success) {
                                unlockedThemes = unlockedThemes + selectedTheme!!.id
                                themeManager.setCustomPrimaryColor(selectedTheme!!.primaryColor)
                                purchaseSuccess = true
                            }
                        }
                        showPurchaseDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    )
                ) {
                    Text("🪙 Comprar por ${selectedTheme!!.price}")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurchaseDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de monedas insuficientes
    if (showInsufficientCoinsDialog && selectedTheme != null) {
        AlertDialog(
            onDismissRequest = { showInsufficientCoinsDialog = false },
            title = { Text("TimeCoins insuficientes") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Necesitas ${selectedTheme!!.price} TimeCoins para desbloquear ${selectedTheme!!.name}.")
                    Text("Tu balance actual: $currentCoins TimeCoins")
                    Text(
                        "Te faltan: ${selectedTheme!!.price - currentCoins} TimeCoins",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "💡 Tip: Completa sesiones de foco para ganar más TimeCoins",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showInsufficientCoinsDialog = false }) {
                    Text("Entendido")
                }
            }
        )
    }

    // Diálogo de nivel requerido
    if (showLevelRequiredDialog && selectedTheme != null) {
        AlertDialog(
            onDismissRequest = { showLevelRequiredDialog = false },
            title = { Text("Nivel insuficiente") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Necesitas alcanzar el nivel ${selectedTheme!!.requiredLevel} para desbloquear ${selectedTheme!!.name}.")
                    Text("Tu nivel actual: $currentLevel")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "💡 Tip: Gana XP completando sesiones de foco y mantén tu racha para subir de nivel",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showLevelRequiredDialog = false }) {
                    Text("Entendido")
                }
            }
        )
    }

    // Snackbar de compra exitosa
    AnimatedVisibility(
        visible = purchaseSuccess,
        enter = fadeIn() + slideInVertically(initialOffsetY = { 100 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { 100 })
    ) {
        LaunchedEffect(purchaseSuccess) {
            if (purchaseSuccess) {
                kotlinx.coroutines.delay(2500)
                purchaseSuccess = false
            }
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF10B981)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("✅", fontSize = 24.sp)
                    Text(
                        text = "¡Tema desbloqueado y aplicado!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeCard(
    theme: PremiumTheme,
    isUnlocked: Boolean,
    meetsLevelRequirement: Boolean,
    canAfford: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val primaryColor = Color(android.graphics.Color.parseColor(theme.primaryColor))
    val secondaryColor = Color(android.graphics.Color.parseColor(theme.secondaryColor))
    
    Card(
        modifier = modifier
            .aspectRatio(0.85f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) 
                MaterialTheme.colorScheme.surfaceVariant 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Preview de colores
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(primaryColor, secondaryColor)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = theme.emoji,
                        fontSize = 28.sp
                    )
                }

                Column {
                    Text(
                        text = theme.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = theme.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }

                // Precio o estado
                if (isUnlocked) {
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Desbloqueado",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Precio
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("🪙", fontSize = 14.sp)
                            Text(
                                text = "${theme.price}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (canAfford) Color(0xFFFFD700) else Color(0xFFEF4444)
                            )
                        }
                        
                        // Nivel requerido
                        if (!meetsLevelRequirement) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Nv.${theme.requiredLevel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Overlay de bloqueado
            if (!isUnlocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Bloqueado",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HowToEarnCoinsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("💡", fontSize = 24.sp)
                Text(
                    text = "Cómo ganar TimeCoins",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EarnMethodRow("⏱️", "Minuto de foco", "+1 🪙")
                EarnMethodRow("✅", "Sesión completada", "+25 🪙")
                EarnMethodRow("⭐", "Día perfecto", "+50 🪙")
                EarnMethodRow("🔥", "Bonus de racha", "x1.5 - x2.0")
                EarnMethodRow("🎉", "Subir de nivel", "+50-500 🪙")
            }
        }
    }
}

@Composable
private fun EarnMethodRow(emoji: String, description: String, reward: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(emoji, fontSize = 18.sp)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = reward,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFD700)
        )
    }
}
