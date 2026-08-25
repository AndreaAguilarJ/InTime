package com.momentummm.app.ui.screen.community

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
// Necesarios para leer los textos desde recursos en vez de tenerlos escritos a mano.
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.momentummm.app.ui.accessibility.rememberSystemAnimationsEnabled
import com.momentummm.app.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.momentummm.app.ui.system.MomentumCard
import com.momentummm.app.ui.system.MomentumDesign
import com.momentummm.app.ui.theme.momentum
import com.momentummm.app.data.entity.*
import kotlinx.coroutines.delay
import com.momentummm.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    isPremiumUser: Boolean = false,
    onUpgradeClick: () -> Unit = {},
    viewModel: CommunityViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val pendingRequests by viewModel.pendingRequests.collectAsStateWithLifecycle()
    val weeklyLeaderboard by viewModel.weeklyLeaderboard.collectAsStateWithLifecycle()
    val friendsLeaderboard by viewModel.friendsLeaderboard.collectAsStateWithLifecycle()
    val myRank by viewModel.myRank.collectAsStateWithLifecycle()
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Mostrar errores en snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            CommunityTopBar(
                selectedTab = selectedTab,
                onSettingsClick = { showSettingsSheet = true },
                onAddFriendClick = { showAddFriendDialog = true },
                pendingCount = pendingRequests.size
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Modern Tabs with badges
            CommunityTabs(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                friendsCount = friends.size,
                pendingCount = pendingRequests.size
            )
            
            // Content with animation
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith 
                    fadeOut(animationSpec = tween(300))
                },
                label = "tab_content"
            ) { tab ->
                when (tab) {
                    0 -> FriendsTab(
                        friends = friends,
                        pendingRequests = pendingRequests,
                        isLoading = isLoading,
                        onAcceptRequest = { viewModel.acceptFriendRequest(it) },
                        onRejectRequest = { viewModel.rejectFriendRequest(it) },
                        onRemoveFriend = { viewModel.removeFriend(it) },
                        onRefresh = { viewModel.refreshFriends() }
                    )
                    1 -> LeaderboardTab(
                        weeklyLeaderboard = weeklyLeaderboard,
                        friendsLeaderboard = friendsLeaderboard,
                        myRank = myRank,
                        isLoading = isLoading,
                        onRefresh = { viewModel.refreshLeaderboard() }
                    )
                    2 -> AchievementsTab(
                        achievements = achievements,
                        isLoading = isLoading,
                        onShare = { viewModel.shareAchievement(context, it) },
                        onRefresh = { viewModel.refreshAchievements() }
                    )
                }
            }
        }
    }
    
    if (showAddFriendDialog) {
        AddFriendDialog(
            onDismiss = { showAddFriendDialog = false },
            onAddFriend = { email, name ->
                viewModel.sendFriendRequest(email, name)
                showAddFriendDialog = false
            }
        )
    }
    
    if (showSettingsSheet) {
        CommunitySettingsSheet(
            onDismiss = { showSettingsSheet = false },
            viewModel = viewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommunityTopBar(
    selectedTab: Int,
    onSettingsClick: () -> Unit,
    onAddFriendClick: () -> Unit,
    pendingCount: Int
) {
    CenterAlignedTopAppBar(
        title = { 
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "👥",
                    fontSize = 24.sp
                )
                Text(
                    stringResource(R.string.community_title),
                    fontWeight = FontWeight.Bold
                ) 
            }
        },
        actions = {
            // Botón de agregar amigo solo en tab de amigos
            AnimatedVisibility(
                visible = selectedTab == 0,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                IconButton(onClick = onAddFriendClick) {
                    Icon(Icons.Default.PersonAdd, stringResource(R.string.community_add_friend_title))
                }
            }
            
            // Botón de configuración
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Outlined.Settings, "Configuración")
            }
        }
    )
}

@Composable
private fun CommunityTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    friendsCount: Int,
    pendingCount: Int
) {
    val tabs = listOf(
        TabInfo(stringResource(R.string.community_tab_friends), Icons.Default.People, if (pendingCount > 0) "$pendingCount" else null),
        TabInfo("Ranking", Icons.Default.Leaderboard, null),
        TabInfo("Logros", Icons.Default.EmojiEvents, null)
    )
    
    TabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        tabs.forEachIndexed { index, tabInfo ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                text = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(tabInfo.title)
                        // Badge para notificaciones
                        tabInfo.badge?.let { badge ->
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        badge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onError,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                },
                icon = { 
                    Icon(
                        tabInfo.icon, 
                        null,
                        modifier = Modifier.size(20.dp)
                    ) 
                }
            )
        }
    }
}

private data class TabInfo(
    val title: String,
    val icon: ImageVector,
    val badge: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FriendsTab(
    friends: List<Friend>,
    pendingRequests: List<Friend>,
    isLoading: Boolean,
    onAcceptRequest: (String) -> Unit,
    onRejectRequest: (String) -> Unit,
    onRemoveFriend: (String) -> Unit,
    onRefresh: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Solicitudes pendientes con animación
            if (pendingRequests.isNotEmpty()) {
                item(key = "pending_header") {
                    SectionHeader(
                        emoji = "📬",
                        title = stringResource(R.string.community_pending_requests),
                        count = pendingRequests.size,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                
                items(
                    items = pendingRequests,
                    key = { "pending_${it.friendUserId}" }
                ) { request ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + expandVertically()
                    ) {
                        FriendRequestCard(
                            friend = request,
                            onAccept = { onAcceptRequest(request.friendUserId) },
                            onReject = { onRejectRequest(request.friendUserId) }
                        )
                    }
                }
                
                item(key = "divider") { 
                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    ) 
                }
            }
            
            // Lista de amigos
            item(key = "friends_header") {
                SectionHeader(
                    emoji = "👥",
                    title = stringResource(R.string.community_my_friends),
                    count = friends.size,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (friends.isEmpty()) {
                item(key = "empty_friends") {
                    EnhancedEmptyStateCard(
                        emoji = "👥",
                        title = stringResource(R.string.community_no_friends),
                        message = stringResource(R.string.community_no_friends_desc),
                        actionLabel = stringResource(R.string.community_how_to_add),
                        onAction = { /* Show help */ }
                    )
                }
            } else {
                items(
                    items = friends,
                    key = { "friend_${it.friendUserId}" }
                ) { friend ->
                    EnhancedFriendCard(
                        friend = friend,
                        onRemove = { onRemoveFriend(friend.friendUserId) }
                    )
                }
            }
            
            // Espaciado inferior
            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(
    emoji: String,
    title: String,
    count: Int,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(emoji, fontSize = 20.sp)
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = color.copy(alpha = 0.1f)
        ) {
            Text(
                "$count",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EnhancedFriendCard(
    friend: Friend,
    onRemove: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar mejorado
            EnhancedAvatar(
                name = friend.friendName,
                level = friend.friendLevel,
                size = 56
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    friend.friendName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatBadge(
                        emoji = "🔥",
                        value = "${friend.friendStreak}",
                        label = "racha"
                    )
                    StatBadge(
                        emoji = "⭐",
                        value = stringResource(R.string.community_level_short, friend.friendLevel),
                        label = null
                    )
                }
            }
            
            // Stats de esta semana
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    "${friend.friendWeeklyFocusMinutes}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "min/semana",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert, 
                        stringResource(R.string.a11y_more_options_for_friend, friend.friendName),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.community_view_profile)) },
                        onClick = { showMenu = false },
                        leadingIcon = {
                            Icon(Icons.Default.Person, null)
                        }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.community_remove_friend), color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            showDeleteConfirmation = true
                        },
                        leadingIcon = {
                            Icon(Icons.Default.PersonRemove, null, tint = MaterialTheme.colorScheme.error)
                        }
                    )
                }
            }
        }
    }
    
    // Diálogo de confirmación
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.community_remove_friend_title)) },
            text = { Text(stringResource(R.string.community_remove_friend_message, friend.friendName)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        onRemove()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.community_remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.community_cancel))
                }
            }
        )
    }
}

@Composable
private fun EnhancedAvatar(
    name: String,
    level: Int,
    size: Int = 48
) {
    val levelColor = when {
        level >= 50 -> Amber400 // Gold
        level >= 30 -> Violet500
        level >= 20 -> Sky500
        level >= 10 -> Mint500
        else -> MaterialTheme.colorScheme.primary
    }
    
    Box(
        modifier = Modifier.size(size.dp),
        contentAlignment = Alignment.Center
    ) {
        // Border ring basado en nivel
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(
                    width = 3.dp,
                    color = levelColor,
                    shape = CircleShape
                )
                .padding(3.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                name.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        // Level badge
        if (level > 1) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(20.dp),
                shape = CircleShape,
                color = levelColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "$level",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBadge(
    emoji: String,
    value: String,
    label: String?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(emoji, fontSize = 14.sp)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        label?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun FriendRequestCard(
    friend: Friend,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    // Animación de pulso para llamar la atención
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (rememberSystemAnimationsEnabled()) 1.02f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar con animación
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    friend.friendName.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    friend.friendName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("👋", fontSize = 14.sp)
                    Text(
                        stringResource(R.string.community_wants_to_be_friend),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = onReject,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    // Sin etiqueta, un lector de pantalla anunciaba dos veces "botón"
                    // seguidas, sin forma de distinguir aceptar de rechazar en una acción
                    // que añade a un desconocido a la lista de amigos.
                    Icon(
                        Icons.Default.Close,
                        stringResource(R.string.a11y_reject_friend_request),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Button(
                    onClick = onAccept,
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Icon(
                        Icons.Default.Check,
                        stringResource(R.string.a11y_accept_friend_request),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LeaderboardTab(
    weeklyLeaderboard: List<LeaderboardEntry>,
    friendsLeaderboard: List<LeaderboardEntry>,
    myRank: LeaderboardEntry?,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    var showFriendsOnly by remember { mutableStateOf(true) }
    val displayList = if (showFriendsOnly) friendsLeaderboard else weeklyLeaderboard
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Mi posición mejorada
            item(key = "my_rank") {
                MyRankCard(myRank = myRank)
            }
            
            // Toggle mejorado
            item(key = "toggle") {
                LeaderboardToggle(
                    showFriendsOnly = showFriendsOnly,
                    onToggle = { showFriendsOnly = it }
                )
            }
            
            // Leaderboard
            if (displayList.isEmpty()) {
                item(key = "empty_leaderboard") {
                    EnhancedEmptyStateCard(
                        emoji = "🏆",
                        title = if (showFriendsOnly) "Sin amigos en el ranking" else stringResource(R.string.community_leaderboard_empty),
                        message = if (showFriendsOnly) "Agrega amigos para ver su progreso" else stringResource(R.string.community_leaderboard_empty_desc),
                        actionLabel = if (showFriendsOnly) stringResource(R.string.community_add_friends) else null,
                        onAction = null
                    )
                }
            } else {
                // Top 3 destacado
                if (displayList.size >= 3) {
                    item(key = "podium") {
                        PodiumCard(top3 = displayList.take(3))
                    }
                }
                
                // Resto del leaderboard
                val remainingList = if (displayList.size >= 3) displayList.drop(3) else displayList
                items(
                    items = remainingList,
                    key = { it.userId }
                ) { entry ->
                    val position = displayList.indexOf(entry) + 1
                    EnhancedLeaderboardEntryCard(
                        entry = entry,
                        position = position
                    )
                }
            }
            
            // Espaciado inferior
            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun MyRankCard(myRank: LeaderboardEntry?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de ranking
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (myRank != null && myRank.rank > 0) {
                    Text(
                        "#${myRank.rank}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        Icons.Default.Leaderboard,
                        null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.community_your_position),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (myRank != null) {
                    Text(
                        "${myRank.weeklyFocusMinutes} min esta semana",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    
                    // Cambio de posición con animación
                    val change = myRank.previousRank - myRank.rank
                    if (change != 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (change > 0) Mint500.copy(alpha = 0.2f) 
                                       else Rose500.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    text = if (change > 0) "⬆️ +$change posiciones" else "⬇️ $change posiciones",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (change > 0) Mint500 else Rose500
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        stringResource(R.string.community_complete_sessions),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaderboardToggle(
    showFriendsOnly: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ToggleButton(
                selected = showFriendsOnly,
                onClick = { onToggle(true) },
                icon = Icons.Default.People,
                label = stringResource(R.string.community_tab_friends),
                modifier = Modifier.weight(1f)
            )
            ToggleButton(
                selected = !showFriendsOnly,
                onClick = { onToggle(false) },
                icon = Icons.Default.Public,
                label = stringResource(R.string.community_tab_global),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ToggleButton(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primary 
               else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(18.dp),
                tint = if (selected) MaterialTheme.colorScheme.onPrimary 
                      else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onPrimary 
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PodiumCard(top3: List<LeaderboardEntry>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "🏆 Top 3 de la semana",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                // 2do lugar
                if (top3.size >= 2) {
                    PodiumEntry(entry = top3[1], position = 2)
                }
                
                // 1er lugar (más alto)
                if (top3.isNotEmpty()) {
                    PodiumEntry(entry = top3[0], position = 1)
                }
                
                // 3er lugar
                if (top3.size >= 3) {
                    PodiumEntry(entry = top3[2], position = 3)
                }
            }
        }
    }
}

@Composable
private fun PodiumEntry(
    entry: LeaderboardEntry,
    position: Int
) {
    val (emoji, height, color) = when (position) {
        1 -> Triple("🥇", 100.dp, Amber400)
        2 -> Triple("🥈", 80.dp, Silver)
        else -> Triple("🥉", 60.dp, Bronze)
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        // Avatar
        EnhancedAvatar(
            name = entry.userName,
            level = entry.userLevel,
            size = if (position == 1) 64 else 48
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Nombre
        Text(
            entry.userName,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        // Minutos
        Text(
            "${entry.weeklyFocusMinutes} min",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Podio
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(height)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(color),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                emoji,
                fontSize = 24.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun EnhancedLeaderboardEntryCard(
    entry: LeaderboardEntry,
    position: Int
) {
    // El podio se lee por color, no por leer el número: oro/plata/bronce en las tres
    // primeras posiciones y un número atenuado en el resto.
    val podiumColor = when (position) {
        1 -> com.momentummm.app.ui.theme.Amber400
        2 -> com.momentummm.app.ui.theme.Neutral300
        3 -> com.momentummm.app.ui.theme.Coral400
        else -> null
    }
    val isPodium = podiumColor != null

    MomentumCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MomentumDesign.Shapes.cardCompact,
        containerColor = if (isPodium) {
            podiumColor!!.copy(alpha = MomentumDesign.Alpha.subtle)
        } else {
            MaterialTheme.momentum.surface
        },
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isPodium) podiumColor!!.copy(alpha = 0.4f) else MaterialTheme.momentum.border
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MomentumDesign.Spacing.compact),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(30.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isPodium) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(MomentumDesign.Shapes.pill)
                            .background(podiumColor!!)
                    )
                }
                Text(
                    text = position.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isPodium) {
                        com.momentummm.app.ui.theme.Neutral950
                    } else {
                        MaterialTheme.momentum.textTertiary
                    }
                )
            }

            Spacer(modifier = Modifier.width(MomentumDesign.Spacing.compact))

            EnhancedAvatar(
                name = entry.userName,
                level = entry.userLevel,
                size = 44
            )

            Spacer(modifier = Modifier.width(MomentumDesign.Spacing.compact))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.userName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.momentum.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(MomentumDesign.Spacing.extraSmall))
                Row(horizontalArrangement = Arrangement.spacedBy(MomentumDesign.Spacing.small)) {
                    LeaderboardMeta(
                        icon = Icons.Filled.LocalFireDepartment,
                        text = entry.currentStreak.toString(),
                        tint = com.momentummm.app.ui.theme.Coral400,
                    )
                    LeaderboardMeta(
                        icon = Icons.Filled.Star,
                        text = stringResource(R.string.community_level_short, entry.userLevel),
                        tint = com.momentummm.app.ui.theme.Amber400,
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = entry.weeklyFocusMinutes.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "min",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.momentum.textTertiary
                )
            }
        }
    }
}

/** Metadato compacto de una fila del ranking: icono vectorial + valor. */
@Composable
private fun LeaderboardMeta(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: androidx.compose.ui.graphics.Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.momentum.textSecondary
        )
    }
}

// ================== Achievements Tab Mejorado ==================

@Composable
private fun AchievementsTab(
    achievements: List<SharedAchievement>,
    isLoading: Boolean,
    onShare: (SharedAchievement) -> Unit,
    onRefresh: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header con estadísticas
            item(key = "achievements_header") {
                AchievementsHeader(achievementsCount = achievements.size)
            }
            
            if (achievements.isEmpty()) {
                item(key = "empty_achievements") {
                    EnhancedEmptyStateCard(
                        emoji = "🎯",
                        title = stringResource(R.string.community_no_achievements),
                        message = stringResource(R.string.community_no_achievements_desc),
                        actionLabel = stringResource(R.string.community_how_to_earn),
                        onAction = null
                    )
                }
                
                // Mostrar logros posibles
                item(key = "possible_achievements_header") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "🎁 Logros que puedes desbloquear",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                item(key = "possible_achievements") {
                    PossibleAchievementsCard()
                }
            } else {
                // Logros no compartidos primero
                val unshared = achievements.filter { !it.isShared }
                val shared = achievements.filter { it.isShared }
                
                if (unshared.isNotEmpty()) {
                    item(key = "unshared_header") {
                        SectionHeader(
                            emoji = "✨",
                            title = stringResource(R.string.community_new_achievements),
                            count = unshared.size,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    
                    items(
                        items = unshared,
                        key = { "achievement_${it.id}" }
                    ) { achievement ->
                        EnhancedAchievementCard(
                            achievement = achievement,
                            onShare = { onShare(achievement) },
                            isNew = true
                        )
                    }
                }
                
                if (shared.isNotEmpty()) {
                    item(key = "shared_header") {
                        SectionHeader(
                            emoji = "📤",
                            title = stringResource(R.string.community_shared_achievements),
                            count = shared.size,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    
                    items(
                        items = shared,
                        key = { "shared_${it.id}" }
                    ) { achievement ->
                        EnhancedAchievementCard(
                            achievement = achievement,
                            onShare = { onShare(achievement) },
                            isNew = false
                        )
                    }
                }
            }
            
            // Espaciado inferior
            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun AchievementsHeader(achievementsCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🏅", fontSize = 28.sp)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.community_your_achievements),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    "$achievementsCount logros desbloqueados",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun PossibleAchievementsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PossibleAchievementRow("🔥", "Racha de 7 días", stringResource(R.string.community_hint_streak_week))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            PossibleAchievementRow("✨", "Semana perfecta", stringResource(R.string.community_hint_no_limits))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            PossibleAchievementRow("🏆", "Top 3 semanal", stringResource(R.string.community_hint_podium))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            PossibleAchievementRow("☢️", "Modo Nuclear", stringResource(R.string.community_hint_nuclear))
        }
    }
}

@Composable
private fun PossibleAchievementRow(emoji: String, title: String, description: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(emoji, fontSize = 24.sp, modifier = Modifier.width(32.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Outlined.Lock,
            null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun EnhancedAchievementCard(
    achievement: SharedAchievement,
    onShare: () -> Unit,
    isNew: Boolean
) {
    val (emoji, color) = when (achievement.achievementType) {
        AchievementType.STREAK_MILESTONE -> "🔥" to Coral500
        AchievementType.LEVEL_UP -> "⬆️" to Sky400
        AchievementType.PERFECT_WEEK -> "✨" to Amber300
        AchievementType.FOCUS_MILESTONE -> "🎯" to Violet500
        AchievementType.NUCLEAR_COMPLETED -> "☢️" to Coral500
        AchievementType.TOP_LEADERBOARD -> "🏆" to Amber400
        AchievementType.FIRST_WEEK -> "🚀" to Mint500
        AchievementType.CUSTOM -> "🎉" to MaterialTheme.colorScheme.primary
    }
    
    val title = when (achievement.achievementType) {
        AchievementType.STREAK_MILESTONE -> stringResource(R.string.community_ach_streak, achievement.achievementValue)
        AchievementType.LEVEL_UP -> stringResource(R.string.community_ach_level, achievement.achievementValue)
        AchievementType.PERFECT_WEEK -> stringResource(R.string.community_ach_perfect_week)
        AchievementType.FOCUS_MILESTONE -> stringResource(R.string.community_ach_focus_hours, achievement.achievementValue)
        AchievementType.NUCLEAR_COMPLETED -> stringResource(R.string.community_ach_nuclear)
        AchievementType.TOP_LEADERBOARD -> stringResource(R.string.community_ach_top_rank, achievement.achievementValue)
        AchievementType.FIRST_WEEK -> stringResource(R.string.community_ach_first_week)
        AchievementType.CUSTOM -> achievement.message.ifEmpty { stringResource(R.string.community_ach_special) }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isNew) color.copy(alpha = 0.1f) 
                            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isNew) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji con fondo
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 28.sp)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                if (isNew) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = color
                    ) {
                        Text(
                            stringResource(R.string.community_badge_new),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                if (achievement.isShared) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            pluralStringResource(R.plurals.community_shared_times, achievement.shareCount, achievement.shareCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Botón de compartir
            if (isNew) {
                Button(
                    onClick = onShare,
                    colors = ButtonDefaults.buttonColors(containerColor = color)
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.community_share))
                }
            } else {
                IconButton(onClick = onShare) {
                    Icon(
                        Icons.Default.Share,
                        stringResource(R.string.a11y_share_achievement),
                        tint = color
                    )
                }
            }
        }
    }
}

// ================== Empty State y Diálogos Mejorados ==================

@Composable
private fun EnhancedEmptyStateCard(
    emoji: String,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 56.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun AddFriendDialog(
    onDismiss: () -> Unit,
    onAddFriend: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    
    // Validar email
    val isValidEmail = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    
    // El texto se resuelve en composición: stringResource no es válido dentro de
    // LaunchedEffect, que corre en una corrutina.
    val invalidEmailMessage = stringResource(R.string.community_invalid_email)
    LaunchedEffect(email) {
        emailError = when {
            email.isEmpty() -> null
            !isValidEmail -> invalidEmailMessage
            else -> null
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { 
            Icon(
                Icons.Default.PersonAdd, 
                null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { 
            Text(
                stringResource(R.string.community_add_friend_title),
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    stringResource(R.string.community_add_friend_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.community_friend_name_label)) },
                    placeholder = { Text(stringResource(R.string.community_friend_name_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.lowercase().trim() },
                    label = { Text(stringResource(R.string.community_email_label)) },
                    placeholder = { Text("amigo@ejemplo.com") },
                    leadingIcon = { Icon(Icons.Default.Email, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = emailError != null,
                    supportingText = emailError?.let { { Text(it) } },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAddFriend(email, name) },
                enabled = email.isNotBlank() && name.isNotBlank() && isValidEmail
            ) {
                Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.community_send_request))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.community_cancel))
            }
        }
    )
}

// ================== Settings Sheet ==================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommunitySettingsSheet(
    onDismiss: () -> Unit,
    viewModel: CommunityViewModel
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Settings,
                    null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    stringResource(R.string.community_settings_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Divider()
            
            // Privacidad
            Text(
                "🔒 Privacidad",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            settings?.let { s ->
                SettingsSwitch(
                    title = stringResource(R.string.community_setting_global_rank),
                    subtitle = stringResource(R.string.community_setting_global_rank_desc),
                    checked = s.showInGlobalLeaderboard,
                    onCheckedChange = { viewModel.updateShowInGlobalLeaderboard(it) }
                )
                
                SettingsSwitch(
                    title = stringResource(R.string.community_setting_show_streak),
                    subtitle = stringResource(R.string.community_setting_show_streak_desc),
                    checked = s.showStreakToFriends,
                    onCheckedChange = { viewModel.updateShowStreakToFriends(it) }
                )
                
                SettingsSwitch(
                    title = stringResource(R.string.community_setting_show_focus),
                    subtitle = stringResource(R.string.community_setting_show_focus_desc),
                    checked = s.showFocusTimeToFriends,
                    onCheckedChange = { viewModel.updateShowFocusTimeToFriends(it) }
                )
                
                Divider()
                
                // Notificaciones
                Text(
                    "🔔 Notificaciones sociales",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                SettingsSwitch(
                    title = stringResource(R.string.community_setting_requests),
                    subtitle = stringResource(R.string.community_setting_requests_desc),
                    checked = s.notifyFriendRequests,
                    onCheckedChange = { viewModel.updateNotifyFriendRequests(it) }
                )
                
                SettingsSwitch(
                    title = stringResource(R.string.community_setting_friend_ach),
                    subtitle = stringResource(R.string.community_setting_friend_ach_desc),
                    checked = s.notifyFriendAchievements,
                    onCheckedChange = { viewModel.updateNotifyFriendAchievements(it) }
                )
                
                SettingsSwitch(
                    title = stringResource(R.string.community_setting_rank_changes),
                    subtitle = stringResource(R.string.community_setting_rank_changes_desc),
                    checked = s.notifyLeaderboardChanges,
                    onCheckedChange = { viewModel.updateNotifyLeaderboardChanges(it) }
                )
            } ?: run {
                // Loading state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
