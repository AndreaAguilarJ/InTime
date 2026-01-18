package com.momentummm.app.ui.screen.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class FAQItem(
    val question: String,
    val answer: String,
    val icon: ImageVector = Icons.Default.HelpOutline
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    
    val faqItems = remember {
        listOf(
            FAQItem(
                question = "¿Cómo configuro límites de tiempo para aplicaciones?",
                answer = "Ve a Ajustes > Funciones > Bienestar Digital > Límites de aplicaciones. Allí puedes seleccionar las apps que deseas limitar y establecer un tiempo máximo de uso diario.",
                icon = Icons.Default.Timer
            ),
            FAQItem(
                question = "¿Cómo funciona el Modo Mínimo?",
                answer = "El Modo Mínimo convierte tu teléfono en una herramienta básica, ocultando apps distractoras y mostrando solo las esenciales. Puedes configurar qué apps quieres que aparezcan en Ajustes > Gestionar aplicaciones.",
                icon = Icons.Default.PhoneAndroid
            ),
            FAQItem(
                question = "¿Qué es el bloqueo dentro de apps?",
                answer = "Esta función te permite bloquear contenido específico dentro de las apps (como Reels en Instagram o Shorts en YouTube) sin bloquear toda la aplicación. Requiere habilitar el servicio de accesibilidad.",
                icon = Icons.Default.VideoLibrary
            ),
            FAQItem(
                question = "¿Cómo protejo mi configuración con contraseña?",
                answer = "Ve a Ajustes > Funciones > Seguridad y Privacidad > Protección por Contraseña. Configura un PIN numérico para proteger tus ajustes y evitar que alguien desactive los límites.",
                icon = Icons.Default.Lock
            ),
            FAQItem(
                question = "¿Por qué la app necesita el permiso de Accesibilidad?",
                answer = "El permiso de Accesibilidad es necesario para detectar y bloquear contenido específico dentro de apps (como Reels o Shorts) y para bloquear sitios web. Sin este permiso, estas funciones no pueden operar.",
                icon = Icons.Default.Accessibility
            ),
            FAQItem(
                question = "¿Por qué la app necesita el permiso de Uso de aplicaciones?",
                answer = "Este permiso permite a Momentum monitorear tu uso de apps para mostrarte estadísticas precisas y hacer cumplir los límites de tiempo que configures.",
                icon = Icons.Default.BarChart
            ),
            FAQItem(
                question = "¿Cómo funciona 'Mi vida en semanas'?",
                answer = "Esta visualización muestra tu vida dividida en semanas, desde tu nacimiento hasta una expectativa de vida promedio. Te ayuda a reflexionar sobre el tiempo y a tomar decisiones más conscientes sobre cómo lo usas.",
                icon = Icons.Default.CalendarMonth
            ),
            FAQItem(
                question = "¿Cómo configuro los widgets?",
                answer = "Mantén presionada la pantalla de inicio de tu teléfono, selecciona 'Widgets', busca 'Momentum' y arrastra el widget que prefieras a tu pantalla. Puedes configurar los colores en Ajustes > Configuración de widgets.",
                icon = Icons.Default.Widgets
            ),
            FAQItem(
                question = "¿Mis datos se sincronizan entre dispositivos?",
                answer = "Sí, si has iniciado sesión con tu cuenta, tus configuraciones y datos se sincronizan automáticamente en la nube. Puedes ver el estado de sincronización en Ajustes > Sincronización.",
                icon = Icons.Default.Sync
            ),
            FAQItem(
                question = "¿Cómo desactivo los límites temporalmente?",
                answer = "Si tienes protección por contraseña activada, deberás ingresar tu PIN para desactivar los límites. Esto es intencional para evitar que desactives los límites impulsivamente.",
                icon = Icons.Default.LockOpen
            )
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ayuda") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Sección de Preguntas Frecuentes
            item {
                Text(
                    text = "Preguntas Frecuentes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            items(faqItems) { faq ->
                ExpandableFAQCard(faq = faq)
            }
            
            // Sección de Contacto
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "¿Necesitas más ayuda?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:soporte@momentum-app.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Soporte Momentum App")
                        }
                        context.startActivity(Intent.createChooser(intent, "Enviar correo"))
                    }
                ) {
                    ListItem(
                        headlineContent = { Text("Contactar Soporte") },
                        supportingContent = { Text("Envíanos un correo con tus dudas") },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null
                            )
                        }
                    )
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://momentum-app.com/guia"))
                        context.startActivity(intent)
                    }
                ) {
                    ListItem(
                        headlineContent = { Text("Guía Completa Online") },
                        supportingContent = { Text("Tutoriales detallados y consejos") },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null
                            )
                        }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ExpandableFAQCard(faq: FAQItem) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = faq.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = faq.question,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Colapsar" else "Expandir"
                )
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = faq.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
