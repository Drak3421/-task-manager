package com.example.myapp.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Removed OTA Imports

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminConsoleScreen(
    viewModel: TaskViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var passcode by remember { mutableStateOf("") }
    var isUnlocked by remember { mutableStateOf(false) }
    var passcodeError by remember { mutableStateOf("") }

    val latestVersion by viewModel.latestVersion.collectAsState()
    val updateDownloadUrl by viewModel.updateDownloadUrl.collectAsState()
    val isPublishingUpdate by viewModel.isPublishingUpdate.collectAsState()

    // Passcode Screen
    if (!isUnlocked) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Admin Verification") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Passcode Required",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Enter Admin Passcode",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Provide the developer passcode to edit global updates",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = passcode,
                    onValueChange = {
                        passcode = it
                        passcodeError = ""
                    },
                    label = { Text("Passcode") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    isError = passcodeError.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
                if (passcodeError.isNotEmpty()) {
                    Text(
                        text = passcodeError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (passcode == "@Muskan1234") {
                            isUnlocked = true
                        } else {
                            passcodeError = "Invalid passcode. Try '@Muskan1234'"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text("Unlock Console")
                }
            }
        }
        return
    }

    // Admin Panel Screen (Unlocked)
    LaunchedEffect(Unit) {
        viewModel.fetchAllRegisteredUsers()
    }
    val registeredUsers by viewModel.registeredUsers.collectAsState()
    val isFetchingUsers by viewModel.isFetchingRegisteredUsers.collectAsState()

    var versionCodeStr by remember { mutableStateOf("") }
    var downloadUrlStr by remember { mutableStateOf("") }

    // Keep fields synchronized if remote changes loaded
    LaunchedEffect(latestVersion, updateDownloadUrl) {
        if (versionCodeStr.isEmpty()) {
            versionCodeStr = latestVersion.toString()
        }
        if (downloadUrlStr.isEmpty()) {
            downloadUrlStr = updateDownloadUrl
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Global Admin Console") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Publish App Update",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Enter a new version code and download link. Other clients will see a required, non-dismissible update dialog instantly.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = versionCodeStr,
                onValueChange = { versionCodeStr = it },
                label = { Text("New Version Code") },
                placeholder = { Text("e.g. 2") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = downloadUrlStr,
                onValueChange = { downloadUrlStr = it },
                label = { Text("Download URL") },
                placeholder = { Text("e.g. https://example.com/app.apk") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(32.dp))

            if (isPublishingUpdate) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                Button(
                    onClick = {
                        val code = versionCodeStr.toIntOrNull()
                        if (code == null || code <= 0) {
                            Toast.makeText(context, "Please enter a valid positive version code (integer)", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (downloadUrlStr.trim().isEmpty()) {
                            Toast.makeText(context, "Please enter a valid download URL", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.publishAppUpdate(code, downloadUrlStr.trim()) { success, err ->
                            if (success) {
                                Toast.makeText(context, "Published app update successfully!", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            } else {
                                Toast.makeText(context, "Failed to publish: $err", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Publish App Update", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Delete User Account",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Permanently remove a user from the global database, clear their friendship connections, and force-logout their active app instance.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(24.dp))

            var deleteSearchQuery by remember { mutableStateOf("") }
            var userToDelete by remember { mutableStateOf<String?>(null) }
            var isDeleting by remember { mutableStateOf(false) }

            OutlinedTextField(
                value = deleteSearchQuery,
                onValueChange = { deleteSearchQuery = it },
                label = { Text("Filter Usernames") },
                placeholder = { Text("Search by username...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (isFetchingUsers) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                val filteredUsers = registeredUsers.filter {
                    it.contains(deleteSearchQuery, ignoreCase = true)
                }

                if (filteredUsers.isEmpty()) {
                    Text(
                        text = "No users found",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp)
                        ) {
                            filteredUsers.forEachIndexed { index, user ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "@$user",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    IconButton(
                                        onClick = { userToDelete = user }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete User",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                if (index < filteredUsers.lastIndex) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isDeleting) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(color = MaterialTheme.colorScheme.error)
            }

            userToDelete?.let { targetUser ->
                AlertDialog(
                    onDismissRequest = { userToDelete = null },
                    title = { Text("Permanently Delete User?") },
                    text = { Text("Are you sure you want to delete the user '@$targetUser'? This action is irreversible and will force-logout the user immediately.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                userToDelete = null
                                isDeleting = true
                                viewModel.adminDeleteUser(targetUser) { success, err ->
                                    isDeleting = false
                                    if (success) {
                                        Toast.makeText(context, "User '@$targetUser' deleted successfully!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Deletion failed: $err", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete Permanently")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { userToDelete = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
