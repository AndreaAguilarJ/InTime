package com.momentummm.app.ui.screen.auth

import android.util.Patterns
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.momentummm.app.R
import com.momentummm.app.ui.system.ButtonSize
import com.momentummm.app.ui.system.ButtonStyle
import com.momentummm.app.ui.system.MomentumButton
import com.momentummm.app.ui.system.MomentumDesign
import com.momentummm.app.ui.theme.momentum
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.momentummm.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    onSignUpClick: () -> Unit,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.momentum.canvas)
    ) {
        // Velo de marca en la parte alta: da presencia a la pantalla de entrada sin
        // teñir el texto ni comprometer el contraste de los botones.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .background(MaterialTheme.momentum.veil(0.22f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(
                    horizontal = MomentumDesign.Spacing.large,
                    vertical = MomentumDesign.Spacing.large
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(MomentumDesign.Spacing.extraLarge))

            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(MomentumDesign.CornerRadius.hero))
                    .background(MaterialTheme.momentum.brandGradient),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.a11y_app_logo),
                    modifier = Modifier.size(96.dp)
                )
            }

            Spacer(modifier = Modifier.height(MomentumDesign.Spacing.extraLarge))

            Text(
                text = stringResource(R.string.auth_welcome_title),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.momentum.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(MomentumDesign.Spacing.compact))

            Text(
                text = stringResource(R.string.auth_welcome_subtitle),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(MomentumDesign.Spacing.medium))

            Text(
                text = stringResource(R.string.auth_welcome_desc),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.momentum.textSecondary
            )

            Spacer(modifier = Modifier.height(MomentumDesign.Spacing.huge))

            MomentumButton(
                onClick = onSignUpClick,
                style = ButtonStyle.Primary,
                size = ButtonSize.Large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.auth_create_account))
            }

            Spacer(modifier = Modifier.height(MomentumDesign.Spacing.compact))

            MomentumButton(
                onClick = onSignInClick,
                style = ButtonStyle.Outline,
                size = ButtonSize.Large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.auth_sign_in))
            }

            Spacer(modifier = Modifier.height(MomentumDesign.Spacing.large))

            Text(
                text = stringResource(R.string.auth_terms_continue),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.momentum.textTertiary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onSignUp: (String, String, String) -> Unit,
    onBackToWelcome: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showPasswordTips by remember { mutableStateOf(false) }
    
    val isFormValid = name.isNotBlank() && 
                     isValidEmail(email) && 
                     isValidPassword(password) && 
                     password == confirmPassword
    
    val passwordStrength = getPasswordStrength(password)
    val passwordTips = getPasswordTips(password)
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.momentum.canvas)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = stringResource(R.string.auth_create_account),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.auth_signup_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Name Field
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.auth_field_full_name)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = {
                if (name.isNotEmpty() && name.length < 2) {
                    Text(
                        text = stringResource(R.string.auth_error_name_too_short),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            isError = name.isNotEmpty() && name.length < 2
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.auth_field_email)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = {
                if (email.isNotEmpty() && !isValidEmail(email)) {
                    Text(
                        text = stringResource(R.string.auth_error_invalid_email),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            isError = email.isNotEmpty() && !isValidEmail(email)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.auth_field_password)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                Row {
                    IconButton(onClick = { showPasswordTips = !showPasswordTips }) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = stringResource(R.string.auth_a11y_password_tips)
                        )
                    }
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = stringResource(if (passwordVisible) R.string.auth_a11y_hide_password else R.string.auth_a11y_show_password)
                        )
                    }
                }
            },
            supportingText = {
                if (password.isNotEmpty()) {
                    Column {
                        Text(
                            text = stringResource(R.string.auth_password_strength, stringResource(passwordStrength.labelRes())),
                            color = when (passwordStrength) {
                                PasswordStrength.WEAK -> MaterialTheme.colorScheme.error
                                PasswordStrength.MEDIUM -> Amber500
                                PasswordStrength.STRONG -> Mint500
                            }
                        )
                        if (showPasswordTips && passwordTips.isNotEmpty()) {
                            passwordTips.forEach { tip ->
                                Text(
                                    text = stringResource(R.string.auth_bullet, stringResource(tip)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Confirm Password Field
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text(stringResource(R.string.auth_field_confirm_password)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(
                        imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = stringResource(if (confirmPasswordVisible) R.string.auth_a11y_hide_password else R.string.auth_a11y_show_password)
                    )
                }
            },
            isError = confirmPassword.isNotEmpty() && password != confirmPassword,
            supportingText = {
                if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                    Text(
                        text = stringResource(R.string.auth_error_passwords_differ),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Account creation tips
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.auth_why_create_account),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                stringArrayResource(R.array.auth_signup_benefits).forEach { benefit ->
                    Text(
                        text = stringResource(R.string.auth_bullet, benefit),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Sign Up Button
        MomentumButton(
            onClick = { onSignUp(name, email, password) },
            enabled = isFormValid && !isLoading,
            style = ButtonStyle.Primary,
            size = ButtonSize.Large,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(stringResource(R.string.auth_create_account))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(
            onClick = onBackToWelcome
        ) {
            Text(stringResource(R.string.auth_have_account))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.auth_terms_create),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun isValidEmail(email: String): Boolean {
    return Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

private fun isValidPassword(password: String): Boolean {
    return password.length >= 8
}

private enum class PasswordStrength {
    WEAK, MEDIUM, STRONG
}

private fun getPasswordStrength(password: String): PasswordStrength {
    var score = 0
    if (password.length >= 8) score++
    if (password.any { it.isUpperCase() }) score++
    if (password.any { it.isLowerCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    
    return when {
        score < 3 -> PasswordStrength.WEAK
        score < 5 -> PasswordStrength.MEDIUM
        else -> PasswordStrength.STRONG
    }
}

/**
 * Consejos de contraseña como ids de recurso, no como texto.
 *
 * Esta función no es `@Composable` y no tiene `Context`, así que devolver
 * cadenas obligaba a escribirlas en español aquí mismo: un usuario con el
 * teléfono en inglés leía los consejos traducidos a medias. Quien los pinta
 * resuelve el recurso.
 */
@StringRes
private fun getPasswordTips(password: String): List<Int> {
    val tips = mutableListOf<Int>()
    if (password.length < 8) tips.add(R.string.auth_password_tip_length)
    if (password.none { it.isUpperCase() }) tips.add(R.string.auth_password_tip_upper)
    if (password.none { it.isLowerCase() }) tips.add(R.string.auth_password_tip_lower)
    if (password.none { it.isDigit() }) tips.add(R.string.auth_password_tip_digit)
    if (password.none { !it.isLetterOrDigit() }) tips.add(R.string.auth_password_tip_symbol)
    return tips
}

/** Etiqueta traducible de la fortaleza; el nombre del enum no se muestra nunca. */
@StringRes
private fun PasswordStrength.labelRes(): Int = when (this) {
    PasswordStrength.WEAK -> R.string.auth_password_weak
    PasswordStrength.MEDIUM -> R.string.auth_password_medium
    PasswordStrength.STRONG -> R.string.auth_password_strong
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    onSignIn: (String, String) -> Unit,
    onBackToWelcome: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    val isFormValid = email.isNotBlank() && password.isNotBlank()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.momentum.canvas)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = stringResource(R.string.auth_sign_in),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.auth_field_email)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.auth_field_password)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = stringResource(if (passwordVisible) R.string.auth_a11y_hide_password else R.string.auth_a11y_show_password)
                    )
                }
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Sign In Button
        MomentumButton(
            onClick = { onSignIn(email, password) },
            enabled = isFormValid && !isLoading,
            modifier = Modifier.fillMaxWidth(),
            style = ButtonStyle.Primary,
            size = ButtonSize.Large,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(stringResource(R.string.auth_sign_in))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(
            onClick = onBackToWelcome
        ) {
            Text(stringResource(R.string.auth_no_account))
        }
    }
}