package com.momentummm.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momentummm.app.ui.theme.MomentumTheme
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource
import com.momentummm.app.R

class InAppBlockedActivity : AppCompatActivity() {

    private var appName: String = ""
    private var featureName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appName = intent.getStringExtra("app_name") ?: "Aplicación"
        featureName = intent.getStringExtra("feature_name") ?: "Función"

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // CRITICAL FIX: Agregar finish() para remover del back stack
                launchHome()
                finish()
            }
        })

        setContent {
            MomentumTheme {
                InAppBlockedScreen(
                    appName = appName,
                    featureName = featureName,
                    onClose = {
                        launchHome()
                        finish()
                    }
                )
            }
        }
    }

    private fun launchHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
    }
}

@Composable
fun InAppBlockedScreen(
    appName: String,
    featureName: String,
    onClose: () -> Unit
) {
    val initialCountdownSeconds = 4
    var timeRemaining by remember { mutableStateOf(initialCountdownSeconds) }

    LaunchedEffect(Unit) {
        while (timeRemaining > 0) {
            delay(1000)
            timeRemaining--
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.error
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.error)
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
                    contentDescription = stringResource(R.string.a11y_blocked),
                    modifier = Modifier.size(96.dp),
                    tint = MaterialTheme.colorScheme.onError
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Título
                Text(
                    text = "Función bloqueada",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onError,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Mensaje
                Text(
                    text = "Estás intentando ver",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onError.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Función bloqueada destacada
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.onError
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
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "en $appName",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Mensaje motivacional
                Text(
                    text = "Esta función está bloqueada para ayudarte a mantener el enfoque y reducir el tiempo de distracción.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onError.copy(alpha = 0.9f),
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
                        containerColor = MaterialTheme.colorScheme.onError,
                        disabledContainerColor = MaterialTheme.colorScheme.onError.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = if (timeRemaining > 0) {
                            "Espera ${timeRemaining}s..."
                        } else {
                            "Entendido"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Texto informativo
                Text(
                    text = "💡 Puedes desactivar este bloqueo desde la configuración de Momentum",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onError.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

