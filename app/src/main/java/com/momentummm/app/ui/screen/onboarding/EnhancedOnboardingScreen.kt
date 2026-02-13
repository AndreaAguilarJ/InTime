package com.momentummm.app.ui.screen.onboarding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()
	val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()

	var currentStep by rememberSaveable { mutableIntStateOf(OnboardingWizardStep.SHOCK_REALITY.ordinal) }

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

	val totalSteps = OnboardingWizardStep.entries.size
	// Proteger contra IndexOutOfBoundsException
	val currentStepEnum = OnboardingWizardStep.entries.getOrNull(currentStep) ?: OnboardingWizardStep.WELCOME
	val progress = (currentStep + 1).toFloat() / totalSteps.coerceAtLeast(1).toFloat()

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
		LinearProgressIndicator(
			progress = if (totalSteps > 1) currentStep.toFloat() / (totalSteps - 1).toFloat() else 0f,
			modifier = Modifier.fillMaxWidth()
		)

		when (currentStepEnum) {
			OnboardingWizardStep.SHOCK_REALITY -> {
				// Handled above, never reaches here
			}
			OnboardingWizardStep.WELCOME -> {
				StepContainerEnhanced(
					icon = Icons.Default.Rocket,
					emoji = "🚀",
					title = "¡Bienvenido a Momentum!",
					description = "Configura permisos clave para que el bloqueo y el seguimiento funcionen al 100%.",
					iconColor = MaterialTheme.colorScheme.primary
				) {
					Spacer(modifier = Modifier.height(16.dp))
					Button(
						onClick = { currentStep = OnboardingWizardStep.USAGE_STATS.ordinal },
						modifier = Modifier
							.fillMaxWidth()
							.height(56.dp),
						shape = RoundedCornerShape(16.dp)
					) {
						Icon(Icons.Default.ArrowForward, contentDescription = null)
						Spacer(modifier = Modifier.size(8.dp))
						Text("Comenzar configuración", fontWeight = FontWeight.Bold)
					}
				}
			}
			OnboardingWizardStep.USAGE_STATS -> {
				StepContainerEnhanced(
					icon = Icons.Default.QueryStats,
					emoji = "📊",
					title = "Permiso de estadísticas",
					description = "Necesitamos acceso a estadísticas de uso para medir tiempo y aplicar límites. Es esencial para el funcionamiento de la app.",
					iconColor = Color(0xFF6366F1),
					isGranted = hasUsagePermission
				) {
					Spacer(modifier = Modifier.height(16.dp))
					
					if (hasUsagePermission) {
						PermissionGrantedCard()
					}
					
					Row(
						horizontalArrangement = Arrangement.spacedBy(12.dp),
						modifier = Modifier.fillMaxWidth()
					) {
						OutlinedButton(
							onClick = { PermissionUtils.openUsageStatsSettings(context) },
							modifier = Modifier
								.weight(1f)
								.height(52.dp),
							shape = RoundedCornerShape(12.dp)
						) {
							Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
							Spacer(modifier = Modifier.size(4.dp))
							Text("Abrir ajustes")
						}
						Button(
							onClick = { currentStep = OnboardingWizardStep.NOTIFICATIONS.ordinal },
							enabled = hasUsagePermission,
							modifier = Modifier
								.weight(1f)
								.height(52.dp),
							shape = RoundedCornerShape(12.dp)
						) {
							Text("Siguiente")
							Spacer(modifier = Modifier.size(4.dp))
							Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
						}
					}
				}
			}
			OnboardingWizardStep.NOTIFICATIONS -> {
				StepContainerEnhanced(
					icon = Icons.Default.Notifications,
					emoji = "🔔",
					title = "Permiso de notificaciones",
					description = "Usamos notificaciones para recordatorios y alertas de bloqueo. Te avisaremos cuando estés cerca de tu límite.",
					iconColor = Color(0xFFF59E0B),
					isGranted = hasNotificationPermission
				) {
					Spacer(modifier = Modifier.height(16.dp))
					
					if (hasNotificationPermission) {
						PermissionGrantedCard()
					}
					
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
						Row(
							horizontalArrangement = Arrangement.spacedBy(12.dp),
							modifier = Modifier.fillMaxWidth()
						) {
							OutlinedButton(
								onClick = {
									notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
								},
								modifier = Modifier
									.weight(1f)
									.height(52.dp),
								shape = RoundedCornerShape(12.dp)
							) {
								Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
								Spacer(modifier = Modifier.size(4.dp))
								Text("Permitir")
							}
							Button(
								onClick = { currentStep = OnboardingWizardStep.OVERLAY.ordinal },
								enabled = hasNotificationPermission,
								modifier = Modifier
									.weight(1f)
									.height(52.dp),
								shape = RoundedCornerShape(12.dp)
							) {
								Text("Siguiente")
								Spacer(modifier = Modifier.size(4.dp))
								Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
							}
						}
						if (!hasNotificationPermission) {
							Spacer(modifier = Modifier.height(8.dp))
							OutlinedButton(
								onClick = { currentStep = OnboardingWizardStep.OVERLAY.ordinal },
								modifier = Modifier
									.fillMaxWidth()
									.height(48.dp),
								shape = RoundedCornerShape(12.dp)
							) {
								Text("Omitir por ahora")
							}
						}
					} else {
						Card(
							colors = CardDefaults.cardColors(
								containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
							),
							shape = RoundedCornerShape(12.dp)
						) {
							Row(
								modifier = Modifier.padding(12.dp),
								verticalAlignment = Alignment.CenterVertically
							) {
								Icon(
									Icons.Default.Info,
									contentDescription = null,
									tint = MaterialTheme.colorScheme.onSurfaceVariant
								)
								Spacer(modifier = Modifier.size(8.dp))
								Text(
									text = "Tu versión de Android no requiere este permiso.",
									style = MaterialTheme.typography.bodyMedium,
									color = MaterialTheme.colorScheme.onSurfaceVariant
								)
							}
						}
						Spacer(modifier = Modifier.height(8.dp))
						Button(
							onClick = { currentStep = OnboardingWizardStep.OVERLAY.ordinal },
							modifier = Modifier
								.fillMaxWidth()
								.height(52.dp),
							shape = RoundedCornerShape(12.dp)
						) {
							Text("Siguiente")
							Spacer(modifier = Modifier.size(4.dp))
							Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
						}
					}
				}
			}
			OnboardingWizardStep.OVERLAY -> {
				StepContainerEnhanced(
					icon = Icons.Default.Layers,
					emoji = "📱",
					title = "Permiso de superposición",
					description = "Recomendado para mostrar la pantalla de bloqueo sobre otras apps. Esto permite que Momentum te proteja efectivamente.",
					iconColor = Color(0xFF10B981),
					isGranted = hasOverlayPermission
				) {
					Spacer(modifier = Modifier.height(16.dp))
					
					if (hasOverlayPermission) {
						PermissionGrantedCard()
					} else {
						Card(
							colors = CardDefaults.cardColors(
								containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
							),
							shape = RoundedCornerShape(12.dp)
						) {
							Row(
								modifier = Modifier.padding(12.dp),
								verticalAlignment = Alignment.CenterVertically
							) {
								Icon(
									Icons.Default.Info,
									contentDescription = null,
									tint = MaterialTheme.colorScheme.onSurfaceVariant,
									modifier = Modifier.size(20.dp)
								)
								Spacer(modifier = Modifier.size(8.dp))
								Text(
									text = "Opcional: puedes activarlo más tarde desde Ajustes.",
									style = MaterialTheme.typography.bodySmall,
									color = MaterialTheme.colorScheme.onSurfaceVariant
								)
							}
						}
					}
					
					Spacer(modifier = Modifier.height(8.dp))
					
					Row(
						horizontalArrangement = Arrangement.spacedBy(12.dp),
						modifier = Modifier.fillMaxWidth()
					) {
						OutlinedButton(
							onClick = {
								try {
									val intent = Intent(
										Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
										Uri.parse("package:${context.packageName}")
									)
									context.startActivity(intent)
								} catch (e: Exception) {
									try {
										context.startActivity(Intent(Settings.ACTION_SETTINGS))
									} catch (_: Exception) { }
								}
							},
							modifier = Modifier
								.weight(1f)
								.height(52.dp),
							shape = RoundedCornerShape(12.dp)
						) {
							Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
							Spacer(modifier = Modifier.size(4.dp))
							Text("Habilitar")
						}
						Button(
							onClick = { currentStep = OnboardingWizardStep.FINISH.ordinal },
							modifier = Modifier
								.weight(1f)
								.height(52.dp),
							shape = RoundedCornerShape(12.dp)
						) {
							Text("Siguiente")
							Spacer(modifier = Modifier.size(4.dp))
							Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
						}
					}
				}
			}
			OnboardingWizardStep.FINISH -> {
				StepContainerEnhanced(
					icon = Icons.Default.CheckCircle,
					emoji = "🎉",
					title = "¡Todo listo!",
					description = "Momentum está preparado para ayudarte a recuperar el control de tu tiempo. A continuación configuraremos tu perfil.",
					iconColor = Color(0xFF22C55E)
				) {
					Spacer(modifier = Modifier.height(24.dp))
					
					// Resumen de permisos
					Card(
						colors = CardDefaults.cardColors(
							containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
						),
						shape = RoundedCornerShape(16.dp)
					) {
						Column(
							modifier = Modifier.padding(16.dp),
							verticalArrangement = Arrangement.spacedBy(12.dp)
						) {
							Text(
								"Resumen de configuración:",
								style = MaterialTheme.typography.titleSmall,
								fontWeight = FontWeight.Bold
							)
							PermissionSummaryRow("Estadísticas de uso", hasUsagePermission)
							PermissionSummaryRow("Notificaciones", hasNotificationPermission)
							PermissionSummaryRow("Superposición", hasOverlayPermission)
						}
					}
					
					Spacer(modifier = Modifier.height(16.dp))
					
					Button(
						onClick = { viewModel.completeOnboarding() },
						modifier = Modifier
							.fillMaxWidth()
							.height(56.dp),
						enabled = !uiState.isLoading,
						shape = RoundedCornerShape(16.dp),
						colors = ButtonDefaults.buttonColors(
							containerColor = Color(0xFF22C55E)
						)
					) {
						if (uiState.isLoading) {
							Row(
								verticalAlignment = Alignment.CenterVertically,
								horizontalArrangement = Arrangement.spacedBy(8.dp)
							) {
								CircularProgressIndicator(
									strokeWidth = 2.dp,
									modifier = Modifier.size(20.dp),
									color = Color.White
								)
								Text("Guardando...", color = Color.White)
							}
						} else {
							Icon(Icons.Default.PlayArrow, contentDescription = null)
							Spacer(modifier = Modifier.size(8.dp))
							Text("Continuar al tutorial", fontWeight = FontWeight.Bold)
						}
					}
				}
			}
		}
	}
}

@Composable
private fun PermissionSummaryRow(name: String, isGranted: Boolean) {
	Row(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.SpaceBetween,
		verticalAlignment = Alignment.CenterVertically
	) {
		Text(name, style = MaterialTheme.typography.bodyMedium)
		Icon(
			imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Cancel,
			contentDescription = null,
			tint = if (isGranted) Color(0xFF22C55E) else MaterialTheme.colorScheme.error,
			modifier = Modifier.size(20.dp)
		)
	}
}

@Composable
private fun PermissionGrantedCard() {
	Card(
		colors = CardDefaults.cardColors(
			containerColor = Color(0xFF22C55E).copy(alpha = 0.1f)
		),
		shape = RoundedCornerShape(12.dp)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(12.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			Icon(
				Icons.Default.CheckCircle,
				contentDescription = null,
				tint = Color(0xFF22C55E),
				modifier = Modifier.size(24.dp)
			)
			Spacer(modifier = Modifier.size(8.dp))
			Text(
				text = "✓ Permiso concedido correctamente",
				style = MaterialTheme.typography.bodyMedium,
				fontWeight = FontWeight.Medium,
				color = Color(0xFF22C55E)
			)
		}
	}
}

@Composable
private fun StepContainerEnhanced(
	icon: ImageVector,
	emoji: String,
	title: String,
	description: String,
	iconColor: Color,
	isGranted: Boolean = false,
	content: @Composable () -> Unit
) {
	Column(
		verticalArrangement = Arrangement.spacedBy(16.dp),
		modifier = Modifier.fillMaxWidth(),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		// Icono grande con emoji
		Box(
			contentAlignment = Alignment.Center,
			modifier = Modifier
				.size(80.dp)
				.clip(CircleShape)
				.background(iconColor.copy(alpha = 0.1f))
		) {
			Text(
				text = emoji,
				fontSize = 40.sp
			)
		}
		
		Text(
			text = title,
			style = MaterialTheme.typography.headlineSmall,
			fontWeight = FontWeight.Bold,
			textAlign = TextAlign.Center
		)
		Text(
			text = description,
			style = MaterialTheme.typography.bodyLarge,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			textAlign = TextAlign.Center
		)
		
		content()
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
