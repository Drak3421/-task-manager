package com.example.myapp.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.myapp.data.YouTubeChannel
import com.example.myapp.data.YouTubeChannelSearchInfo
import com.example.myapp.data.YouTubeVideo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoutubeUpdatesScreen(
    viewModel: TaskViewModel,
    onNavigateToWebView: (String, String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val videos by viewModel.filteredVideos.collectAsState()
    val channels by viewModel.youtubeChannels.collectAsState()
    val selectedChannelId by viewModel.selectedChannelId.collectAsState()
    val timeFilter by viewModel.timeFilter.collectAsState()
    val typeFilter by viewModel.typeFilter.collectAsState()
    val isRefreshing by viewModel.isNewsRefreshing.collectAsState()
    val searchedVideos by viewModel.searchedVideos.collectAsState()
    val isVideoSearching by viewModel.isVideoSearching.collectAsState()

    var showManageDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("YouTube Updates", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        IconButton(onClick = { viewModel.refreshYouTubeFeeds() }) {
                            Icon(imageVector = Icons.Rounded.Refresh, contentDescription = "Refresh")
                        }
                    }
                    IconButton(onClick = { showManageDialog = true }) {
                        Icon(imageVector = Icons.Rounded.Subscriptions, contentDescription = "Manage Channels")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Horizontal scrollable tabs for creators & Recent
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedChannelId == null,
                        onClick = { viewModel.selectChannel(null) },
                        label = { Text("All Feed") },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = selectedChannelId == "recent_history",
                        onClick = { viewModel.selectChannel("recent_history") },
                        label = { Text("Recent") },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
                items(channels, key = { it.id }) { channel ->
                    FilterChip(
                        selected = selectedChannelId == channel.id,
                        onClick = { viewModel.selectChannel(channel.id) },
                        label = { Text(channel.name) },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = false,
                        onClick = { showManageDialog = true },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "Manage",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Manage")
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            // Only show sorting options if we are NOT on the Recent history tab
            if (selectedChannelId != "recent_history") {
                // Horizontal select chips for time ranges
                val timeOptions = listOf(
                    "all" to "All Videos",
                    "latest" to "Latest Video",
                    "1_week" to "Last 1 Week",
                    "1_month" to "Last 1 Month"
                )
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(timeOptions) { option ->
                        FilterChip(
                            selected = timeFilter == option.first,
                            onClick = { viewModel.setTimeFilter(option.first) },
                            label = { Text(option.second) },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }

                // Horizontal select chips for upload types (Shorts / Videos)
                val typeOptions = listOf(
                    "all" to "All Uploads",
                    "videos" to "Videos Only",
                    "shorts" to "Shorts Only"
                )
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(typeOptions) { option ->
                        FilterChip(
                            selected = typeFilter == option.first,
                            onClick = { viewModel.setTypeFilter(option.first) },
                            label = { Text(option.second) },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // News Feed
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (videos.isEmpty() && !isRefreshing) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Feed,
                            contentDescription = "No Feed",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedChannelId == "recent_history") "No Recent Videos" else "No Video Updates",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (selectedChannelId == "recent_history") "Videos you watch will show up here." else "Subscribe to your favorite YouTube channels to get their latest video updates right here.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (selectedChannelId != "recent_history") {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showManageDialog = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Add YouTube Channels")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(videos, key = { it.videoId }) { video ->
                             VideoUpdateCard(video = video, onClick = {
                                     // Save to history list
                                     viewModel.openYouTubeVideo(video)
                                     // Force YouTube app Watch page
                                     try {
                                         val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${video.videoId}")).apply {
                                             setPackage("com.google.android.youtube")
                                             addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                         }
                                         context.startActivity(intent)
                                     } catch (e: Exception) {
                                         val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${video.videoId}")).apply {
                                             addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                         }
                                         context.startActivity(webIntent)
                                     }
                             })
                        }
                    }
                }
            }
        }
    }

    if (showManageDialog) {
        ManageChannelsDialog(
            channels = channels,
            searchedVideos = searchedVideos,
            isSearching = isVideoSearching,
            onSearchVideos = { topic, country -> viewModel.searchYouTubeVideos(topic, country) },
            onAddChannel = { url, callback -> viewModel.addYouTubeChannel(url, callback) },
            onAddChannelDirect = { id, name -> viewModel.addYouTubeChannelDirect(id, name) },
            onDeleteChannel = { viewModel.deleteYouTubeChannel(it) },
            onClearSearch = { viewModel.clearSearchedVideos() },
            onDismiss = {
                viewModel.clearSearchedVideos()
                showManageDialog = false
            },
            onPlayVideo = { video ->
                try {
                    // Save to history list
                    viewModel.openYouTubeVideo(video)
                    // Force YouTube app Watch page
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${video.videoId}")).apply {
                            setPackage("com.google.android.youtube")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${video.videoId}")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(webIntent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        )
    }
}

@Composable
fun VideoUpdateCard(
    video: YouTubeVideo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp, 62.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                Image(
                    painter = rememberAsyncImagePainter(model = video.thumbnailUrl),
                    contentDescription = "Video Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (video.isShort) Color(0xFFFF2D55)
                            else Color.Black.copy(alpha = 0.7f)
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (video.isShort) "SHORTS" else "VIDEO",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = video.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${video.channelName} • ${video.publishedDate}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Rounded.PlayCircleOutline,
                contentDescription = "Watch Video",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(28.dp)
                    .padding(start = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageChannelsDialog(
    channels: List<YouTubeChannel>,
    searchedVideos: List<YouTubeVideo>,
    isSearching: Boolean,
    onSearchVideos: (String, String) -> Unit,
    onAddChannel: (String, (Boolean) -> Unit) -> Unit,
    onAddChannelDirect: (String, String) -> Unit,
    onDeleteChannel: (YouTubeChannel) -> Unit,
    onClearSearch: () -> Unit,
    onDismiss: () -> Unit,
    onPlayVideo: (YouTubeVideo) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var countryCode by remember { mutableStateOf("IN") }
    val tabTitles = listOf("Subscriptions", "Discover")

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Manage Subscriptions", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedTab == 0) {
                    var channelUrl by remember { mutableStateOf("") }
                    var errorText by remember { mutableStateOf<String?>(null) }
                    var isResolving by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = channelUrl,
                        onValueChange = { channelUrl = it },
                        label = { Text("Channel Link, Handle, or ID") },
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("e.g. @mkbhd, or URL") },
                        enabled = !isResolving,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (errorText != null) {
                        Text(
                            text = errorText!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    Button(
                        onClick = {
                            if (channelUrl.trim().isEmpty()) {
                                errorText = "Please enter a channel link or handle."
                            } else {
                                isResolving = true
                                errorText = null
                                onAddChannel(channelUrl.trim()) { success ->
                                    isResolving = false
                                    if (success) {
                                        channelUrl = ""
                                    } else {
                                        errorText = "Failed to resolve channel."
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isResolving
                    ) {
                        if (isResolving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Add Channel")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Active Subscriptions", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    if (channels.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No active subscriptions. Add channels above or search in Discover tab.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(channels, key = { it.id }) { channel ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(channel.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        if (!channel.handle.isNullOrBlank()) {
                                            Text(
                                                text = channel.handle,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    IconButton(onClick = { onDeleteChannel(channel) }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (selectedTab == 1) {
                    var topic by remember { mutableStateOf("") }
                    var showCountryPrompt by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = topic,
                            onValueChange = { topic = it },
                            label = { Text("Search Topic") },
                            placeholder = { Text("e.g. space, tech, music") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (topic.trim().isNotEmpty()) {
                                    showCountryPrompt = true
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Search")
                        }
                    }

                    if (showCountryPrompt) {
                        AlertDialog(
                            onDismissRequest = { showCountryPrompt = false },
                            title = { Text("Select Country Code") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Enter 2-letter country code (e.g. US, IN, GB, JP):", fontSize = 14.sp)
                                    OutlinedTextField(
                                        value = countryCode,
                                        onValueChange = { countryCode = it.take(2).uppercase() },
                                        placeholder = { Text("IN") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showCountryPrompt = false
                                        onSearchVideos(topic.trim(), countryCode.trim().ifEmpty { "IN" })
                                    }
                                ) {
                                    Text("Search")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCountryPrompt = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    if (isSearching) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (searchedVideos.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Type a topic and search to discover videos",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(searchedVideos) { video ->
                                val isSubscribed = channels.any { it.id == video.channelId }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            onPlayVideo(video)
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp, 45.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(model = video.thumbnailUrl),
                                            contentDescription = "Video Thumbnail",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            video.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            video.channelName,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (video.description.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                video.description,
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    if (isSubscribed) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "Subscribed",
                                            tint = Color(0xFF4CAF50),
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    } else {
                                        IconButton(
                                            onClick = {
                                                if (video.channelId.isNotEmpty()) {
                                                    onAddChannelDirect(video.channelId, video.channelName)
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Add,
                                                contentDescription = "Subscribe",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
