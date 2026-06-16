package com.example.myapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapp.data.Friend
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    viewModel: TaskViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit
) {
    val myUsername by viewModel.myUsername.collectAsState()
    val friends by viewModel.friends.collectAsState()
    val incomingRequests by viewModel.incomingRequests.collectAsState()
    val sentRequests by viewModel.sentRequests.collectAsState()
    val discoverableUsers by viewModel.discoverableUsers.collectAsState()
    val isRefreshingDiscoverable by viewModel.isRefreshingDiscoverable.collectAsState()
    val isRegistering by viewModel.isRegisteringUsername.collectAsState()

    // Trigger sync when screen opens
    LaunchedEffect(myUsername) {
        if (myUsername.isNotEmpty()) {
            viewModel.syncWithServer()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Friends", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (myUsername.isNotEmpty()) {
                        IconButton(onClick = { viewModel.syncWithServer() }) {
                            Icon(imageVector = Icons.Rounded.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (myUsername.isEmpty()) {
                RegistrationView(
                    isRegistering = isRegistering,
                    onRegister = { name, onResult -> viewModel.registerUsername(name, onResult) }
                )
            } else {
                FriendsListView(
                    viewModel = viewModel,
                    myUsername = myUsername,
                    friends = friends,
                    incomingRequests = incomingRequests,
                    sentRequests = sentRequests,
                    discoverableUsers = discoverableUsers,
                    isRefreshingDiscoverable = isRefreshingDiscoverable,
                    onSendRequest = { name, onResult -> viewModel.sendFriendRequest(name, onResult) },
                    onAcceptRequest = { name, onResult -> viewModel.acceptFriendRequest(name, onResult) },
                    onDeclineRequest = { name, onResult -> viewModel.declineFriendRequest(name, onResult) },
                    onDeleteFriend = { viewModel.removeFriend(it) },
                    onNavigateToChat = onNavigateToChat
                )
            }
        }
    }
}

@Composable
fun RegistrationView(
    isRegistering: Boolean,
    onRegister: (String, (Boolean, String?) -> Unit) -> Unit
) {
    var inputUsername by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.AccountCircle,
            contentDescription = "User Account",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Choose a Unique Username",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Register a username to chat and share files with other users globally.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = inputUsername,
            onValueChange = {
                inputUsername = it
                errorMessage = null
            },
            label = { Text("Username") },
            placeholder = { Text("e.g. alex_123") },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            enabled = !isRegistering,
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (inputUsername.trim().isEmpty()) {
                    errorMessage = "Username cannot be empty"
                } else {
                    onRegister(inputUsername.trim()) { success, err ->
                        if (!success) {
                            errorMessage = err ?: "Registration failed"
                        }
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            enabled = !isRegistering && inputUsername.trim().isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isRegistering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Register")
            }
        }
    }
}

@Composable
fun FriendsListView(
    viewModel: TaskViewModel,
    myUsername: String,
    friends: List<Friend>,
    incomingRequests: List<String>,
    sentRequests: List<String>,
    discoverableUsers: List<String>,
    isRefreshingDiscoverable: Boolean,
    onSendRequest: (String, (Boolean, String?) -> Unit) -> Unit,
    onAcceptRequest: (String, (Boolean, String?) -> Unit) -> Unit,
    onDeclineRequest: (String, (Boolean, String?) -> Unit) -> Unit,
    onDeleteFriend: (Friend) -> Unit,
    onNavigateToChat: (String) -> Unit
) {
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.updateProfilePicture(context, it)
        }
    }

    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Friends", "Requests", "Discover")

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // User Profile Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(
                    username = myUsername,
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    viewModel = viewModel,
                    fontSize = 16.sp,
                    fallbackBrush = Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "My Profile",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "@$myUsername",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabTitles.forEachIndexed { index, title ->
                val badgeCount = when (index) {
                    1 -> incomingRequests.size
                    else -> 0
                }
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(title, fontWeight = FontWeight.Bold)
                            if (badgeCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Text(badgeCount.toString(), color = Color.White)
                                }
                            }
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // Friends Tab
                    if (friends.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Rounded.PeopleOutline,
                                    contentDescription = "No Friends",
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No friends added yet",
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Go to Discover to add people!",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(friends, key = { it.username }) { friend ->
                                FriendRow(
                                    friend = friend,
                                    onClick = { onNavigateToChat(friend.username) },
                                    onDelete = { onDeleteFriend(friend) },
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Requests Tab (Incoming & Sent)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            Text(
                                text = "Incoming Requests (${incomingRequests.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (incomingRequests.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No pending incoming requests",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        } else {
                            items(incomingRequests) { username ->
                                RequestRow(
                                    username = username,
                                    isIncoming = true,
                                    onAccept = { onAcceptRequest(username) { _, _ -> } },
                                    onDecline = { onDeclineRequest(username) { _, _ -> } },
                                    viewModel = viewModel
                                )
                            }
                        }

                        item {
                            Text(
                                text = "Sent Requests (${sentRequests.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                        if (sentRequests.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No pending sent requests",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        } else {
                            items(sentRequests) { username ->
                                RequestRow(
                                    username = username,
                                    isIncoming = false,
                                    onAccept = {},
                                    onDecline = {},
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // Discover Tab
                    var filterQuery by remember { mutableStateOf("") }
                    val filteredUsers = remember(discoverableUsers, filterQuery) {
                        discoverableUsers.filter {
                            it.contains(filterQuery, ignoreCase = true)
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        OutlinedTextField(
                            value = filterQuery,
                            onValueChange = { filterQuery = it },
                            placeholder = { Text("Search users...") },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        )

                        if (isRefreshingDiscoverable) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else if (filteredUsers.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No users found",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                items(filteredUsers) { username ->
                                    val isFriend = friends.any { it.username.equals(username, ignoreCase = true) }
                                    val isSent = sentRequests.contains(username)
                                    val isIncoming = incomingRequests.contains(username)

                                    DiscoverUserRow(
                                        username = username,
                                        isFriend = isFriend,
                                        isSent = isSent,
                                        isIncoming = isIncoming,
                                        onSendRequest = { onSendRequest(username) { _, _ -> } },
                                        onAcceptRequest = { onAcceptRequest(username) { _, _ -> } },
                                        onDeclineRequest = { onDeclineRequest(username) { _, _ -> } },
                                        onChat = { onNavigateToChat(username) },
                                        viewModel = viewModel
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FriendRow(
    friend: Friend,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    viewModel: TaskViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Initial Avatar
                val colorHex = friend.avatarColorHex
                val color = remember(colorHex) {
                    try {
                        Color(android.graphics.Color.parseColor(colorHex))
                    } catch (e: Exception) {
                        Color(0xFF3F51B5)
                    }
                }
                UserAvatar(
                    username = friend.username,
                    modifier = Modifier.size(46.dp),
                    viewModel = viewModel,
                    fontSize = 16.sp,
                    fallbackColor = color
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = friend.username,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (friend.status == "Online") Color(0xFF4CAF50)
                                    else if (friend.status == "Busy") Color(0xFFFF9800)
                                    else Color.Gray
                                )
                        )
                        Text(
                            text = friend.status,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Remove Friend",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun RequestRow(
    username: String,
    isIncoming: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    viewModel: TaskViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(
                    username = username,
                    modifier = Modifier.size(40.dp),
                    viewModel = viewModel,
                    fontSize = 14.sp,
                    textColor = MaterialTheme.colorScheme.secondary,
                    fallbackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = username,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (isIncoming) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onAccept,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Accept",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDecline,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Decline",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                Text(
                    text = "Pending",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}

@Composable
fun DiscoverUserRow(
    username: String,
    isFriend: Boolean,
    isSent: Boolean,
    isIncoming: Boolean,
    onSendRequest: () -> Unit,
    onAcceptRequest: () -> Unit,
    onDeclineRequest: () -> Unit,
    onChat: () -> Unit,
    viewModel: TaskViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(
                    username = username,
                    modifier = Modifier.size(40.dp),
                    viewModel = viewModel,
                    fontSize = 14.sp,
                    textColor = MaterialTheme.colorScheme.secondary,
                    fallbackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = username,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            when {
                isFriend -> {
                    IconButton(onClick = onChat) {
                        Icon(
                            imageVector = Icons.Rounded.Chat,
                            contentDescription = "Chat",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                isSent -> {
                    Text(
                        text = "Requested",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }
                isIncoming -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onAcceptRequest) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Accept",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onDeclineRequest) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Decline",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                else -> {
                    IconButton(onClick = onSendRequest) {
                        Icon(
                            imageVector = Icons.Rounded.PersonAdd,
                            contentDescription = "Send Friend Request",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
