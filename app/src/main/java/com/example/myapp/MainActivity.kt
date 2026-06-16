package com.example.myapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.myapp.ui.HomeScreen
import com.example.myapp.ui.SettingsScreen
import com.example.myapp.ui.TaskEntryScreen
import com.example.myapp.ui.TaskViewModel
import com.example.myapp.ui.YoutubeUpdatesScreen
import com.example.myapp.ui.NewsUpdatesScreen
import com.example.myapp.ui.WebViewScreen
import com.example.myapp.ui.FriendsScreen
import com.example.myapp.ui.ChatScreen
import com.example.myapp.ui.BrowserScreen
import com.example.myapp.ui.AdminConsoleScreen
import com.example.myapp.ui.MusicLibraryScreen
import com.example.myapp.ui.theme.MyAppTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.remember

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle response
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val viewModel: TaskViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val backgroundUri by viewModel.backgroundUri.collectAsState()

            MyAppTheme(darkTheme = isDarkMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
                ) {
                    if (backgroundUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(model = backgroundUri),
                            contentDescription = "Background",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    AppNavigation(viewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: TaskViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable(route = "home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToTask = { taskId ->
                    navController.navigate("taskEntry/$taskId")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToYoutubeUpdates = {
                    navController.navigate("youtubeUpdates")
                },
                onNavigateToNewsUpdates = {
                    navController.navigate("newsUpdates")
                },
                onNavigateToBrowser = {
                    navController.navigate("browser")
                },
                onNavigateToFriends = {
                    navController.navigate("friends")
                },
                onNavigateToMusicLibrary = {
                    navController.navigate("musicLibrary")
                }
            )
        }
        composable(route = "musicLibrary") {
            MusicLibraryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(route = "taskEntry/{taskId}") { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId")?.toIntOrNull() ?: -1
            TaskEntryScreen(
                viewModel = viewModel,
                taskId = taskId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(route = "settings") {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAdminConsole = {
                    navController.navigate("admin_console")
                }
            )
        }
        composable(route = "admin_console") {
            AdminConsoleScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(route = "browser") {
            BrowserScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(route = "friends") {
            FriendsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { username ->
                    navController.navigate("chat/$username")
                }
            )
        }
        composable(route = "chat/{username}") { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: ""
            ChatScreen(
                friendUsername = username,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToWebView = { url, title ->
                    val encoded = java.net.URLEncoder.encode(url, "UTF-8")
                    navController.navigate("webview?url=$encoded&title=$title")
                }
            )
        }
        composable(route = "youtubeUpdates") {
            YoutubeUpdatesScreen(
                viewModel = viewModel,
                onNavigateToWebView = { url, title ->
                    navController.navigate("webview?url=$url&title=$title")
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(route = "newsUpdates") {
            NewsUpdatesScreen(
                viewModel = viewModel,
                onNavigateToWebView = { url, title ->
                    navController.navigate("webview?url=$url&title=$title")
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(route = "webview?url={url}&title={title}") { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
            val title = backStackEntry.arguments?.getString("title") ?: "Web View"
            val decodedUrl = java.net.URLDecoder.decode(encodedUrl, "UTF-8")
            WebViewScreen(
                url = decodedUrl,
                title = title,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
