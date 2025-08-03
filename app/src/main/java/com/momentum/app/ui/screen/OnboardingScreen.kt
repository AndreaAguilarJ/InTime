package com.momentum.app.ui.screen

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.momentum.app.R
import com.momentum.app.ui.viewmodel.OnboardingViewModel
import com.momentum.app.ui.viewmodel.OnboardingStep
import com.momentum.app.util.PermissionUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onCompleted: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            onCompleted()
        }
    }

    LaunchedEffect(Unit) {
        // Check initial permission status
        val hasPermission = PermissionUtils.hasUsageStatsPermission(context)
        viewModel.updatePermissionStatus(hasPermission)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (uiState.currentStep) {
            OnboardingStep.WELCOME -> {
                WelcomeStep(
                    onNext = { viewModel.nextStep() }
                )
            }
            OnboardingStep.PERMISSIONS -> {
                PermissionsStep(
                    hasPermission = uiState.hasUsagePermission,
                    onPermissionGranted = { 
                        viewModel.updatePermissionStatus(true)
                        viewModel.nextStep()
                    },
                    onRequestPermission = {
                        PermissionUtils.openUsageStatsSettings(context)
                    }
                )
            }
            OnboardingStep.BIRTH_DATE -> {
                BirthDateStep(
                    selectedDate = uiState.selectedBirthDate,
                    onDateSelected = { date ->
                        viewModel.setBirthDate(date)
                        viewModel.nextStep()
                    }
                )
            }
            OnboardingStep.COMPLETED -> {
                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text(
                        text = "¡Configuración completada!",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep(
    onNext: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = stringResource(R.string.welcome_title),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = stringResource(R.string.welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.continue_button))
        }
    }
}

@Composable
private fun PermissionsStep(
    hasPermission: Boolean,
    onPermissionGranted: () -> Unit,
    onRequestPermission: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = stringResource(R.string.permission_title),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = stringResource(R.string.usage_stats_permission_rationale),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (hasPermission) {
            Button(
                onClick = onPermissionGranted,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.continue_button))
            }
        } else {
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.grant_permission))
            }
        }
    }
}

@Composable
private fun BirthDateStep(
    selectedDate: Date?,
    onDateSelected: (Date) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            onDateSelected(calendar.time)
        },
        calendar.get(Calendar.YEAR) - 25, // Default to 25 years ago
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = stringResource(R.string.birth_date_title),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = stringResource(R.string.birth_date_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        selectedDate?.let { date ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Fecha seleccionada: ${dateFormatter.format(date)}",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Button(
            onClick = { datePickerDialog.show() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (selectedDate == null) "Seleccionar fecha de nacimiento" else "Cambiar fecha")
        }
        
        selectedDate?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onDateSelected(it) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.continue_button))
            }
        }
    }
}