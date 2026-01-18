package com.momentummm.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.momentummm.app.MomentumApplication
import com.momentummm.app.R
import com.momentummm.app.ui.password.PasswordProtectionViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

// ============================================================================
// COMPONENTES DE PREFERENCIA REUTILIZABLES
// ============================================================================

/**
 * Categoría de preferencias con título y contenido expandible opcional
 */
@Composable
fun PreferenceCategory(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        }
    }
}

/**
 * Subcategoría expandible dentro de una categoría principal
 */
@Composable
fun PreferenceSubCategory(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    
    Column(modifier = modifier.fillMaxWidth()) {
        // Header de la subcategoría
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Colapsar" else "Expandir",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Contenido expandible
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp),
                content = content
            )
        }
    }
}

/**
 * Item de preferencia clickeable
 */
@Composable
fun PreferenceItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showDivider: Boolean = true,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant 
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface 
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant 
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
            if (trailingContent != null) {
                trailingContent()
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
        if (showDivider) {
            Divider(
                modifier = Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * Item de preferencia con Switch
 */
@Composable
fun PreferenceSwitchItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showDivider: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onCheckedChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) MaterialTheme.colorScheme.primary 
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
        if (showDivider) {
            Divider(
                modifier = Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}

// ============================================================================
// PANTALLA PRINCIPAL DE AJUSTES
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToScreen: (String) -> Unit = {},
    passwordViewModel: PasswordProtectionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val application = context.applicationContext as MomentumApplication
    val coroutineScope = rememberCoroutineScope()
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    // Estado de protección por contraseña
    val passwordProtection by passwordViewModel.passwordProtection.collectAsState()
    val isPasswordSet = passwordProtection?.isEnabled == true && !passwordProtection?.passwordHash.isNullOrEmpty()
    val isProtectionActive = passwordProtection?.isEnabled == true
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // ====================================================================
        // HEADER
        // ====================================================================
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.nav_settings),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // ====================================================================
        // 1. CUENTA
        // ====================================================================
        item {
            PreferenceCategory(title = "Cuenta") {
                PreferenceItem(
                    title = "Configuración de cuenta",
                    subtitle = "Gestionar tu perfil y datos personales",
                    icon = Icons.Default.AccountCircle,
                    onClick = { onNavigateToScreen("account_settings") }
                )
                PreferenceItem(
                    title = "Ayuda",
                    subtitle = "Preguntas frecuentes y soporte",
                    icon = Icons.Default.HelpOutline,
                    onClick = { onNavigateToScreen("help") }
                )
                PreferenceItem(
                    title = "Cerrar sesión",
                    subtitle = "Salir de tu cuenta actual",
                    icon = Icons.Default.Logout,
                    onClick = { showLogoutDialog = true }
                )
                PreferenceItem(
                    title = "Tutorial de la app",
                    subtitle = "Aprende a usar todas las funciones de Momentum",
                    icon = Icons.Default.School,
                    onClick = { onNavigateToScreen("tutorial") },
                    showDivider = false
                )
            }
        }

        // ====================================================================
        // 2. FUNCIONES
        // ====================================================================
        item {
            PreferenceCategory(title = "Funciones") {
                // ------------------------------------------------------------
                // 2.1 Seguridad y Privacidad
                // ------------------------------------------------------------
                PreferenceSubCategory(
                    title = "Seguridad y Privacidad",
                    icon = Icons.Default.Security,
                    initiallyExpanded = true
                ) {
                    PreferenceSwitchItem(
                        title = "Protección activa",
                        subtitle = if (isProtectionActive) "La app está protegida" else "Activa para proteger tu configuración",
                        icon = Icons.Default.Shield,
                        checked = isProtectionActive,
                        onCheckedChange = { enabled ->
                            coroutineScope.launch {
                                passwordViewModel.toggleProtection(enabled)
                            }
                        },
                        enabled = isPasswordSet // Solo se puede activar si hay contraseña configurada
                    )
                    PreferenceItem(
                        title = "Protección por Contraseña",
                        subtitle = if (isPasswordSet) "Contraseña configurada" else "Configura una contraseña numérica",
                        icon = Icons.Default.Lock,
                        onClick = {
                            if (isPasswordSet) {
                                onNavigateToScreen("password_manage")
                            } else {
                                onNavigateToScreen("password_setup")
                            }
                        },
                        showDivider = false,
                        trailingContent = {
                            if (isPasswordSet) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Configurado",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    )
                }
                
                Divider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                // ------------------------------------------------------------
                // 2.2 Bienestar Digital
                // ------------------------------------------------------------
                PreferenceSubCategory(
                    title = "Bienestar Digital",
                    icon = Icons.Default.Psychology,
                    initiallyExpanded = true
                ) {
                    PreferenceItem(
                        title = "Límites de aplicaciones",
                        subtitle = "Configura tiempo máximo de uso por app",
                        icon = Icons.Default.Timer,
                        onClick = { onNavigateToScreen("app_limits") }
                    )
                    PreferenceItem(
                        title = "Bloqueo dentro de Apps",
                        subtitle = "Bloquea Reels, Shorts y contenido específico",
                        icon = Icons.Default.VideoLibrary,
                        onClick = { onNavigateToScreen("in_app_blocking") }
                    )
                    PreferenceItem(
                        title = "Bloqueo de sitios web",
                        subtitle = "Bloquear sitios web distractores",
                        icon = Icons.Default.Web,
                        onClick = { onNavigateToScreen("website_blocks") }
                    )
                    PreferenceItem(
                        title = "Gestionar aplicaciones",
                        subtitle = "Apps permitidas en Modo Mínimo",
                        icon = Icons.Default.PhoneAndroid,
                        onClick = { onNavigateToScreen("app_whitelist") },
                        showDivider = false
                    )
                }

                Divider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                // ------------------------------------------------------------
                // 2.3 Personalización y Datos
                // ------------------------------------------------------------
                PreferenceSubCategory(
                    title = "Personalización y Datos",
                    icon = Icons.Default.Palette,
                    initiallyExpanded = true
                ) {
                    PreferenceItem(
                        title = "Notificaciones",
                        subtitle = "Configurar alertas y recordatorios",
                        icon = Icons.Default.Notifications,
                        onClick = { onNavigateToScreen("notification_settings") }
                    )
                    PreferenceItem(
                        title = "Mi vida en semanas",
                        subtitle = "Visualiza tu vida y reflexiona sobre el tiempo",
                        icon = Icons.Default.CalendarMonth,
                        onClick = { onNavigateToScreen("mi_vida_en_semanas") }
                    )
                    PreferenceItem(
                        title = "Personalización",
                        subtitle = "Temas y apariencia de la aplicación",
                        icon = Icons.Default.ColorLens,
                        onClick = { onNavigateToScreen("theme_settings") }
                    )
                    PreferenceItem(
                        title = "Configuración de widgets",
                        subtitle = "Personalizar widgets de pantalla de inicio",
                        icon = Icons.Default.Widgets,
                        onClick = { onNavigateToScreen("widget_setup") }
                    )
                    PreferenceItem(
                        title = "Sincronización",
                        subtitle = "Ver estado y sincronizar datos con la nube",
                        icon = Icons.Default.Sync,
                        onClick = { onNavigateToScreen("sync_settings") },
                        showDivider = false
                    )
                }
            }
        }

        // ====================================================================
        // 3. INFORMACIÓN
        // ====================================================================
        item {
            PreferenceCategory(title = "Información") {
                PreferenceItem(
                    title = "Acerca de",
                    subtitle = "Información sobre Momentum",
                    icon = Icons.Default.Info,
                    onClick = { onNavigateToScreen("about") }
                )
                PreferenceItem(
                    title = "Permisos",
                    subtitle = "Gestionar permisos de la aplicación",
                    icon = Icons.Default.Security,
                    onClick = { onNavigateToScreen("permissions_settings") },
                    showDivider = false
                )
            }
        }

        // Espacio al final
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // ========================================================================
    // DIÁLOGO DE CONFIRMACIÓN DE CIERRE DE SESIÓN
    // ========================================================================
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { 
                Text(
                    text = "Cerrar sesión",
                    fontWeight = FontWeight.SemiBold
                ) 
            },
            text = { 
                Text("¿Estás seguro de que quieres cerrar sesión? Tus datos locales se mantendrán sincronizados.") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            application.appwriteService.logout()
                            showLogoutDialog = false
                            // Navegar a la pantalla de login
                            onNavigateToScreen("logout")
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Cerrar sesión")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}