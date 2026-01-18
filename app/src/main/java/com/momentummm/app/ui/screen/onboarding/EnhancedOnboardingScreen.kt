package com.momentummm.app.ui.screen.onboarding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.momentummm.app.MomentumApplication
import com.momentummm.app.ui.viewmodel.OnboardingViewModel
import com.momentummm.app.ui.viewmodel.OnboardingViewModelFactory
import com.momentummm.app.util.PermissionUtils

private enum class OnboardingWizardStep {
	SHOCK_REALITY, // Nueva pantalla de impacto psicológico
	WELCOME,
	USAGE_STATS,
	NOTIFICATIONS,
	OVERLAY,
	FINISH
}

@Composable
fun EnhancedOnboardingScreen(
	onCompleted: () -> Unit,
	viewModel: OnboardingViewModel = run {
		val context = LocalContext.current
		val application = context.applicationContext as MomentumApplication
		viewModel(factory = OnboardingViewModelFactory(context, application.userRepository))
	}
) {
	val context = LocalContext.current
	val lifecycleOwner = LocalLifecycleOwner.current
	val uiState by viewModel.uiState.collectAsState()
	val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()

	var currentStep by rememberSaveable { mutableIntStateOf(OnboardingWizardStep.WELCOME.ordinal) }

	var hasUsagePermission by remember {
		mutableStateOf(PermissionUtils.hasUsageStatsPermission(context))
	}
	var hasNotificationPermission by remember {
		mutableStateOf(
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				ContextCompat.checkSelfPermission(
					context,
					Manifest.permission.POST_NOTIFICATIONS
				) == PackageManager.PERMISSION_GRANTED
			} else {
				true
			}
		)
	}
	var hasOverlayPermission by remember {
		mutableStateOf(
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				Settings.canDrawOverlays(context)
			} else {
				true
			}
		)
	}

	val notificationPermissionLauncher = rememberLauncherForActivityResult(
		ActivityResultContracts.RequestPermission()
	) { granted ->
		hasNotificationPermission = granted
	}

	LaunchedEffect(onboardingCompleted, uiState.isCompleted) {
		if (onboardingCompleted || uiState.isCompleted) {
			onCompleted()
		}
	}

	LaunchedEffect(hasUsagePermission, currentStep) {
		if (currentStep == OnboardingWizardStep.USAGE_STATS.ordinal && hasUsagePermission) {
			currentStep = OnboardingWizardStep.NOTIFICATIONS.ordinal
		}
	}

	androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
		val observer = LifecycleEventObserver { _, event ->
			if (event == Lifecycle.Event.ON_RESUME) {
				hasUsagePermission = PermissionUtils.hasUsageStatsPermission(context)
				hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					Settings.canDrawOverlays(context)
				} else {
					true
				}
				hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
					ContextCompat.checkSelfPermission(
						context,
						Manifest.permission.POST_NOTIFICATIONS
					) == PackageManager.PERMISSION_GRANTED
				} else {
					true
				}
			}
		}
		lifecycleOwner.lifecycle.addObserver(observer)
		onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
	}

	val totalSteps = OnboardingWizardStep.values().size
	val currentStepEnum = OnboardingWizardStep.values()[currentStep]
	val progress = (currentStep + 1).toFloat() / totalSteps.toFloat()

	// La pantalla de shock ocupa toda la pantalla sin UI de wizard
	if (currentStepEnum == OnboardingWizardStep.SHOCK_REALITY) {
		val application = context.applicationContext as MomentumApplication
		ShockOnboardingScreen(
			usageStatsRepository = application.usageStatsRepository,
			userBirthYear = null, // Se puede agregar selección de edad después
			onContinue = { currentStep = OnboardingWizardStep.WELCOME.ordinal },
			onSkip = { currentStep = OnboardingWizardStep.WELCOME.ordinal }
		)
		return
	}

	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(24.dp),
		verticalArrangement = Arrangement.spacedBy(20.dp)
	) {
		Text(
			text = "Paso ${currentStep} de ${totalSteps - 1}", // Excluir shock del contador
			style = MaterialTheme.typography.labelLarge,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
		LinearProgressIndicator(progress = currentStep.toFloat() / (totalSteps - 1).toFloat(), modifier = Modifier.fillMaxWidth())

		when (currentStepEnum) {
			OnboardingWizardStep.SHOCK_REALITY -> {
				// Handled above, never reaches here
			}
			OnboardingWizardStep.WELCOME -> {
				StepContainer(
					title = "Bienvenido a Momentum",
					description = "Configura permisos clave para que el bloqueo y el seguimiento funcionen al 100%."
				) {
					Button(
						onClick = { currentStep = OnboardingWizardStep.USAGE_STATS.ordinal },
						modifier = Modifier.fillMaxWidth()
					) {
						Text("Comenzar")
					}
				}
			}
			OnboardingWizardStep.USAGE_STATS -> {
				StepContainer(
					title = "Permiso de uso",
					description = "Necesitamos acceso a estadísticas de uso para medir tiempo y aplicar límites."
				) {
					Row(
						horizontalArrangement = Arrangement.spacedBy(12.dp),
						modifier = Modifier.fillMaxWidth()
					) {
						OutlinedButton(
							onClick = { PermissionUtils.openUsageStatsSettings(context) },
							modifier = Modifier.weight(1f)
						) {
							Text("Ir a ajustes")
						}
						Button(
							onClick = { currentStep = OnboardingWizardStep.NOTIFICATIONS.ordinal },
							enabled = hasUsagePermission,
							modifier = Modifier.weight(1f)
						) {
							Text("Siguiente")
						}
					}
					if (hasUsagePermission) {
						Text(
							text = "Permiso concedido",
							style = MaterialTheme.typography.bodyMedium,
							color = MaterialTheme.colorScheme.primary
						)
					}
				}
			}
			OnboardingWizardStep.NOTIFICATIONS -> {
				StepContainer(
					title = "Permiso de notificaciones",
					description = "Usamos notificaciones para recordatorios y alertas de bloqueo."
				) {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
						Row(
							horizontalArrangement = Arrangement.spacedBy(12.dp),
							modifier = Modifier.fillMaxWidth()
						) {
							OutlinedButton(
								onClick = {
									notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
								},
								modifier = Modifier.weight(1f)
							) {
								Text("Permitir")
							}
							Button(
								onClick = { currentStep = OnboardingWizardStep.OVERLAY.ordinal },
								enabled = hasNotificationPermission,
								modifier = Modifier.weight(1f)
							) {
								Text("Siguiente")
							}
						}
						if (!hasNotificationPermission) {
							OutlinedButton(
								onClick = { currentStep = OnboardingWizardStep.OVERLAY.ordinal },
								modifier = Modifier.fillMaxWidth()
							) {
								Text("Omitir")
							}
						} else {
							Text(
								text = "Permiso concedido",
								style = MaterialTheme.typography.bodyMedium,
								color = MaterialTheme.colorScheme.primary
							)
						}
					} else {
						Text(
							text = "Tu versión de Android no requiere este permiso.",
							style = MaterialTheme.typography.bodyMedium,
							color = MaterialTheme.colorScheme.onSurfaceVariant
						)
						Button(
							onClick = { currentStep = OnboardingWizardStep.OVERLAY.ordinal },
							modifier = Modifier.fillMaxWidth()
						) {
							Text("Siguiente")
						}
					}
				}
			}
			OnboardingWizardStep.OVERLAY -> {
				StepContainer(
					title = "Permiso de superposición",
					description = "Recomendado para mostrar la pantalla de bloqueo sobre otras apps."
				) {
					Row(
						horizontalArrangement = Arrangement.spacedBy(12.dp),
						modifier = Modifier.fillMaxWidth()
					) {
						OutlinedButton(
							onClick = {
								val intent = Intent(
									Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
									Uri.parse("package:${context.packageName}")
								)
								context.startActivity(intent)
							},
							modifier = Modifier.weight(1f)
						) {
							Text("Habilitar")
						}
						Button(
							onClick = { currentStep = OnboardingWizardStep.FINISH.ordinal },
							modifier = Modifier.weight(1f)
						) {
							Text("Siguiente")
						}
					}
					if (hasOverlayPermission) {
						Text(
							text = "Permiso concedido",
							style = MaterialTheme.typography.bodyMedium,
							color = MaterialTheme.colorScheme.primary
						)
					} else {
						Text(
							text = "Opcional: puedes activarlo más tarde desde Ajustes.",
							style = MaterialTheme.typography.bodyMedium,
							color = MaterialTheme.colorScheme.onSurfaceVariant
						)
					}
				}
			}
			OnboardingWizardStep.FINISH -> {
				StepContainer(
					title = "Todo listo",
					description = "¡Momentum está preparado para ayudarte a enfocarte!"
				) {
					Button(
						onClick = { viewModel.completeOnboarding() },
						modifier = Modifier.fillMaxWidth(),
						enabled = !uiState.isLoading
					) {
						if (uiState.isLoading) {
							Row(
								verticalAlignment = Alignment.CenterVertically,
								horizontalArrangement = Arrangement.spacedBy(8.dp)
							) {
								CircularProgressIndicator(
									strokeWidth = 2.dp,
									modifier = Modifier.height(18.dp)
								)
								Text("Guardando...")
							}
						} else {
							Text("Comenzar")
						}
					}
				}
			}
		}
	}
}

@Composable
private fun StepContainer(
	title: String,
	description: String,
	content: @Composable () -> Unit
) {
	Column(
		verticalArrangement = Arrangement.spacedBy(16.dp),
		modifier = Modifier.fillMaxWidth()
	) {
		Text(
			text = title,
			style = MaterialTheme.typography.headlineMedium,
			fontWeight = FontWeight.SemiBold
		)
		Text(
			text = description,
			style = MaterialTheme.typography.bodyLarge,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
		Spacer(modifier = Modifier.height(8.dp))
		content()
	}
}
