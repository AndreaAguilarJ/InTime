package com.momentummm.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momentummm.app.ui.theme.MomentumTheme
import kotlinx.coroutines.delay

class InAppBlockedActivity : ComponentActivity() {

    private var appName: String = ""
    private var featureName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appName = intent.getStringExtra("app_name") ?: "Aplicación"
        featureName = intent.getStringExtra("feature_name") ?: "Función"

        setContent {
            MomentumTheme {
                InAppBlockedScreen(
                    appName = appName,
                    featureName = featureName,
                    onClose = { finish() }
                )
            }
        }
    }

    override fun onBackPressed() {
        // Prevenir que el usuario cierre con el botón de atrás
        // Solo permitir cerrar con el botón "Entendido"
    }
}

@Composable
fun InAppBlockedScreen(
    appName: String,
    featureName: String,
    onClose: () -> Unit
) {
    var timeRemaining by remember { mutableStateOf(3) }

    LaunchedEffect(Unit) {
        while (timeRemaining > 0) {
            delay(1000)
            timeRemaining--
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Ícono de bloqueo
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = "Bloqueado",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Título
                Text(
                    text = "Función Bloqueada",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Mensaje
                Text(
                    text = "Has intentado acceder a:",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Función bloqueada destacada
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = featureName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "en $appName",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Mensaje motivacional
                Text(
                    text = "Esta función está bloqueada para ayudarte a mantener el enfoque y reducir el tiempo de distracción.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Botón para cerrar
                Button(
                    onClick = {
                        if (timeRemaining == 0) {
                            onClose()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = timeRemaining == 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = if (timeRemaining > 0) {
                            "Espera ${timeRemaining}s..."
                        } else {
                            "Entendido"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Texto informativo
                Text(
                    text = "💡 Puedes desactivar este bloqueo desde la configuración de InTime",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

