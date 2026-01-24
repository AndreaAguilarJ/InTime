package com.momentummm.app.ui.screen.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import com.momentummm.app.R

data class FAQItem(
    @StringRes val questionRes: Int,
    @StringRes val answerRes: Int,
    val icon: ImageVector = Icons.Default.HelpOutline
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    
    // Strings para usar en lambdas
    val supportEmailUri = stringResource(R.string.help_support_email_uri)
    val emailSubject = stringResource(R.string.help_support_email_subject)
    val emailChooserTitle = stringResource(R.string.help_send_email_chooser)
    val guideUrl = stringResource(R.string.help_guide_url)
    
    val faqItems = remember {
        listOf(
            FAQItem(
                questionRes = R.string.help_faq_q1,
                answerRes = R.string.help_faq_a1,
                icon = Icons.Default.Timer
            ),
            FAQItem(
                questionRes = R.string.help_faq_q2,
                answerRes = R.string.help_faq_a2,
                icon = Icons.Default.PhoneAndroid
            ),
            FAQItem(
                questionRes = R.string.help_faq_q3,
                answerRes = R.string.help_faq_a3,
                icon = Icons.Default.VideoLibrary
            ),
            FAQItem(
                questionRes = R.string.help_faq_q4,
                answerRes = R.string.help_faq_a4,
                icon = Icons.Default.Lock
            ),
            FAQItem(
                questionRes = R.string.help_faq_q5,
                answerRes = R.string.help_faq_a5,
                icon = Icons.Default.Accessibility
            ),
            FAQItem(
                questionRes = R.string.help_faq_q6,
                answerRes = R.string.help_faq_a6,
                icon = Icons.Default.BarChart
            ),
            FAQItem(
                questionRes = R.string.help_faq_q7,
                answerRes = R.string.help_faq_a7,
                icon = Icons.Default.CalendarMonth
            ),
            FAQItem(
                questionRes = R.string.help_faq_q8,
                answerRes = R.string.help_faq_a8,
                icon = Icons.Default.Widgets
            ),
            FAQItem(
                questionRes = R.string.help_faq_q9,
                answerRes = R.string.help_faq_a9,
                icon = Icons.Default.Sync
            ),
            FAQItem(
                questionRes = R.string.help_faq_q10,
                answerRes = R.string.help_faq_a10,
                icon = Icons.Default.LockOpen
            )
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.help_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.help_back_cd)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Sección de Preguntas Frecuentes
            item {
                Text(
                    text = stringResource(R.string.help_faq_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            items(faqItems) { faq ->
                ExpandableFAQCard(faq = faq)
            }
            
            // Sección de Contacto
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.help_contact_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse(supportEmailUri)
                            putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                        }
                        context.startActivity(
                            Intent.createChooser(
                                intent,
                                emailChooserTitle
                            )
                        )
                    }
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.help_contact_support_title)) },
                        supportingContent = { Text(stringResource(R.string.help_contact_support_subtitle)) },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null
                            )
                        }
                    )
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(guideUrl)
                        )
                        context.startActivity(intent)
                    }
                ) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.help_online_guide_title)) },
                        supportingContent = { Text(stringResource(R.string.help_online_guide_subtitle)) },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null
                            )
                        }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ExpandableFAQCard(faq: FAQItem) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = faq.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(faq.questionRes),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) {
                        stringResource(R.string.help_collapse_cd)
                    } else {
                        stringResource(R.string.help_expand_cd)
                    }
                )
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(faq.answerRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
