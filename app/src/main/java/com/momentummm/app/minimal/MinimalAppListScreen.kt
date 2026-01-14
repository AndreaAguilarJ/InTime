package com.momentummm.app.minimal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Intent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinimalAppListScreen(
    minimalPhoneManager: MinimalPhoneManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    // Use remember with key to reload if needed, though getInstalledApps is somewhat heavy
    // ideally it should be a StateFlow in Manager, but getting it once is fine for now
    val allApps = remember { minimalPhoneManager.getInstalledApps() }
    
    val filteredApps = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            allApps
        } else {
            allApps.filter { it.appName.contains(searchQuery, ignoreCase = true) }
        }
    }
    
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding() // Handle safe area
            .padding(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack, 
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar aplicación...") },
                leadingIcon = { 
                    Icon(
                        Icons.Default.Search, 
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    ) 
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                shape = MaterialTheme.shapes.medium
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(filteredApps) { app ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                            if (intent != null) {
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(intent)
                            }
                        }
                        .padding(vertical = 16.dp, horizontal = 12.dp)
                ) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Divider(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    thickness = 0.5.dp
                )
            }
        }
    }
}
