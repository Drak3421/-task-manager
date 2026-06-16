package com.example.myapp.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapp.data.Task
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent
import android.media.RingtoneManager
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEntryScreen(
    viewModel: TaskViewModel,
    taskId: Int,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var timestampMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var autoQuitMinutes by remember { mutableStateOf(5) }
    var ringtoneUri by remember { mutableStateOf<String?>(null) }
    var ringtoneName by remember { mutableStateOf<String?>(null) }

    val musicTracks by viewModel.allMusicTracks.collectAsState()
    var showSoundPickerDialog by remember { mutableStateOf(false) }

    var previewingUri by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    fun stopPreview() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
        previewingUri = null
    }

    fun playPreview(uriStr: String?) {
        stopPreview()
        if (uriStr == null) {
            val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            try {
                val mp = MediaPlayer().apply {
                    setDataSource(context, defaultUri)
                    setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    prepare()
                    start()
                }
                mediaPlayer = mp
                previewingUri = ""
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            try {
                val mp = MediaPlayer().apply {
                    setDataSource(context, android.net.Uri.parse(uriStr))
                    setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    prepare()
                    start()
                }
                mediaPlayer = mp
                previewingUri = uriStr
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        }
    }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, android.net.Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (uri != null) {
                ringtoneUri = uri.toString()
                val ringtone = RingtoneManager.getRingtone(context, uri)
                ringtoneName = ringtone?.getTitle(context) ?: "Custom Ringtone"
            }
        }
    }

    LaunchedEffect(taskId) {
        if (taskId != -1) {
            viewModel.getTaskById(taskId)?.let { task ->
                title = task.title
                description = task.description
                timestampMs = task.timestampMs
                autoQuitMinutes = task.autoQuitDurationMinutes
                ringtoneUri = task.ringtoneUri
                ringtoneName = task.ringtoneName
            }
        } else {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MINUTE, 5)
            timestampMs = cal.timeInMillis
        }
    }

    val cal = Calendar.getInstance().apply { timeInMillis = timestampMs }
    val formattedDate = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault()).format(cal.time)
    val formattedTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (taskId == -1) "New Alarm" else "Edit Alarm", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        val finalTitle = if (title.isBlank()) "Task Alarm" else title
                        val task = Task(
                            id = if (taskId == -1) 0 else taskId,
                            title = finalTitle,
                            description = description,
                            timestampMs = timestampMs,
                            autoQuitDurationMinutes = autoQuitMinutes,
                            ringtoneUri = ringtoneUri,
                            ringtoneName = ringtoneName
                        )
                        viewModel.saveTask(task) {
                            onNavigateBack()
                        }
                    }) {
                        Text("Save", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // iOS-Style Input Card for Title & Notes
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        )
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                    TextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("Notes") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        )
                    )
                }
            }

            // iOS-Style Date & Time Selection Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val newCal = Calendar.getInstance().apply { timeInMillis = timestampMs }
                                        newCal.set(Calendar.YEAR, year)
                                        newCal.set(Calendar.MONTH, month)
                                        newCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        timestampMs = newCal.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Date", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(formattedDate, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        val newCal = Calendar.getInstance().apply { timeInMillis = timestampMs }
                                        newCal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        newCal.set(Calendar.MINUTE, minute)
                                        timestampMs = newCal.timeInMillis
                                    },
                                    cal.get(Calendar.HOUR_OF_DAY),
                                    cal.get(Calendar.MINUTE),
                                    false
                                ).show()
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Time", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(formattedTime, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            // iOS-Style Sound Picker Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showSoundPickerDialog = true
                        }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sound", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(ringtoneName ?: "Default Sound", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
                }
            }

            // Custom Stepper Card for Alarm Silence timeout
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Silence Duration", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Silences the alarm automatically after the specified time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { if (autoQuitMinutes > 1) autoQuitMinutes-- }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Remove,
                                    contentDescription = "Decrease",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Text(
                            text = "$autoQuitMinutes min",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { if (autoQuitMinutes < 60) autoQuitMinutes++ }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Increase",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
        }
    }

    if (showSoundPickerDialog) {
        Dialog(onDismissRequest = {
            stopPreview()
            showSoundPickerDialog = false
        }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Select Alarm Sound",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    val systemSounds = remember { getSystemAlarms(context) }
                    val mySongs = remember(musicTracks) {
                        musicTracks.map {
                            AlarmSoundOption(it.title, it.filePath, isSystem = false)
                        }
                    }
                    val allSounds = systemSounds + mySongs

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "Standard Sounds",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        val (sysOptions, songOptions) = allSounds.partition { it.isSystem || it.isDefault }

                        items(sysOptions) { option ->
                            SoundItemRow(
                                option = option,
                                isSelected = (option.uri == ringtoneUri) || (option.isDefault && ringtoneUri == null),
                                isPlaying = (previewingUri == option.uri && (option.uri != null || previewingUri == "")),
                                onSelect = {
                                    ringtoneUri = option.uri
                                    ringtoneName = option.name
                                },
                                onPlay = {
                                    if (previewingUri == option.uri && (option.uri != null || previewingUri == "")) {
                                        stopPreview()
                                    } else {
                                        playPreview(option.uri)
                                    }
                                }
                            )
                        }

                        if (songOptions.isNotEmpty()) {
                            item {
                                Text(
                                    text = "My Music",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }

                            items(songOptions) { option ->
                                SoundItemRow(
                                    option = option,
                                    isSelected = (option.uri == ringtoneUri),
                                    isPlaying = (previewingUri == option.uri),
                                    onSelect = {
                                        ringtoneUri = option.uri
                                        ringtoneName = option.name
                                    },
                                    onPlay = {
                                        if (previewingUri == option.uri) {
                                            stopPreview()
                                        } else {
                                            playPreview(option.uri)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            stopPreview()
                            showSoundPickerDialog = false
                        }) {
                            Text("Done", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
    }
}
}
}

data class AlarmSoundOption(
    val name: String,
    val uri: String?,
    val isSystem: Boolean,
    val isDefault: Boolean = false
)

@Composable
fun SoundItemRow(
    option: AlarmSoundOption,
    isSelected: Boolean,
    isPlaying: Boolean,
    onSelect: () -> Unit,
    onPlay: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .padding(vertical = 6.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = option.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        IconButton(onClick = onPlay) {
            Icon(
                imageVector = if (isPlaying) androidx.compose.material.icons.Icons.Rounded.Stop else androidx.compose.material.icons.Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Stop Preview" else "Play Preview",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

fun getSystemAlarms(context: android.content.Context): List<AlarmSoundOption> {
    val list = mutableListOf<AlarmSoundOption>()
    list.add(AlarmSoundOption("Default Alarm Sound", null, isSystem = false, isDefault = true))
    val manager = RingtoneManager(context).apply {
        setType(RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_RINGTONE)
    }
    try {
        val cursor = manager.cursor
        while (cursor.moveToNext()) {
            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            val uri = manager.getRingtoneUri(cursor.position)?.toString()
            if (uri != null) {
                list.add(AlarmSoundOption(title, uri, isSystem = true))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}
