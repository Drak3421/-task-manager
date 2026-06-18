package com.example.myapp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import coil.compose.rememberAsyncImagePainter
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapp.data.Task
import com.example.myapp.data.SharedTask
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TaskViewModel,
    onNavigateToTask: (Int) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToYoutubeUpdates: () -> Unit,
    onNavigateToNewsUpdates: () -> Unit,
    onNavigateToBrowser: () -> Unit,
    onNavigateToFriends: () -> Unit,
    onNavigateToMusicLibrary: () -> Unit,
    onNavigateToGroupTasks: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    val sharedTasks by viewModel.sharedTasks.collectAsState()
    val welcomeName by viewModel.welcomeName.collectAsState()
    val isUpdateAvailable by viewModel.isUpdateAvailable.collectAsState()
    val updateDownloadUrl by viewModel.updateDownloadUrl.collectAsState()
    val latestVersion by viewModel.latestVersion.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()

    var currentTime by remember { mutableStateOf(Calendar.getInstance().time) }
    
    // Live Watch
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = Calendar.getInstance().time
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { onNavigateToTask(-1) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(bottom = 76.dp) // shift FAB up to clear mini player
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task")
                }
            },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title = { Text("") },
                    actions = {
                        IconButton(
                            onClick = onNavigateToSettings,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp)
            ) {
                // Clock Layout Selection
                val clockLayout by viewModel.clockLayout.collectAsState()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (clockLayout) {
                        "minimalist" -> {
                            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                            Text(
                                text = timeFormat.format(currentTime),
                                fontSize = 54.sp,
                                fontWeight = FontWeight.Light,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        "analog" -> {
                            val color = MaterialTheme.colorScheme.onBackground
                            val accentColor = MaterialTheme.colorScheme.primary
                            androidx.compose.foundation.Canvas(modifier = Modifier.size(120.dp)) {
                                val radius = size.minDimension / 2
                                val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                                
                                // Draw face
                                drawCircle(color = color, radius = radius, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
                                
                                val calendar = Calendar.getInstance()
                                val hour = calendar.get(Calendar.HOUR)
                                val minute = calendar.get(Calendar.MINUTE)
                                val second = calendar.get(Calendar.SECOND)
                                
                                // Hour hand
                                val hourAngle = Math.PI * (hour + minute / 60.0) / 6.0 - Math.PI / 2
                                val hourLength = radius * 0.5f
                                drawLine(
                                    color = color,
                                    start = center,
                                    end = androidx.compose.ui.geometry.Offset(
                                        center.x + (hourLength * kotlin.math.cos(hourAngle)).toFloat(),
                                        center.y + (hourLength * kotlin.math.sin(hourAngle)).toFloat()
                                    ),
                                    strokeWidth = 8f,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                                
                                // Minute hand
                                val minAngle = Math.PI * minute / 30.0 - Math.PI / 2
                                val minLength = radius * 0.75f
                                drawLine(
                                    color = color,
                                    start = center,
                                    end = androidx.compose.ui.geometry.Offset(
                                        center.x + (minLength * kotlin.math.cos(minAngle)).toFloat(),
                                        center.y + (minLength * kotlin.math.sin(minAngle)).toFloat()
                                    ),
                                    strokeWidth = 6f,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                                
                                // Second hand
                                val secAngle = Math.PI * second / 30.0 - Math.PI / 2
                                val secLength = radius * 0.85f
                                drawLine(
                                    color = accentColor,
                                    start = center,
                                    end = androidx.compose.ui.geometry.Offset(
                                        center.x + (secLength * kotlin.math.cos(secAngle)).toFloat(),
                                        center.y + (secLength * kotlin.math.sin(secAngle)).toFloat()
                                    ),
                                    strokeWidth = 3f,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                                
                                // Center dot
                                drawCircle(color = accentColor, radius = 6f, center = center)
                            }
                        }
                        else -> { // digital
                            val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
                            Text(
                                text = timeFormat.format(currentTime),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Light,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                // Date line under the clock
                val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }
                Text(
                    text = dateFormat.format(currentTime),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 2.dp)
                )

                // Time-aware greeting
                Text(
                    text = "${greetingForHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))}, $welcomeName",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 20.dp, bottom = 16.dp)
                )

                // Quick-access shortcut tiles
                val shortcuts = listOf(
                    ShortcutItem("Friends", Icons.Rounded.People,
                        listOf(Color(0xFF0A84FF), Color(0xFF5AC8FA)), onNavigateToFriends),
                    ShortcutItem("YouTube", Icons.Rounded.Subscriptions,
                        listOf(Color(0xFFFF3B30), Color(0xFFFF6961)), onNavigateToYoutubeUpdates),
                    ShortcutItem("News", Icons.Rounded.Newspaper,
                        listOf(Color(0xFF5856D6), Color(0xFFAF52DE)), onNavigateToNewsUpdates),
                    ShortcutItem("Browser", Icons.Rounded.Language,
                        listOf(Color(0xFF34C759), Color(0xFF30D158)), onNavigateToBrowser),
                    ShortcutItem("Music", Icons.Rounded.MusicNote,
                        listOf(Color(0xFFFF2D55), Color(0xFFFF375F)), onNavigateToMusicLibrary)
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    shortcuts.chunked(3).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowItems.forEach { item ->
                                ShortcutTile(item = item, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ALARMS",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val totalCount = tasks.size + sharedTasks.size
                    if (totalCount > 0) {
                        Text(
                            text = "$totalCount",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (tasks.isEmpty() && sharedTasks.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Rounded.Alarm,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No alarms yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap + to set your first alarm",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 90.dp), // allow scrolling past mini player
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(tasks, key = { "own_${it.id}" }) { task ->
                            TaskCard(
                                task = task,
                                onClick = { onNavigateToTask(task.id) },
                                onDelete = { viewModel.deleteTask(task) }
                            )
                        }
                        items(sharedTasks, key = { "shared_${it.ownerUsername}_${it.taskId}" }) { shared ->
                            SharedTaskCard(shared = shared)
                        }
                    }
                }
            }
        }

        // Required Update Dialog
        if (isUpdateAvailable) {
            val context = LocalContext.current
            var titleClickCount by remember { mutableStateOf(0) }
            AlertDialog(
                onDismissRequest = { /* Non-dismissible */ },
                title = {
                    Text(
                        text = "Update Required",
                        modifier = Modifier.clickable {
                            titleClickCount++
                            if (titleClickCount >= 5) {
                                titleClickCount = 0
                                onNavigateToSettings()
                            }
                        }
                    )
                },
                text = {
                    Column {
                        Text("A new version of this app is available. Please download and reinstall to continue.")
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Installed Version: ${viewModel.currentVersionCode}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("Latest Version: $latestVersion", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        
                        if (downloadProgress != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            val pct = (downloadProgress!! * 100).toInt()
                            Text("Downloading update: $pct%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { downloadProgress!! },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    if (downloadProgress == null) {
                        Button(
                            onClick = {
                                if (updateDownloadUrl.isNotEmpty()) {
                                    viewModel.downloadAndInstallUpdate(context, updateDownloadUrl)
                                } else {
                                    Toast.makeText(context, "Download URL is not configured yet", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("Download & Install")
                        }
                    }
                },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            )
        }

        // Music player sheet floats on top at the bottom
        MusicPlayerSheet(
            viewModel = viewModel,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCard(
    task: Task,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )
    
    var isVisible by remember { mutableStateOf(true) }

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            isVisible = false
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        exit = shrinkVertically(animationSpec = tween(durationMillis = 300)) + fadeOut()
    ) {
        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = false,
            backgroundContent = {
                val color = MaterialTheme.colorScheme.error
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color, RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White
                    )
                }
            }
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        val date = Date(task.timestampMs)

                        Text(
                            text = timeFormat.format(date),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${task.title} • ${dateFormat.format(date)}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(Color(0xFFFF9500), Color(0xFFFF6A00))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Alarm,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun greetingForHour(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..20 -> "Good evening"
    else -> "Good night"
}

data class ShortcutItem(
    val label: String,
    val icon: ImageVector,
    val gradient: List<Color>,
    val onClick: () -> Unit
)

@Composable
fun SharedTaskCard(shared: SharedTask) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val date = Date(shared.timestampMs)

                Text(
                    text = timeFormat.format(date),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${shared.title} • ${dateFormat.format(date)}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "from ${shared.ownerUsername}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(Color(0xFF0A84FF), Color(0xFF5AC8FA))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.People,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun ShortcutTile(item: ShortcutItem, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { item.onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(item.gradient)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
