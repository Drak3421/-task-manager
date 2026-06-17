package com.example.myapp.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myapp.data.GroupTask
import com.example.myapp.data.TaskGroup
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupTasksScreen(
    viewModel: TaskViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val groups by viewModel.taskGroups.collectAsState()
    val groupTasks by viewModel.groupTasks.collectAsState()
    val friends by viewModel.friends.collectAsState()
    val myUsername by viewModel.myUsername.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Group Tasks", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (myUsername.isBlank()) {
                        Toast.makeText(context, "Set up your account in Friends first", Toast.LENGTH_SHORT).show()
                    } else {
                        showCreateDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Group")
            }
        }
    ) { padding ->
        if (groups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.Groups,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No groups yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Create a group to share tasks with friends and assign each other to-dos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(groups, key = { it.id }) { group ->
                    val pending = groupTasks.count { it.groupId == group.id && it.status != "done" }
                    GroupCard(group = group, pendingCount = pending, onClick = { onNavigateToDetail(group.id) })
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateGroupDialog(
            friends = friends.map { it.username },
            onDismiss = { showCreateDialog = false },
            onCreate = { name, members ->
                viewModel.createTaskGroup(name, members) { ok, err ->
                    Toast.makeText(context, if (ok) "Group created" else (err ?: "Failed"), Toast.LENGTH_SHORT).show()
                }
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun GroupCard(group: TaskGroup, pendingCount: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Groups,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(group.name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${group.members.size} members" + if (pendingCount > 0) " • $pendingCount open" else "",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CreateGroupDialog(
    friends: List<String>,
    onDismiss: () -> Unit,
    onCreate: (String, List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val selected = remember { mutableStateListOf<String>() }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("New Group", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Text("Add friends", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                if (friends.isEmpty()) {
                    Text("You have no friends to add yet. The group will start with just you.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        friends.forEach { friend ->
                            val isChecked = selected.contains(friend)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) selected.remove(friend) else selected.add(friend)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        if (it) selected.add(friend) else selected.remove(friend)
                                    }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(friend, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onCreate(name, selected.toList()) },
                        enabled = name.isNotBlank()
                    ) { Text("Create") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    viewModel: TaskViewModel,
    groupId: String,
    onNavigateBack: () -> Unit
) {
    val groups by viewModel.taskGroups.collectAsState()
    val allTasks by viewModel.groupTasks.collectAsState()
    val myUsername by viewModel.myUsername.collectAsState()

    val group = groups.find { it.id == groupId }
    val tasks = allTasks.filter { it.groupId == groupId }.sortedWith(
        compareBy({ it.status == "done" }, { it.createdAt })
    )

    var showAddDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    // Group was deleted / left — pop back.
    LaunchedEffect(group) {
        if (group == null) onNavigateBack()
    }
    if (group == null) return

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(group.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Leave group") },
                            onClick = {
                                showMenu = false
                                viewModel.leaveGroup(groupId)
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Task")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Text(
                "Members: " + group.members.joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            if (tasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No tasks yet. Tap + to add one and assign it to a member.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(tasks, key = { it.id }) { task ->
                        GroupTaskCard(
                            task = task,
                            myUsername = myUsername.lowercase(),
                            onMarkDone = { viewModel.markGroupTaskDone(groupId, task.id) },
                            onDelete = { viewModel.deleteGroupTask(groupId, task.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddGroupTaskDialog(
            members = group.members,
            onDismiss = { showAddDialog = false },
            onAdd = { title, desc, dueAt, assignedTo ->
                viewModel.createGroupTask(groupId, title, desc, dueAt, assignedTo)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupTaskCard(
    task: GroupTask,
    myUsername: String,
    onMarkDone: () -> Unit,
    onDelete: () -> Unit
) {
    val isDone = task.status == "done"
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete(); true
            } else false
        }
    )
    var isVisible by remember { mutableStateOf(true) }
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) isVisible = false
    }

    AnimatedVisibility(
        visible = isVisible,
        exit = shrinkVertically(animationSpec = tween(300)) + fadeOut()
    ) {
        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = false,
            backgroundContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.error, RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                }
            }
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            task.title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (isDone) TextDecoration.LineThrough else null
                        )
                        if (task.description.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(task.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (task.dueAt > 0L) {
                            Spacer(Modifier.height(4.dp))
                            val fmt = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
                            Text("Due ${fmt.format(Date(task.dueAt))}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(6.dp))
                        val forWho = if (task.assignedTo.isBlank()) "Anyone" else task.assignedTo
                        Text(
                            "Assigned by ${task.createdBy}  •  For $forWho",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isDone) {
                            Spacer(Modifier.height(6.dp))
                            Surface(
                                color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "✓ Done by ${task.doneBy}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    if (!isDone) {
                        Spacer(Modifier.width(12.dp))
                        FilledTonalIconButton(onClick = onMarkDone) {
                            Icon(Icons.Default.Check, contentDescription = "Mark done")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGroupTaskDialog(
    members: List<String>,
    onDismiss: () -> Unit,
    onAdd: (String, String, Long, String) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueAt by remember { mutableStateOf(0L) }
    var assignedTo by remember { mutableStateOf("") } // "" = anyone
    var assigneeMenu by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("New Task", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))

                // Assignee
                Text("Assign to", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Box {
                    OutlinedButton(onClick = { assigneeMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (assignedTo.isBlank()) "Anyone" else assignedTo)
                    }
                    DropdownMenu(expanded = assigneeMenu, onDismissRequest = { assigneeMenu = false }) {
                        DropdownMenuItem(text = { Text("Anyone") }, onClick = { assignedTo = ""; assigneeMenu = false })
                        members.forEach { m ->
                            DropdownMenuItem(text = { Text(m) }, onClick = { assignedTo = m; assigneeMenu = false })
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                // Optional due date/time
                Text("Due date (optional)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { if (dueAt > 0) timeInMillis = dueAt }
                            DatePickerDialog(
                                context,
                                { _, y, mo, d ->
                                    val c = Calendar.getInstance().apply { if (dueAt > 0) timeInMillis = dueAt }
                                    c.set(Calendar.YEAR, y); c.set(Calendar.MONTH, mo); c.set(Calendar.DAY_OF_MONTH, d)
                                    TimePickerDialog(
                                        context,
                                        { _, h, min ->
                                            c.set(Calendar.HOUR_OF_DAY, h); c.set(Calendar.MINUTE, min)
                                            dueAt = c.timeInMillis
                                        },
                                        c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false
                                    ).show()
                                },
                                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        val label = if (dueAt > 0L) SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(dueAt)) else "Set date"
                        Text(label)
                    }
                    if (dueAt > 0L) {
                        TextButton(onClick = { dueAt = 0L }) { Text("Clear") }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onAdd(title, description, dueAt, assignedTo) },
                        enabled = title.isNotBlank()
                    ) { Text("Add") }
                }
            }
        }
    }
}
