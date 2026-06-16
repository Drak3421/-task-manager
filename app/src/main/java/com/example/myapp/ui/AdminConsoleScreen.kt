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
        }
    }
}
