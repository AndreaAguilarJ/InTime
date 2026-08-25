package com.momentummm.app.ui.screen.websiteblock

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.momentummm.app.R
import com.momentummm.app.ui.theme.MomentumTheme

/**
 * Pantalla que se muestra al intentar abrir un sitio bloqueado.
 *
 * ─── BUG CORREGIDO ────────────────────────────────────────────────────────
 * Esta Activity no interceptaba el botón atrás y su único botón hacía
 * `finish()`. Como se abre encima del navegador, ambas cosas devolvían al
 * usuario a la misma pestaña con el sitio bloqueado cargado: el bloqueo de
 * webs se saltaba pulsando atrás una vez. Las otras dos pantallas de bloqueo
 * del proyecto (AppBlockedActivity e InAppBlockedActivity) sí navegan al
 * inicio; a esta se le olvidó.
 */
class WebsiteBlockedActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Atrás no puede devolver al navegador con el sitio abierto.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goHomeAndFinish()
            }
        })

        val blockedUrl = intent.getStringExtra("blocked_url")
            ?: getString(R.string.website_blocked_unknown_site)

        setContent {
            MomentumTheme {
                WebsiteBlockedScreen(
                    blockedUrl = blockedUrl,
                    onClose = { goHomeAndFinish() }
                )
            }
        }
    }

    /** Sale al inicio en lugar de volver a la pestaña bloqueada. */
    private fun goHomeAndFinish() {
        runCatching {
            startActivity(
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }.onFailure {
            android.util.Log.w("WebsiteBlockedActivity", "No se pudo ir al inicio", it)
        }
        finish()
    }
}

@Composable
private fun WebsiteBlockedScreen(
    blockedUrl: String,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.Block,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.website_blocked_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.website_blocked_attempt_message),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = blockedUrl,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.website_blocked_description),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.website_blocked_acknowledge))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.website_blocked_manage_hint),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
