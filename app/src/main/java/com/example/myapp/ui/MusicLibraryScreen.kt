package com.example.myapp.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapp.data.MusicTrack
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicLibraryScreen(
    viewModel: TaskViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allTracks by viewModel.allMusicTracks.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    val selectedTracks = remember { mutableStateListOf<MusicTrack>() }
    val isInSelectionMode = selectedTracks.isNotEmpty()
    var trackToRename by remember { mutableStateOf<MusicTrack?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameTitle by remember { mutableStateOf("") }
    var renameArtist by remember { mutableStateOf("") }
    var showShareDialog by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0 = All, 1 = Imported, 2 = Device

    val requiredPermission = if (android.os.Build.VERSION.SDK_INT >= 33) {
        android.Manifest.permission.READ_MEDIA_AUDIO
    } else {
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                requiredPermission
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasPermission = isGranted
        if (isGranted) {
            viewModel.loadDeviceSongs(context)
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            viewModel.loadDeviceSongs(context)
        }
    }

    val audioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importAudioFile(context, it) }
    }

    // Filter tracks based on search query and selected tab
    val filteredTracks = remember(allTracks, searchQuery, selectedTab) {
        allTracks.filter { track ->
            val matchesSearch = track.title.contains(searchQuery, ignoreCase = true) ||
                    track.artist.contains(searchQuery, ignoreCase = true)
            val matchesTab = when (selectedTab) {
                1 -> !track.id.startsWith("device_")
                2 -> track.id.startsWith("device_")
                else -> true
            }
            matchesSearch && matchesTab
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        if (isInSelectionMode) {
                            Text("${selectedTracks.size} Selected", fontWeight = FontWeight.Bold)
                        } else {
                            Text("Music Library", fontWeight = FontWeight.Bold)
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (isInSelectionMode) {
                                    selectedTracks.clear()
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
                            IconButton(onClick = { showShareDialog = true }) {
                                Icon(imageVector = Icons.Rounded.Share, contentDescription = "Share selected songs")
                            }
                            val canDeleteAll = selectedTracks.all { !it.id.startsWith("device_") }
                            if (canDeleteAll) {
                                IconButton(
                                    onClick = {
                                        viewModel.deleteAudioFiles(selectedTracks.toList())
                                        selectedTracks.clear()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "Delete selected songs",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        } else {
                            IconButton(onClick = { audioLauncher.launch("audio/*") }) {
                                Icon(imageVector = Icons.Rounded.CloudUpload, contentDescription = "Import Audio")
                            }
                            if (!hasPermission) {
                                IconButton(onClick = { permissionLauncher.launch(requiredPermission) }) {
                                    Icon(imageVector = Icons.Rounded.FindInPage, contentDescription = "Scan Device")
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
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search title, artist...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true
                )

                // Tabs for filtering types
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("All (${allTracks.size})") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Imported (${allTracks.count { !it.id.startsWith("device_") }})") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Device (${allTracks.count { it.id.startsWith("device_") }})") }
                    )
                }

                // List of songs
                if (filteredTracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.MusicOff,
                                contentDescription = "No Tracks",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No results found" else "No music tracks",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (searchQuery.isEmpty() && !hasPermission) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { permissionLauncher.launch(requiredPermission) }) {
                                    Text("Scan Device Audio")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp)
                    ) {
                        items(filteredTracks, key = { it.id }) { track ->
                            val isCurrentlyPlaying = currentTrack?.id == track.id
                            val isTrackSelected = selectedTracks.any { it.id == track.id }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isTrackSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else if (isCurrentlyPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                    .combinedClickable(
                                        onLongClick = {
                                            if (selectedTracks.any { it.id == track.id }) {
                                                selectedTracks.removeAll { it.id == track.id }
                                            } else {
                                                selectedTracks.add(track)
                                            }
                                        },
                                        onClick = {
                                            if (isInSelectionMode) {
                                                if (selectedTracks.any { it.id == track.id }) {
                                                    selectedTracks.removeAll { it.id == track.id }
                                                } else {
                                                    selectedTracks.add(track)
                                                }
                                            } else {
                                                if (isCurrentlyPlaying && isPlaying) {
                                                    viewModel.musicPlayerManager.pause()
                                                } else if (isCurrentlyPlaying) {
                                                    viewModel.musicPlayerManager.resume()
                                                } else {
                                                    viewModel.musicPlayerManager.play(track)
                                                }
                                            }
                                        }
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isInSelectionMode) {
                                    Checkbox(
                                        checked = isTrackSelected,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                if (!selectedTracks.any { it.id == track.id }) selectedTracks.add(track)
                                            } else {
                                                selectedTracks.removeAll { it.id == track.id }
                                            }
                                        },
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                }

                                // Album Art circle
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isCurrentlyPlaying) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isCurrentlyPlaying && isPlaying) Icons.Rounded.VolumeUp else Icons.Rounded.MusicNote,
                                        contentDescription = "Song Icon",
                                        tint = if (isCurrentlyPlaying) Color.White else MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Details
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = track.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isCurrentlyPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = track.artist,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Duration
                                Text(
                                    text = formatTime(track.durationMs.toInt()),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                // Action buttons (Rename & Delete)
                                if (!track.id.startsWith("device_") && !isInSelectionMode) {
                                    IconButton(
                                        onClick = {
                                            trackToRename = track
                                            renameTitle = track.title
                                            renameArtist = track.artist
                                            showRenameDialog = true
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = "Rename track",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteAudioFile(track) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Delete,
                                            contentDescription = "Delete track",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Rename dialog
        if (showRenameDialog && trackToRename != null) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text("Rename Song") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = renameTitle,
                            onValueChange = { renameTitle = it },
                            label = { Text("Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = renameArtist,
                            onValueChange = { renameArtist = it },
                            label = { Text("Artist") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val trackId = trackToRename?.id
                            if (trackId != null && renameTitle.isNotBlank()) {
                                viewModel.renameMusicTrack(trackId, renameTitle, renameArtist)
                            }
                            showRenameDialog = false
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Share dialog
        if (showShareDialog) {
            val friends by viewModel.friends.collectAsState()
            AlertDialog(
                onDismissRequest = { showShareDialog = false },
                title = { Text("Share ${selectedTracks.size} Song(s)") },
                text = {
                    if (friends.isEmpty()) {
                        Text("You have no friends to share with. Add some friends first!")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                        ) {
                            items(friends) { friend ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showShareDialog = false
                                            val count = selectedTracks.size
                                            val friendUsername = friend.username
                                            Toast.makeText(context, "Sharing $count song(s) with $friendUsername...", Toast.LENGTH_SHORT).show()
                                            
                                            // Share all selected tracks sequentially to avoid concurrent rate-limit failures
                                            val tracksCopy = selectedTracks.toList()
                                            selectedTracks.clear()
                                            coroutineScope.launch {
                                                tracksCopy.forEach { track ->
                                                    viewModel.uploadAndSendFileSync(context, friendUsername, track.filePath, "audio")
                                                    delay(1000)
                                                }
                                            }
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    UserAvatar(
                                        username = friend.username,
                                        modifier = Modifier.size(32.dp),
                                        viewModel = viewModel,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(friend.username, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showShareDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Floating mini player sheet
        MusicPlayerSheet(
            viewModel = viewModel,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

private fun formatTime(ms: Int): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}
