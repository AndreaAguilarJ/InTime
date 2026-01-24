package com.momentummm.app.ui.screen.community

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.momentummm.app.data.entity.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    isPremiumUser: Boolean = false,
    onUpgradeClick: () -> Unit = {},
    viewModel: CommunityViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    
    val friends by viewModel.friends.collectAsState()
    val pendingRequests by viewModel.pendingRequests.collectAsState()
    val weeklyLeaderboard by viewModel.weeklyLeaderboard.collectAsState()
    val friendsLeaderboard by viewModel.friendsLeaderboard.collectAsState()
    val myRank by viewModel.myRank.collectAsState()
    
    var showAddFriendDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "👥 Comunidad",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                actions = {
                    if (selectedTab == 0) {
                        IconButton(onClick = { showAddFriendDialog = true }) {
                            Icon(Icons.Default.PersonAdd, "Agregar amigo")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Amigos") },
                    icon = { Icon(Icons.Default.People, null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Ranking") },
                    icon = { Icon(Icons.Default.Leaderboard, null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Logros") },
                    icon = { Icon(Icons.Default.EmojiEvents, null) }
                )
            }
            
            when (selectedTab) {
                0 -> FriendsTab(
                    friends = friends,
                    pendingRequests = pendingRequests,
                    onAcceptRequest = { viewModel.acceptFriendRequest(it) },
                    onRejectRequest = { viewModel.rejectFriendRequest(it) },
                    onRemoveFriend = { viewModel.removeFriend(it) }
                )
                1 -> LeaderboardTab(
                    weeklyLeaderboard = weeklyLeaderboard,
                    friendsLeaderboard = friendsLeaderboard,
                    myRank = myRank,
                    onRefresh = { viewModel.refreshLeaderboard() }
                )
                2 -> AchievementsTab(
                    viewModel = viewModel
                )
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
}

@Composable
private fun FriendsTab(
    friends: List<Friend>,
    pendingRequests: List<Friend>,
    onAcceptRequest: (String) -> Unit,
    onRejectRequest: (String) -> Unit,
    onRemoveFriend: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Solicitudes pendientes
        if (pendingRequests.isNotEmpty()) {
            item {
                Text(
                    "Solicitudes pendientes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            pendingRequests.forEach { request ->
                item {
                    FriendRequestCard(
                        friend = request,
                        onAccept = { onAcceptRequest(request.friendUserId) },
                        onReject = { onRejectRequest(request.friendUserId) }
                    )
                }
            }
            
            item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }
        }
        
        // Lista de amigos
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Mis amigos (${friends.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        if (friends.isEmpty()) {
            item {
                EmptyStateCard(
                    emoji = "👥",
                    title = "Sin amigos todavía",
                    message = "Agrega amigos para competir y motivarse mutuamente"
                )
            }
        } else {
            friends.forEach { friend ->
                item {
                    FriendCard(
                        friend = friend,
                        onRemove = { onRemoveFriend(friend.friendUserId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendCard(
    friend: Friend,
    onRemove: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    friend.friendName.first().uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    friend.friendName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "🔥 ${friend.friendStreak} días",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "⭐ Nivel ${friend.friendLevel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Stats de esta semana
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${friend.friendWeeklyFocusMinutes}m",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "esta semana",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, null)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Eliminar amigo") },
                        onClick = {
                            showMenu = false
                            onRemove()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.PersonRemove, null, tint = MaterialTheme.colorScheme.error)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendRequestCard(
    friend: Friend,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    friend.friendName.first().uppercase(),
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
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Quiere ser tu amigo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onReject) {
                    Text("❌")
                }
                Button(onClick = onAccept) {
                    Text("✅")
                }
            }
        }
    }
}

@Composable
private fun LeaderboardTab(
    weeklyLeaderboard: List<LeaderboardEntry>,
    friendsLeaderboard: List<LeaderboardEntry>,
    myRank: LeaderboardEntry?,
    onRefresh: () -> Unit
) {
    var showFriendsOnly by remember { mutableStateOf(true) }
    val displayList = if (showFriendsOnly) friendsLeaderboard else weeklyLeaderboard
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Mi posición
        item {
            myRank?.let { rank ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "#${rank.rank}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Tu posición",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "${rank.weeklyFocusMinutes} min esta semana",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        
                        // Cambio de posición
                        val change = rank.previousRank - rank.rank
                        if (change != 0) {
                            Text(
                                if (change > 0) "⬆️ +$change" else "⬇️ $change",
                                color = if (change > 0) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                        }
                    }
                }
            }
        }
        
        // Toggle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = showFriendsOnly,
                        onClick = { showFriendsOnly = true },
                        label = { Text("Amigos") }
                    )
                    FilterChip(
                        selected = !showFriendsOnly,
                        onClick = { showFriendsOnly = false },
                        label = { Text("Global") }
                    )
                }
                
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, "Actualizar")
                }
            }
        }
        
        // Leaderboard
        if (displayList.isEmpty()) {
            item {
                EmptyStateCard(
                    emoji = "🏆",
                    title = if (showFriendsOnly) "Sin amigos en el ranking" else "Leaderboard vacío",
                    message = if (showFriendsOnly) "Agrega amigos para ver su progreso" else "Sé el primero en aparecer"
                )
            }
        } else {
            itemsIndexed(displayList) { index, entry ->
                LeaderboardEntryCard(
                    entry = entry,
                    position = index + 1
                )
            }
        }
    }
}

@Composable
private fun LeaderboardEntryCard(
    entry: LeaderboardEntry,
    position: Int
) {
    val medalEmoji = when (position) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> "#$position"
    }
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Posición
            Box(
                modifier = Modifier.width(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    medalEmoji,
                    fontSize = if (position <= 3) 24.sp else 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    entry.userName.first().uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.userName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "🔥 ${entry.currentStreak}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "⭐ Lv.${entry.userLevel}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            // Minutos
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${entry.weeklyFocusMinutes}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AchievementsTab(
    viewModel: CommunityViewModel
) {
    val achievements by viewModel.achievements.collectAsState()
    val context = LocalContext.current
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Tus logros para compartir",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        if (achievements.isEmpty()) {
            item {
                EmptyStateCard(
                    emoji = "🎯",
                    title = "Sin logros todavía",
                    message = "Completa objetivos para desbloquear logros compartibles"
                )
            }
        } else {
            achievements.forEach { achievement ->
                item {
                    AchievementCard(
                        achievement = achievement,
                        onShare = { viewModel.shareAchievement(context, achievement) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementCard(
    achievement: SharedAchievement,
    onShare: () -> Unit
) {
    val emoji = when (achievement.achievementType) {
        AchievementType.STREAK_MILESTONE -> "🔥"
        AchievementType.LEVEL_UP -> "⬆️"
        AchievementType.PERFECT_WEEK -> "✨"
        AchievementType.FOCUS_MILESTONE -> "🎯"
        AchievementType.NUCLEAR_COMPLETED -> "☢️"
        AchievementType.TOP_LEADERBOARD -> "🏆"
        AchievementType.FIRST_WEEK -> "🚀"
        AchievementType.CUSTOM -> "🎉"
    }
    
    val title = when (achievement.achievementType) {
        AchievementType.STREAK_MILESTONE -> "Racha de ${achievement.achievementValue} días"
        AchievementType.LEVEL_UP -> "Nivel ${achievement.achievementValue}"
        AchievementType.PERFECT_WEEK -> "Semana perfecta"
        AchievementType.FOCUS_MILESTONE -> "${achievement.achievementValue}h de foco"
        AchievementType.NUCLEAR_COMPLETED -> "Modo Nuclear completado"
        AchievementType.TOP_LEADERBOARD -> "Top ${achievement.achievementValue}"
        AchievementType.FIRST_WEEK -> "Primera semana"
        AchievementType.CUSTOM -> "Logro personalizado"
    }
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 32.sp)
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                if (achievement.isShared) {
                    Text(
                        "Compartido ${achievement.shareCount} veces",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Button(onClick = onShare) {
                Icon(Icons.Default.Share, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Compartir")
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    emoji: String,
    title: String,
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            Text(emoji, fontSize = 48.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
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
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar amigo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAddFriend(email, name) },
                enabled = email.isNotBlank() && name.isNotBlank()
            ) {
                Text("Enviar solicitud")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
