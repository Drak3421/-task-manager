package com.example.myapp.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapp.data.FavoriteWebsite
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: TaskViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val favoriteWebsites by viewModel.favoriteWebsites.collectAsState()
    var urlInput by remember { mutableStateOf("") }
    var activeUrl by remember { mutableStateOf("") }
    var lastLoadedUrl by remember { mutableStateOf("") }

    val isFavorited = remember(favoriteWebsites, urlInput) {
        favoriteWebsites.any { it.url == urlInput }
    }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    var customView by remember { mutableStateOf<android.view.View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    // Custom back press handler to go back in WebView history
    BackHandler(enabled = canGoBack && customView == null) {
        webViewInstance?.goBack()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (customView != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    android.widget.FrameLayout(ctx).apply {
                        setBackgroundColor(android.graphics.Color.BLACK)
                        addView(
                            customView,
                            android.widget.FrameLayout.LayoutParams(
                                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                            )
                        )
                    }
                }
            )

            BackHandler {
                customViewCallback?.onCustomViewHidden()
                customView = null
                customViewCallback = null
            }
        } else {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = {
                                if (activeUrl.isNotEmpty()) {
                                    activeUrl = ""
                                    urlInput = ""
                                    lastLoadedUrl = ""
                                } else {
                                    onNavigateBack()
                                }
                            }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        title = {
                            OutlinedTextField(
                                value = urlInput,
                                onValueChange = { urlInput = it },
                                placeholder = { Text("Search or type URL", fontSize = 14.sp) },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = "Search",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = {
                                    if (urlInput.isNotEmpty()) {
                                        IconButton(onClick = { urlInput = "" }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "Clear",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Search
                                ),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        focusManager.clearFocus()
                                        val query = urlInput.trim()
                                        if (query.isNotEmpty()) {
                                            val destination = if (query.startsWith("http://") || query.startsWith("https://")) {
                                                query
                                            } else if (query.contains(".") && !query.contains(" ")) {
                                                "https://$query"
                                            } else {
                                                "https://www.google.com/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
                                            }
                                            activeUrl = destination
                                            urlInput = destination
                                        }
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .padding(end = 8.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                        },
                        actions = {
                            if (activeUrl.isNotEmpty()) {
                                IconButton(onClick = {
                                    viewModel.toggleFavoriteWebsite(webViewInstance?.title ?: "Favorite Website", urlInput)
                                }) {
                                    Icon(
                                        imageVector = if (isFavorited) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                        contentDescription = "Favorite",
                                        tint = if (isFavorited) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(onClick = {
                                    webViewInstance?.reload()
                                }) {
                                    Icon(imageVector = Icons.Rounded.Refresh, contentDescription = "Reload")
                                }
                            }
                        }
                    )
                },
                bottomBar = {
                    if (activeUrl.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(vertical = 8.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { webViewInstance?.goBack() },
                                    enabled = canGoBack
                                ) {
                                    Icon(imageVector = Icons.Rounded.ArrowBackIosNew, contentDescription = "Web Back")
                                }
                                IconButton(
                                    onClick = { webViewInstance?.goForward() },
                                    enabled = canGoForward
                                ) {
                                    Icon(imageVector = Icons.Rounded.ArrowForwardIos, contentDescription = "Web Forward")
                                }
                                IconButton(
                                    onClick = {
                                        activeUrl = ""
                                        urlInput = ""
                                        lastLoadedUrl = ""
                                    }
                                ) {
                                    Icon(imageVector = Icons.Rounded.Home, contentDescription = "Web Home")
                                }
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    if (activeUrl.isEmpty()) {
                        // Redesigned Crisp Browser Home Page
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp)
                                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(40.dp))
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Language,
                                    contentDescription = "Search Web",
                                    modifier = Modifier.size(40.dp),
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "In-App Browser",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Search the web and download wallpapers directly",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))

                            // Home Page Search Bar
                            OutlinedTextField(
                                value = urlInput,
                                onValueChange = { urlInput = it },
                                placeholder = { Text("Search or type URL", fontSize = 15.sp) },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = "Search",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = {
                                    if (urlInput.isNotEmpty()) {
                                        IconButton(onClick = { urlInput = "" }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "Clear",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Search
                                ),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        focusManager.clearFocus()
                                        val query = urlInput.trim()
                                        if (query.isNotEmpty()) {
                                            val destination = if (query.startsWith("http://") || query.startsWith("https://")) {
                                                query
                                            } else if (query.contains(".") && !query.contains(" ")) {
                                                "https://$query"
                                            } else {
                                                "https://www.google.com/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
                                            }
                                            activeUrl = destination
                                            urlInput = destination
                                        }
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(28.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            if (favoriteWebsites.isNotEmpty()) {
                                Text(
                                    text = "My Favorites",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                )
                                
                                val favoritesChunked = favoriteWebsites.chunked(4)
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                                ) {
                                    favoritesChunked.forEach { rowItems ->
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            rowItems.forEach { fav ->
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(vertical = 4.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier.size(64.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        // Icon tile
                                                        Box(
                                                            modifier = Modifier
                                                                .size(54.dp)
                                                                .clip(RoundedCornerShape(16.dp))
                                                                .background(
                                                                    Brush.linearGradient(
                                                                        listOf(
                                                                            MaterialTheme.colorScheme.primary,
                                                                            MaterialTheme.colorScheme.secondary
                                                                        )
                                                                    )
                                                                )
                                                                .clickable {
                                                                    activeUrl = fav.url
                                                                    urlInput = fav.url
                                                                },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = fav.title.firstOrNull()?.uppercase() ?: "W",
                                                                color = Color.White,
                                                                fontSize = 18.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }

                                                        // Small X close button at top-right of the icon
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.TopEnd)
                                                                .size(20.dp)
                                                                .clip(CircleShape)
                                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                                .clickable {
                                                                    viewModel.toggleFavoriteWebsite(fav.title, fav.url)
                                                                },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.Close,
                                                                contentDescription = "Remove",
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = fav.title,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                            if (rowItems.size < 4) {
                                                repeat(4 - rowItems.size) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Text(
                                text = "Quick Wallpaper Sites",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            )

                            val quickLinks = listOf(
                                QuickLink("Unsplash", "https://unsplash.com", Icons.Rounded.Image),
                                QuickLink("Pexels", "https://www.pexels.com", Icons.Rounded.PhotoLibrary),
                                QuickLink("Pixabay", "https://pixabay.com", Icons.Rounded.Wallpaper),
                                QuickLink("Google Images", "https://www.google.com/imghp", Icons.Rounded.Search)
                            )

                            val quickLinksChunked = quickLinks.chunked(4)
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
                            ) {
                                quickLinksChunked.forEach { rowItems ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        rowItems.forEach { link ->
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(vertical = 4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(54.dp)
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .background(
                                                            Brush.linearGradient(
                                                                listOf(
                                                                    MaterialTheme.colorScheme.secondary,
                                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                                                )
                                                            )
                                                        )
                                                        .clickable {
                                                            activeUrl = link.url
                                                            urlInput = link.url
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = link.icon,
                                                        contentDescription = link.name,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = link.name,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                        if (rowItems.size < 4) {
                                            repeat(4 - rowItems.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // WebView component
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        databaseEnabled = true
                                        useWideViewPort = true
                                        loadWithOverviewMode = true
                                        supportZoom()
                                        builtInZoomControls = true
                                        displayZoomControls = false
                                    }
                                    
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                            super.onPageStarted(view, url, favicon)
                                            isLoading = true
                                            url?.let {
                                                if (urlInput != it) {
                                                    urlInput = it
                                                }
                                                lastLoadedUrl = it
                                                activeUrl = it
                                            }
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            isLoading = false
                                            canGoBack = canGoBack()
                                            canGoForward = canGoForward()
                                            url?.let {
                                                if (urlInput != it) {
                                                    urlInput = it
                                                }
                                                lastLoadedUrl = it
                                                activeUrl = it
                                            }
                                        }

                                        override fun shouldOverrideUrlLoading(
                                            view: WebView?,
                                            request: android.webkit.WebResourceRequest?
                                        ): Boolean {
                                            val urlString = request?.url?.toString() ?: return false
                                            if (urlString.contains("youtube.com") || urlString.contains("youtu.be") || urlString.startsWith("vnd.youtube")) {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlString)).apply {
                                                        setPackage("com.google.android.youtube")
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                    context.startActivity(intent)
                                                    return true
                                                } catch (e: Exception) {
                                                    try {
                                                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(urlString)).apply {
                                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        }
                                                        context.startActivity(webIntent)
                                                        return true
                                                    } catch (ex: Exception) {
                                                        ex.printStackTrace()
                                                    }
                                                }
                                            }
                                            return super.shouldOverrideUrlLoading(view, request)
                                        }
                                    }
                                    
                                    webChromeClient = object : WebChromeClient() {
                                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                            super.onProgressChanged(view, newProgress)
                                        }

                                        override fun onShowCustomView(view: android.view.View?, callback: CustomViewCallback?) {
                                            super.onShowCustomView(view, callback)
                                            if (customView != null) {
                                                callback?.onCustomViewHidden()
                                                return
                                            }
                                            customView = view
                                            customViewCallback = callback
                                        }

                                        override fun onHideCustomView() {
                                            super.onHideCustomView()
                                            customViewCallback?.onCustomViewHidden()
                                            customView = null
                                            customViewCallback = null
                                        }
                                    }

                                    setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                                        val request = android.app.DownloadManager.Request(Uri.parse(url)).apply {
                                            setMimeType(mimetype)
                                            addRequestHeader("User-Agent", userAgent)
                                            setDescription("Downloading wallpaper...")
                                            val fileName = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimetype)
                                            setTitle(fileName)
                                            setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                            setDestinationInExternalPublicDir(
                                                android.os.Environment.DIRECTORY_DOWNLOADS,
                                                fileName
                                            )
                                        }
                                        val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                                        try {
                                            dm.enqueue(request)
                                            Toast.makeText(ctx, "Download started...", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            Toast.makeText(ctx, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }

                                    webViewInstance = this
                                    loadUrl(activeUrl)
                                }
                            },
                            update = { webView ->
                                if (activeUrl.isNotEmpty() && activeUrl != lastLoadedUrl) {
                                    lastLoadedUrl = activeUrl
                                    webView.loadUrl(activeUrl)
                                }
                            }
                        )
                        
                        if (isLoading) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

data class QuickLink(
    val name: String,
    val url: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
