package com.example.myapp.ui

import android.content.Intent
import android.net.Uri
import java.io.File
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    url: String,
    title: String,
    viewModel: TaskViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val downloadedArticles by viewModel.downloadedNews.collectAsState()
    val isDownloaded = remember(downloadedArticles, url) {
        downloadedArticles.any { it.link == url }
    }

    val feedArticles by viewModel.aiNewsArticles.collectAsState()
    val recentArticles by viewModel.recentNews.collectAsState()
    val matchingArticle = remember(feedArticles, recentArticles, url) {
        feedArticles.find { it.link == url } ?: recentArticles.find { it.link == url }
    }

    var currentWebViewUrl by remember { mutableStateOf(url) }
    var lastLoadedUrl by remember { mutableStateOf(url) }

    var customView by remember { mutableStateOf<android.view.View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

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
                topBar = {
                    TopAppBar(
                        title = { Text(title, maxLines = 1, style = MaterialTheme.typography.titleMedium) },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            if (matchingArticle != null && !url.startsWith("file://")) {
                                IconButton(
                                    onClick = {
                                        if (isDownloaded) {
                                            Toast.makeText(context, "Already downloaded for offline reading", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Downloading article...", Toast.LENGTH_SHORT).show()
                                            viewModel.downloadNewsArticle(matchingArticle, currentWebViewUrl) { success ->
                                                if (success) {
                                                    Toast.makeText(context, "Downloaded successfully!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isDownloaded) Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                                        contentDescription = if (isDownloaded) "Downloaded" else "Download Offline"
                                    )
                                }
                            }
                        }
                    )
                }
            ) { paddingValues ->
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                mediaPlaybackRequiresUserGesture = false
                                allowFileAccess = true
                                allowContentAccess = true
                                userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                                javaScriptCanOpenWindowsAutomatically = true
                            }
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    if (url != null) {
                                        currentWebViewUrl = url
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
                            
                            if (url.startsWith("file://")) {
                                try {
                                    val file = File(url.substring(7))
                                    if (file.exists()) {
                                        val htmlContent = file.readText()
                                        loadDataWithBaseURL("https://localhost/", htmlContent, "text/html", "UTF-8", null)
                                    } else {
                                        loadDataWithBaseURL("https://localhost/", "<html><body><h3>Error: Offline article file not found.</h3></body></html>", "text/html", "UTF-8", null)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    loadUrl(url)
                                }
                            } else {
                                loadUrl(url)
                            }
                        }
                    },
                    update = { webView ->
                        if (url != lastLoadedUrl) {
                            lastLoadedUrl = url
                            currentWebViewUrl = url
                            if (url.startsWith("file://")) {
                                try {
                                    val file = File(url.substring(7))
                                    if (file.exists()) {
                                        val htmlContent = file.readText()
                                        webView.loadDataWithBaseURL("https://localhost/", htmlContent, "text/html", "UTF-8", null)
                                    } else {
                                        webView.loadDataWithBaseURL("https://localhost/", "<html><body><h3>Error: Offline article file not found.</h3></body></html>", "text/html", "UTF-8", null)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    webView.loadUrl(url)
                                }
                            } else {
                                webView.loadUrl(url)
                            }
                        }
                    }
                )
            }
        }
    }
}
