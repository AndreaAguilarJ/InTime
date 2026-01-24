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
                contentDescription = if (isExpanded) stringResource(R.string.settings_collapse)
                else stringResource(R.string.settings_expand),
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
            PreferenceCategory(title = stringResource(R.string.settings_category_account)) {
                PreferenceItem(
                    title = stringResource(R.string.account_settings),
                    subtitle = stringResource(R.string.settings_account_subtitle),
                    icon = Icons.Default.AccountCircle,
                    onClick = { onNavigateToScreen("account_settings") }
                )
                PreferenceItem(
                    title = stringResource(R.string.settings_help_title),
                    subtitle = stringResource(R.string.settings_help_subtitle),
                    icon = Icons.Default.HelpOutline,
                    onClick = { onNavigateToScreen("help") }
                )
                PreferenceItem(
                    title = stringResource(R.string.logout),
                    subtitle = stringResource(R.string.settings_logout_subtitle),
                    icon = Icons.Default.Logout,
                    onClick = { showLogoutDialog = true }
                )
                PreferenceItem(
                    title = stringResource(R.string.tutorial_title),
                    subtitle = stringResource(R.string.settings_tutorial_subtitle),
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
            PreferenceCategory(title = stringResource(R.string.settings_category_features)) {
                // ------------------------------------------------------------
                // 2.1 Seguridad y Privacidad
                // ------------------------------------------------------------
                PreferenceSubCategory(
                    title = stringResource(R.string.settings_security_privacy_title),
                    icon = Icons.Default.Security,
                    initiallyExpanded = true
                ) {
                    PreferenceSwitchItem(
                        title = stringResource(R.string.settings_protection_active_title),
                        subtitle = if (isProtectionActive) {
                            stringResource(R.string.settings_protection_active_on_subtitle)
                        } else {
                            stringResource(R.string.settings_protection_active_off_subtitle)
                        },
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
                        title = stringResource(R.string.settings_password_protection_title),
                        subtitle = if (isPasswordSet) {
                            stringResource(R.string.settings_password_configured)
                        } else {
                            stringResource(R.string.settings_password_configure)
                        },
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
                                    contentDescription = stringResource(R.string.settings_configured),
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
                    title = stringResource(R.string.settings_digital_wellbeing_title),
                    icon = Icons.Default.Psychology,
                    initiallyExpanded = true
                ) {
                    PreferenceItem(
                        title = stringResource(R.string.app_limits),
                        subtitle = stringResource(R.string.settings_app_limits_subtitle),
                        icon = Icons.Default.Timer,
                        onClick = { onNavigateToScreen("app_limits") }
                    )
                    PreferenceItem(
                        title = stringResource(R.string.settings_in_app_blocking_title),
                        subtitle = stringResource(R.string.settings_in_app_blocking_subtitle),
                        icon = Icons.Default.VideoLibrary,
                        onClick = { onNavigateToScreen("in_app_blocking") }
                    )
                    PreferenceItem(
                        title = stringResource(R.string.settings_website_blocks_title),
                        subtitle = stringResource(R.string.settings_website_blocks_subtitle),
                        icon = Icons.Default.Web,
                        onClick = { onNavigateToScreen("website_blocks") }
                    )
                    PreferenceItem(
                        title = stringResource(R.string.manage_apps),
                        subtitle = stringResource(R.string.settings_app_whitelist_subtitle),
                        icon = Icons.Default.PhoneAndroid,
                        onClick = { onNavigateToScreen("app_whitelist") }
                    )
                    PreferenceItem(
                        title = "Bloqueo Inteligente",
                        subtitle = "Timer flotante, modo nuclear, sueño, ayuno digital",
                        icon = Icons.Default.AutoAwesome,
                        onClick = { onNavigateToScreen("smart_blocking") },
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
                    title = stringResource(R.string.settings_personalization_data_title),
                    icon = Icons.Default.Palette,
                    initiallyExpanded = true
                ) {
                    PreferenceItem(
                        title = stringResource(R.string.settings_language_title),
                        subtitle = stringResource(R.string.settings_language_subtitle),
                        icon = Icons.Default.Translate,
                        onClick = { onNavigateToScreen("language_settings") }
                    )
                    PreferenceItem(
                        title = stringResource(R.string.settings_gamification_title),
                        subtitle = stringResource(R.string.settings_gamification_subtitle),
                        icon = Icons.Default.EmojiEvents,
                        onClick = { onNavigateToScreen("gamification_settings") }
                    )
                    PreferenceItem(
                        title = stringResource(R.string.notifications),
                        subtitle = stringResource(R.string.settings_notifications_subtitle),
                        icon = Icons.Default.Notifications,
                        onClick = { onNavigateToScreen("notification_settings") }
                    )
                    PreferenceItem(
                        title = stringResource(R.string.my_life_in_weeks),
                        subtitle = stringResource(R.string.settings_life_in_weeks_subtitle),
                        icon = Icons.Default.CalendarMonth,
                        onClick = { onNavigateToScreen("mi_vida_en_semanas") }
                    )
                    PreferenceItem(
                        title = stringResource(R.string.settings_theme_title),
                        subtitle = stringResource(R.string.settings_theme_subtitle),
                        icon = Icons.Default.ColorLens,
                        onClick = { onNavigateToScreen("theme_settings") }
                    )
                    PreferenceItem(
                        title = stringResource(R.string.widget_settings),
                        subtitle = stringResource(R.string.settings_widgets_subtitle),
                        icon = Icons.Default.Widgets,
                        onClick = { onNavigateToScreen("widget_setup") }
                    )
                    PreferenceItem(
                        title = stringResource(R.string.settings_sync_title),
                        subtitle = stringResource(R.string.settings_sync_subtitle),
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
            PreferenceCategory(title = stringResource(R.string.settings_category_information)) {
                PreferenceItem(
                    title = stringResource(R.string.about),
                    subtitle = stringResource(R.string.settings_about_subtitle),
                    icon = Icons.Default.Info,
                    onClick = { onNavigateToScreen("about") }
                )
                PreferenceItem(
                    title = stringResource(R.string.settings_permissions_title),
                    subtitle = stringResource(R.string.settings_permissions_subtitle),
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
                    text = stringResource(R.string.logout),
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(stringResource(R.string.dialog_logout_message))
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
                    Text(stringResource(R.string.logout))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}