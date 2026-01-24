package com.momentummm.app.minimal

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinimalDialerScreen(
    onCall: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var phoneNumber by remember { mutableStateOf("") }
    val hapticFeedback = LocalHapticFeedback.current
    
    val dialpadButtons = listOf(
        DialpadButton("1", ""),
        DialpadButton("2", "ABC"),
        DialpadButton("3", "DEF"),
        DialpadButton("4", "GHI"),
        DialpadButton("5", "JKL"),
        DialpadButton("6", "MNO"),
        DialpadButton("7", "PQRS"),
        DialpadButton("8", "TUV"),
        DialpadButton("9", "WXYZ"),
        DialpadButton("*", ""),
        DialpadButton("0", "+"),
        DialpadButton("#", "")
    )
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header minimalista
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onBack()
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Rounded.ArrowBack, 
                    contentDescription = "Volver",
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = "Marcador",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(40.dp))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Display de número de teléfono
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = if (phoneNumber.isEmpty()) "Ingresa un número" else formatPhoneNumber(phoneNumber),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        letterSpacing = 2.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = if (phoneNumber.isEmpty()) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontWeight = FontWeight.Light
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Dialpad
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(dialpadButtons) { button ->
                DialpadButtonComponent(
                    button = button,
                    onClick = { 
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        phoneNumber += button.number 
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Botones de acción
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Backspace
            FilledTonalIconButton(
                onClick = {
                    if (phoneNumber.isNotEmpty()) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        phoneNumber = phoneNumber.dropLast(1)
                    }
                },
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                enabled = phoneNumber.isNotEmpty()
            ) {
                Icon(
                    Icons.Default.Backspace, 
                    contentDescription = "Borrar",
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Call button con animación
            val callButtonScale by animateFloatAsState(
                targetValue = if (phoneNumber.isNotEmpty()) 1f else 0.9f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "call_scale"
            )
            
            FloatingActionButton(
                onClick = { 
                    if (phoneNumber.isNotEmpty()) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCall(phoneNumber)
                        phoneNumber = ""
                    }
                },
                modifier = Modifier
                    .size(72.dp)
                    .graphicsLayer {
                        scaleX = callButtonScale
                        scaleY = callButtonScale
                    },
                shape = CircleShape,
                containerColor = if (phoneNumber.isNotEmpty()) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(
                    Icons.Default.Call, 
                    contentDescription = "Llamar",
                    modifier = Modifier.size(28.dp),
                    tint = if (phoneNumber.isNotEmpty())
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            
            // Spacer para balance visual
            Spacer(modifier = Modifier.size(64.dp))
        }
        
        Spacer(modifier = Modifier.weight(1f))
    }
}

// Formatea el número de teléfono para mejor legibilidad
private fun formatPhoneNumber(number: String): String {
    return when {
        number.length <= 3 -> number
        number.length <= 6 -> "${number.take(3)} ${number.drop(3)}"
        number.length <= 10 -> "${number.take(3)} ${number.drop(3).take(3)} ${number.drop(6)}"
        else -> number
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialpadButtonComponent(
    button: DialpadButton,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = button.number,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (button.letters.isNotEmpty()) {
                Text(
                    text = button.letters,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

private data class DialpadButton(
    val number: String,
    val letters: String
)