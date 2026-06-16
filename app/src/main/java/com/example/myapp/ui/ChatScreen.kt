package com.example.myapp.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.myapp.data.ChatMessage
import com.example.myapp.data.FileAttachment
import com.example.myapp.data.MusicTrack
import com.example.myapp.data.DownloadedNewsArticle
import java.io.File
import kotlinx.coroutines.delay
import android.content.Intent
import androidx.core.content.FileProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    friendUsername: String,
    viewModel: TaskViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToWebView: (String, String) -> Unit
) {
    val context = LocalContext.current
    val chatMessages by viewModel.chatMessages.collectAsState()
    val allMusicTracks by viewModel.allMusicTracks.collectAsState()
    val downloadedArticles by viewModel.downloadedNews.collectAsState()

    DisposableEffect(friendUsername) {
        viewModel.activeChatFriend.value = friendUsername.lowercase()
        onDispose {
            viewModel.activeChatFriend.value = null
        }
    }
    
    // Filter messages for this specific friend
    val friendMessages = remember(chatMessages, friendUsername) {
        chatMessages.filter {
            it.friendUsername.equals(friendUsername, ignoreCase = true)
        }.sortedBy { it.timestampMs }
    }

    val selectedMessageIds = remember { mutableStateListOf<String>() }

    var messageText by remember { mutableStateOf("") }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showMusicSelector by remember { mutableStateOf(false) }
    var showArticleSelector by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Scroll to bottom when new messages arrive and mark them as read
    LaunchedEffect(friendMessages.size, friendUsername) {
        if (friendMessages.isNotEmpty()) {
            listState.animateScrollToItem(friendMessages.size - 1)
        }
        viewModel.markMessagesAsRead(friendUsername)
    }

    // Generic file picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val file = copyUriToLocalFile(context, uri)
            if (file != null) {
                val mimeType = context.contentResolver.getType(uri) ?: "generic"
                val fileType = when {
                    mimeType.startsWith("image/") -> "image"
                    mimeType.startsWith("audio/") -> "audio"
                    else -> "generic"
                }
                viewModel.uploadAndSendFile(context, friendUsername, file.absolutePath, fileType)
            } else {
                Toast.makeText(context, "Failed to read file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val isInSelectionMode = selectedMessageIds.isNotEmpty()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    if (isInSelectionMode) {
                        Text("${selectedMessageIds.size} Selected", fontWeight = FontWeight.Bold)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UserAvatar(
                                username = friendUsername,
                                modifier = Modifier.size(36.dp),
                                viewModel = viewModel,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(friendUsername, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4CAF50))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Online", fontSize = 11.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isInSelectionMode) {
                                selectedMessageIds.clear()
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isInSelectionMode) Icons.Rounded.Close else Icons.Default.ArrowBack,
                            contentDescription = if (isInSelectionMode) "Clear selection" else "Back"
                        )
                    }
                },
                actions = {
                    if (isInSelectionMode) {
                        IconButton(
                            onClick = {
                                viewModel.deleteChatMessages(selectedMessageIds.toSet())
                                selectedMessageIds.clear()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Delete selected messages",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        var showMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = "Options"
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Clear Chat") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.clearChatMessages(friendUsername)
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteSweep,
                                            contentDescription = "Clear Chat"
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            // Messages Area
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(friendMessages, key = { it.id }) { message ->
                    val isMe = message.sender.equals("me", ignoreCase = true)
                    MessageBubble(
                        message = message,
                        isMe = isMe,
                        isSelected = selectedMessageIds.contains(message.id),
                        isInSelectionMode = isInSelectionMode,
                        onToggleSelect = {
                            if (selectedMessageIds.contains(message.id)) {
                                selectedMessageIds.remove(message.id)
                            } else {
                                selectedMessageIds.add(message.id)
                            }
                        },
                        onPlayAudio = { attachment ->
                            // Play in background music player
                            viewModel.musicPlayerManager.play(
                                MusicTrack(
                                    id = attachment.fileName,
                                    filePath = attachment.filePath,
                                    title = attachment.fileName,
                                    artist = "Shared by ${message.sender}",
                                    durationMs = 0L
                                )
                            )
                            Toast.makeText(context, "Playing ${attachment.fileName}", Toast.LENGTH_SHORT).show()
                        },
                        onStopAudio = {
                            viewModel.musicPlayerManager.pause()
                        },
                        onOpenArticle = { attachment ->
                            // Navigate to local WebView reader screen
                            val fileUrl = "file://${attachment.filePath}"
                            onNavigateToWebView(fileUrl, attachment.fileName)
                        },
                        onOpenFile = { attachment ->
                            openSharedFile(context, attachment)
                        },
                        onDelete = {
                            viewModel.deleteChatMessage(message.id)
                        },
                        onAddTrackToLibrary = { attachment ->
                            viewModel.importReceivedAudioTrack(context, attachment) { success ->
                                if (success) {
                                    Toast.makeText(context, "Added to Music Library", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to add track", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onAddArticleToLibrary = { attachment ->
                            viewModel.importReceivedNewsArticle(context, attachment, message.sender) { success ->
                                if (success) {
                                    Toast.makeText(context, "Added to Downloaded Articles", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to import article", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }

            val typingFriends by viewModel.typingFriends.collectAsState()
            var isFriendTyping by remember { mutableStateOf(false) }
            LaunchedEffect(typingFriends, friendUsername) {
                val lastTime = typingFriends[friendUsername.lowercase()] ?: 0L
                val elapsed = System.currentTimeMillis() - lastTime
                if (elapsed < 4000) {
                    isFriendTyping = true
                    delay(4000 - elapsed)
                    isFriendTyping = false
                } else {
                    isFriendTyping = false
                }
            }

            if (isFriendTyping) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$friendUsername is typing...",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Input Area
            Surface(
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { showAttachmentSheet = true },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Attachments",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedTextField(
                        value = messageText,
                        onValueChange = {
                            messageText = it
                            viewModel.sendTypingIndicator(friendUsername)
                        },
                        placeholder = { Text("Message...") },
                        shape = RoundedCornerShape(24.dp),
                        singleLine = false,
                        maxLines = 4,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )

                    IconButton(
                        onClick = {
                            if (messageText.trim().isNotEmpty()) {
                                viewModel.sendChatMessage(friendUsername, messageText.trim())
                                messageText = ""
                            }
                        },
                        enabled = messageText.trim().isNotEmpty(),
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (messageText.trim().isNotEmpty()) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Send,
                            contentDescription = "Send",
                            tint = if (messageText.trim().isNotEmpty()) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }

    // Attachment Dialog Sheet
    if (showAttachmentSheet) {
        AlertDialog(
            onDismissRequest = { showAttachmentSheet = false },
            title = { Text("Share Attachment", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Share Music
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAttachmentSheet = false
                                showMusicSelector = true
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.MusicNote, contentDescription = "Music", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Share Music Track")
                    }

                    // Share News Article
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAttachmentSheet = false
                                showArticleSelector = true
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Article, contentDescription = "Article", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Share Offline News Article")
                    }


                }
            },
            confirmButton = {
                TextButton(onClick = { showAttachmentSheet = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Music Selector Dialog
    if (showMusicSelector) {
        AlertDialog(
            onDismissRequest = { showMusicSelector = false },
            title = { Text("Select Music Track", fontWeight = FontWeight.Bold) },
            text = {
                if (allMusicTracks.isEmpty()) {
                    Text("No music tracks available.", modifier = Modifier.padding(vertical = 12.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allMusicTracks) { track ->
                            Card(
                                onClick = {
                                    showMusicSelector = false
                                    viewModel.uploadAndSendFile(context, friendUsername, track.filePath, "audio")
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(track.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(track.artist, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMusicSelector = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // News Article Selector Dialog
    if (showArticleSelector) {
        AlertDialog(
            onDismissRequest = { showArticleSelector = false },
            title = { Text("Select News Article", fontWeight = FontWeight.Bold) },
            text = {
                if (downloadedArticles.isEmpty()) {
                    Text("No downloaded articles available.", modifier = Modifier.padding(vertical = 12.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(downloadedArticles) { article ->
                            Card(
                                onClick = {
                                    showArticleSelector = false
                                    viewModel.uploadAndSendFile(context, friendUsername, article.localFilePath, "news_article")
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(article.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(article.source, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showArticleSelector = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    isMe: Boolean,
    isSelected: Boolean,
    isInSelectionMode: Boolean,
    onToggleSelect: () -> Unit,
    onPlayAudio: (FileAttachment) -> Unit,
    onStopAudio: () -> Unit,
    onOpenArticle: (FileAttachment) -> Unit,
    onOpenFile: (FileAttachment) -> Unit,
    onDelete: () -> Unit,
    onAddTrackToLibrary: ((FileAttachment) -> Unit)? = null,
    onAddArticleToLibrary: ((FileAttachment) -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    val rowBackground = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackground)
            .combinedClickable(
                onLongClick = {
                    onToggleSelect()
                },
                onClick = {
                    if (isInSelectionMode) {
                        onToggleSelect()
                    }
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMe) 16.dp else 4.dp,
                            bottomEnd = if (isMe) 4.dp else 16.dp
                        )
                    )
                    .background(
                        if (isMe) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .combinedClickable(
                        onLongClick = {
                            if (isInSelectionMode) {
                                onToggleSelect()
                            } else {
                                showMenu = true
                            }
                        },
                        onClick = {
                            if (isInSelectionMode) {
                                onToggleSelect()
                            }
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .widthIn(max = 280.dp)
            ) {
                Column {
                    if (message.fileAttachment != null) {
                        AttachmentCard(
                            attachment = message.fileAttachment,
                            isMe = isMe,
                            onPlayAudio = onPlayAudio,
                            onStopAudio = onStopAudio,
                            onOpenArticle = onOpenArticle,
                            onOpenFile = onOpenFile,
                            onAddTrackToLibrary = onAddTrackToLibrary,
                            onAddArticleToLibrary = onAddArticleToLibrary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = message.text,
                        color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.align(Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val tickIcon = when (message.status) {
                                "read" -> Icons.Rounded.DoneAll
                                "delivered" -> Icons.Rounded.DoneAll
                                "going" -> Icons.Rounded.Schedule
                                else -> Icons.Rounded.Done
                            }
                            val tickColor = when (message.status) {
                                "read" -> Color(0xFF00B0FF) // Blue Tick
                                "going" -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f)
                                else -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f) // Gray Tick
                            }
                            Icon(
                                imageVector = tickIcon,
                                contentDescription = message.status,
                                modifier = Modifier.size(15.dp),
                                tint = tickColor
                            )
                        }
                    }
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    showMenu = false
                    onDelete()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete Message"
                    )
                }
            )
        }
    }
}

@Composable
fun AttachmentCard(
    attachment: FileAttachment,
    isMe: Boolean,
    onPlayAudio: (FileAttachment) -> Unit,
    onStopAudio: () -> Unit,
    onOpenArticle: (FileAttachment) -> Unit,
    onOpenFile: (FileAttachment) -> Unit,
    onAddTrackToLibrary: ((FileAttachment) -> Unit)? = null,
    onAddArticleToLibrary: ((FileAttachment) -> Unit)? = null
) {
    val containerColor = if (isMe) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
    else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    val textColor = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        when (attachment.fileType) {
            "image" -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clickable { onOpenFile(attachment) }
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = if (attachment.filePath.isNotEmpty()) File(attachment.filePath) else attachment.downloadUrl
                        ),
                        contentDescription = "Shared Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            "audio" -> {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = "Audio file",
                        tint = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = attachment.fileName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Music Track",
                            fontSize = 11.sp,
                            color = if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (attachment.filePath.isNotEmpty()) {
                        IconButton(onClick = { onPlayAudio(attachment) }) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Play",
                                tint = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                            )
                        }
                        if (onAddTrackToLibrary != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = { onAddTrackToLibrary(attachment) }) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Add to Music Library",
                                    tint = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
            }
            "news_article" -> {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Article,
                        contentDescription = "Article file",
                        tint = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = attachment.fileName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Offline Article",
                            fontSize = 11.sp,
                            color = if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (attachment.filePath.isNotEmpty()) {
                        if (!isMe && onAddArticleToLibrary != null) {
                            IconButton(onClick = { onAddArticleToLibrary(attachment) }) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Add to offline articles",
                                    tint = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        IconButton(onClick = { onOpenArticle(attachment) }) {
                            Icon(
                                imageVector = Icons.Rounded.OpenInNew,
                                contentDescription = "Open",
                                tint = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
            }
            else -> {
                // Generic file
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.InsertDriveFile,
                        contentDescription = "File",
                        tint = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = attachment.fileName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Shared File",
                            fontSize = 11.sp,
                            color = if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (attachment.filePath.isNotEmpty()) {
                        IconButton(onClick = { onOpenFile(attachment) }) {
                            Icon(
                                imageVector = Icons.Rounded.OpenInNew,
                                contentDescription = "Open",
                                tint = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
            }
        }
    }
}

private fun copyUriToLocalFile(context: Context, uri: Uri): File? {
    return try {
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(uri, null, null, null, null)
        var displayName = "file_${System.currentTimeMillis()}"
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    val name = it.getString(index)
                    if (!name.isNullOrEmpty()) displayName = name
                }
            }
        }
        
        val targetDir = File(context.filesDir, "shared_files")
        if (!targetDir.exists()) targetDir.mkdirs()
        
        val destFile = File(targetDir, "${System.currentTimeMillis()}_$displayName")
        contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        destFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun openSharedFile(context: Context, attachment: FileAttachment) {
    if (attachment.filePath.isEmpty()) {
        Toast.makeText(context, "File is not downloaded yet", Toast.LENGTH_SHORT).show()
        return
    }
    val file = File(attachment.filePath)
    if (!file.exists()) {
        Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show()
        return
    }
    
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "com.example.myapp.fileprovider",
            file
        )
        val mimeType = context.contentResolver.getType(uri) ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open file with"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "No application found to open this file type", Toast.LENGTH_SHORT).show()
    }
}
