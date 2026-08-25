package com.momentummm.app.ui.screen.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.momentummm.app.R
import com.momentummm.app.data.repository.SubscriptionRepository
import com.momentummm.app.ui.system.MomentumCard
import com.momentummm.app.ui.system.MomentumDesign
import com.momentummm.app.ui.system.MomentumDivider
import com.momentummm.app.ui.theme.momentum
import com.momentummm.app.data.manager.BillingManager
import com.momentummm.app.data.model.SubscriptionPlan
import com.momentummm.app.data.model.SubscriptionPlans

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onSubscriptionSelected: (String, Boolean) -> Unit,
    onStartTrial: () -> Unit,
    onBackClick: () -> Unit,
    isTrialAvailable: Boolean = true,
    remainingTrialDays: Int = 0,
    billingManager: BillingManager? = null
) {
    val context = LocalContext.current

    val billing = billingManager ?: remember { BillingManager(context) }
    
    var selectedPlan by remember { mutableStateOf("premium") }
    var isYearly by remember { mutableStateOf(true) }
    
    // Initialize billing connection
    LaunchedEffect(Unit) {
        billing.startConnection()
    }

    // Precios reales de Google Play, en la moneda del país del usuario.
    // Se recalculan cuando Play responde; hasta entonces el mapa está vacío y las
    // tarjetas muestran el precio del plan como respaldo.
    val availableProducts by billing.availableProducts.collectAsStateWithLifecycle()
    val playPrices = remember(availableProducts) {
        mapOf(
            BillingManager.PREMIUM_MONTHLY_SKU to
                billing.formattedPriceFor(BillingManager.PREMIUM_MONTHLY_SKU),
            BillingManager.PREMIUM_YEARLY_SKU to
                billing.formattedPriceFor(BillingManager.PREMIUM_YEARLY_SKU)
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        // Top bar
        TopAppBar(
            title = { Text("Momentum Premium") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.a11y_back))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                // Header
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.sub_unlock_title),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.sub_unlock_desc),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Trial Banner
            // La segunda condición no es redundante: startFreeTrial marca isTrialUsed, así
            // que isTrialAvailable pasa a false EN CUANTO empieza la prueba. Con solo la
            // primera condición el banner desaparecía justo al activarla y el usuario no
            // volvía a ver nunca cuántos días le quedaban: la rama "días restantes" de
            // TrialBanner era código inalcanzable.
            if (isTrialAvailable || remainingTrialDays > 0) {
                item {
                    TrialBanner(
                        onStartTrial = onStartTrial,
                        remainingDays = remainingTrialDays
                    )
                }
            }
            
            // Billing Toggle
            item {
                BillingToggle(
                    isYearly = isYearly,
                    onToggle = { isYearly = it }
                )
            }
            
            // Plans
            items(SubscriptionPlans.getAllPlans()) { plan ->
                if (plan.id != "free") {
                    PlanCard(
                        plan = plan,
                        isSelected = selectedPlan == plan.id,
                        isYearly = isYearly,
                        // El precio del plan solo se usa si Play todavía no ha
                        // respondido: el importe real depende del país.
                        playStorePrice = if (plan.id == "free") null else {
                            playPrices[
                                if (isYearly) BillingManager.PREMIUM_YEARLY_SKU
                                else BillingManager.PREMIUM_MONTHLY_SKU
                            ]
                        },
                        onClick = { selectedPlan = plan.id }
                    )
                }
            }
            
            // Features comparison
            item {
                FeaturesComparison()
            }
            
            // CTA Button
            item {
                Button(
                    onClick = { onSubscriptionSelected(selectedPlan, isYearly) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.sub_start_now),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            item {
                Text(
                    text = stringResource(R.string.sub_guarantees),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun TrialBanner(
    onStartTrial: () -> Unit,
    remainingDays: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.AccessTime,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (remainingDays > 0) {
                        stringResource(R.string.subscription_trial_active_title)
                    } else {
                        stringResource(
                            R.string.subscription_trial_offer_title,
                            SubscriptionRepository.TRIAL_DAYS
                        )
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = if (remainingDays > 0) {
                        stringResource(
                            R.string.subscription_trial_active_subtitle,
                            remainingDays
                        )
                    } else {
                        stringResource(R.string.subscription_trial_offer_subtitle)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            if (remainingDays == 0) {
                Button(
                    onClick = onStartTrial,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(stringResource(R.string.subscription_trial_start_button))
                }
            }
        }
    }
}

@Composable
private fun BillingToggle(
    isYearly: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            BillingOption(
                text = stringResource(R.string.sub_monthly),
                isSelected = !isYearly,
                onClick = { onToggle(false) },
                modifier = Modifier.weight(1f)
            )
            BillingOption(
                text = stringResource(R.string.sub_yearly),
                subtitle = stringResource(R.string.sub_save_percent, 33),
                isSelected = isYearly,
                onClick = { onToggle(true) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BillingOption(
    text: String,
    subtitle: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                Color.Transparent
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: SubscriptionPlan,
    isSelected: Boolean,
    isYearly: Boolean,
    /**
     * Precio tal y como lo devuelve Google Play, ya formateado en la moneda del país
     * del usuario. Null mientras la conexión con Play no esté lista o si el producto
     * no está publicado; en ese caso se usa el precio del plan como respaldo.
     */
    playStorePrice: String? = null,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary

    MomentumCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        containerColor = if (isSelected) {
            accent.copy(alpha = MomentumDesign.Alpha.subtle)
        } else {
            MaterialTheme.momentum.surface
        },
        // El plan seleccionado se marca con borde de acento en vez de con sombra:
        // en modo oscuro la elevación no se percibe y la selección quedaba ambigua.
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) accent else MaterialTheme.momentum.border
        )
    ) {
        Column(modifier = Modifier.padding(MomentumDesign.Spacing.cozy)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Radio dibujado a mano para que el anillo use el acento del tema
                // y crezca con el estado seleccionado.
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(MomentumDesign.Shapes.pill)
                        .background(
                            if (isSelected) accent else MaterialTheme.momentum.surfaceSunken
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(MomentumDesign.Spacing.compact))

                Text(
                    text = plan.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.momentum.textPrimary,
                    modifier = Modifier.weight(1f)
                )

                if (plan.isPopular) {
                    Text(
                        text = stringResource(R.string.subscription_best_value),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .clip(MomentumDesign.Shapes.pill)
                            .background(accent)
                            .padding(
                                horizontal = MomentumDesign.Spacing.small,
                                vertical = 4.dp
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(MomentumDesign.Spacing.medium))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    modifier = Modifier.alignByBaseline(),
                    text = playStorePrice
                        ?: if (isYearly) plan.priceYearly else plan.priceMonthly,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.momentum.textPrimary
                )
                Spacer(modifier = Modifier.width(MomentumDesign.Spacing.extraSmall))
                Text(
                    modifier = Modifier.alignByBaseline(),
                    text = if (isYearly) {
                        stringResource(R.string.subscription_per_year)
                    } else {
                        stringResource(R.string.subscription_per_month)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.momentum.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(MomentumDesign.Spacing.medium))
            MomentumDivider()
            Spacer(modifier = Modifier.height(MomentumDesign.Spacing.medium))

            plan.features.take(5).forEach { feature ->
                Row(
                    modifier = Modifier.padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(MomentumDesign.Shapes.pill)
                            .background(accent.copy(alpha = MomentumDesign.Alpha.soft)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(MomentumDesign.Spacing.compact))
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.momentum.textPrimary
                    )
                }
            }

            if (plan.features.size > 5) {
                Spacer(modifier = Modifier.height(MomentumDesign.Spacing.small))
                Text(
                    text = stringResource(
                        R.string.subscription_more_features,
                        plan.features.size - 5
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = accent
                )
            }
        }
    }
}

@Composable
private fun FeaturesComparison() {
    Column {
        Text(
            text = stringResource(R.string.sub_why_premium),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        val features = listOf(
            Triple(stringResource(R.string.sub_f_basic_analytics), true, true),
            Triple(stringResource(R.string.sub_f_basic_widget), true, true),
            Triple(stringResource(R.string.sub_f_adv_analytics), false, true),
            Triple(stringResource(R.string.sub_f_focus), false, true),
            Triple(stringResource(R.string.sub_f_themes), false, true),
            Triple(stringResource(R.string.sub_f_export), false, true),
            Triple(stringResource(R.string.sub_f_profiles), false, true),
            Triple(stringResource(R.string.sub_f_support), false, true)
        )
        
        Card {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row {
                    Text(
                        text = stringResource(R.string.sub_feature),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(2f)
                    )
                    Text(
                        text = stringResource(R.string.sub_free),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(R.string.sub_premium),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                features.forEach { (feature, free, premium) ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(2f)
                        )
                        Icon(
                            if (free) Icons.Filled.Check else Icons.Filled.Close,
                            contentDescription = null,
                            tint = if (free) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .size(20.dp)
                                .wrapContentWidth()
                        )
                        Icon(
                            if (premium) Icons.Filled.Check else Icons.Filled.Close,
                            contentDescription = null,
                            tint = if (premium) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .size(20.dp)
                                .wrapContentWidth()
                        )
                    }
                }
            }
        }
    }
}