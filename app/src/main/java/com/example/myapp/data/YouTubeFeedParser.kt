package com.example.myapp.data

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object YouTubeFeedParser {

    suspend fun resolveChannelIdFromLink(input: String): String? = withContext(Dispatchers.IO) {
        val trimmed = input.trim()
        if (trimmed.startsWith("UC") && trimmed.length == 24) {
            return@withContext trimmed
        }
        if (trimmed.contains("/channel/")) {
            val id = trimmed.substringAfter("/channel/").substringBefore("/").substringBefore("?")
            if (id.startsWith("UC") && id.length == 24) {
                return@withContext id
            }
        }
        
        // If it looks like a URL or starts with @, try direct handle/link resolution
        if (trimmed.startsWith("http") || trimmed.startsWith("@") || trimmed.contains("youtube.com") || trimmed.contains("/")) {
            val urlString = when {
                trimmed.startsWith("http") -> trimmed
                trimmed.startsWith("@") -> "https://www.youtube.com/$trimmed"
                trimmed.startsWith("youtube.com") -> "https://www.$trimmed"
                else -> "https://www.youtube.com/@$trimmed"
            }
            
            var connection: HttpURLConnection? = null
            try {
                val url = URL(urlString)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    readTimeout = 8000
                    connectTimeout = 8000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                }
                connection.connect()
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val html = connection.inputStream.bufferedReader().use { it.readText() }
                    
                    val canonicalMarker = "youtube.com/channel/"
                    if (html.contains(canonicalMarker)) {
                        val sub = html.substringAfter(canonicalMarker)
                        val id = sub.take(24)
                        if (id.startsWith("UC")) {
                            return@withContext id
                        }
                    }
                    
                    val idMarker = "\"channelId\":\""
                    if (html.contains(idMarker)) {
                        val sub = html.substringAfter(idMarker)
                        val id = sub.take(24)
                        if (id.startsWith("UC")) {
                            return@withContext id
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                connection?.disconnect()
            }
        }

        // Search YouTube for channel by name
        val encodedQuery = java.net.URLEncoder.encode(trimmed, "UTF-8")
        val searchUrl = "https://www.youtube.com/results?search_query=$encodedQuery&sp=EgIQAg%253D%253D"
        var searchConn: HttpURLConnection? = null
        try {
            val url = URL(searchUrl)
            searchConn = (url.openConnection() as HttpURLConnection).apply {
                readTimeout = 8000
                connectTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            }
            searchConn.connect()
            if (searchConn.responseCode == HttpURLConnection.HTTP_OK) {
                val html = searchConn.inputStream.bufferedReader().use { it.readText() }
                
                // Try browseId marker first
                val browseMarker = "\"browseId\":\"UC"
                if (html.contains(browseMarker)) {
                    val sub = html.substringAfter(browseMarker)
                    val id = "UC" + sub.take(22)
                    if (id.length == 24) {
                        return@withContext id
                    }
                }
                
                // Try channelId marker
                val idMarker = "\"channelId\":\"UC"
                if (html.contains(idMarker)) {
                    val sub = html.substringAfter(idMarker)
                    val id = "UC" + sub.take(22)
                    if (id.length == 24) {
                        return@withContext id
                    }
                }
                
                // Try channel link marker
                val linkMarker = "/channel/UC"
                if (html.contains(linkMarker)) {
                    val sub = html.substringAfter(linkMarker)
                    val id = "UC" + sub.take(22)
                    if (id.length == 24) {
                        return@withContext id
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            searchConn?.disconnect()
        }
        null
    }

    data class VideoType(val isShort: Boolean, val isLive: Boolean)

    suspend fun determineVideoType(videoId: String): VideoType = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("https://www.youtube.com/shorts/$videoId")
            connection = (url.openConnection() as HttpURLConnection).apply {
                readTimeout = 4000
                connectTimeout = 4000
                requestMethod = "GET"
                instanceFollowRedirects = false // Do not follow redirects so we can detect them via status code
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            }
            connection.connect()
            
            val responseCode = connection.responseCode
            // If it returns 3xx redirect, it's a standard video. If it returns 200, it's a Short.
            val isShort = responseCode == HttpURLConnection.HTTP_OK
            
            var isLive = false
            if (!isShort) {
                var watchConnection: HttpURLConnection? = null
                try {
                    val watchUrl = URL("https://www.youtube.com/watch?v=$videoId")
                    watchConnection = (watchUrl.openConnection() as HttpURLConnection).apply {
                        readTimeout = 4000
                        connectTimeout = 4000
                        requestMethod = "GET"
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    }
                    watchConnection.connect()
                    if (watchConnection.responseCode == HttpURLConnection.HTTP_OK) {
                        val html = watchConnection.inputStream.bufferedReader().use { it.readText() }
                        isLive = html.contains("\"isLive\":true") || html.contains("\"isLiveStream\":true")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    watchConnection?.disconnect()
                }
            }
            VideoType(isShort = isShort, isLive = isLive)
        } catch (e: Exception) {
            VideoType(isShort = false, isLive = false)
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun fetchChannelFeed(channelId: String, channelName: String): List<YouTubeVideo> = withContext(Dispatchers.IO) {
        // Prefer scraper over RSS since RSS only returns ~15 most recent videos
        val scraped = fetchChannelFeedScrape(channelId, channelName)
        if (scraped.isNotEmpty()) {
            return@withContext scraped
        }

        // Fallback to RSS feed
        val urlString = "https://www.youtube.com/feeds/videos.xml?channel_id=$channelId"
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        try {
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                readTimeout = 8000
                connectTimeout = 8000
                requestMethod = "GET"
                doInput = true
            }
            connection.connect()
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = connection.inputStream
                val baseVideos = parseFeedXml(inputStream, channelName)
                if (baseVideos.isNotEmpty()) {
                    return@withContext baseVideos.map { video ->
                        async {
                            if (video.videoUrl.contains("/shorts/")) {
                                video.copy(isShort = true)
                            } else if (video.videoUrl.contains("/watch?v=")) {
                                video.copy(isShort = false)
                            } else {
                                val type = determineVideoType(video.videoId)
                                video.copy(isShort = type.isShort, isLive = type.isLive)
                            }
                        }
                    }.awaitAll()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inputStream?.close()
            connection?.disconnect()
        }
        emptyList()
    }

    private fun parseFeedXml(inputStream: InputStream, channelName: String): List<YouTubeVideo> {
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setInput(inputStream, null)
        
        val videos = mutableListOf<YouTubeVideo>()
        var eventType = parser.eventType
        
        var currentTitle: String? = null
        var currentVideoId: String? = null
        var currentPublished: String? = null
        var currentVideoUrl: String? = null
        var currentIsShort = false
        
        var inEntry = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val name = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (name.equals("entry", ignoreCase = true)) {
                        inEntry = true
                        currentTitle = null
                        currentVideoId = null
                        currentPublished = null
                        currentVideoUrl = null
                        currentIsShort = false
                    } else if (inEntry) {
                        when {
                            name.equals("title", ignoreCase = true) -> {
                                currentTitle = parser.nextText()
                            }
                            name.equals("yt:videoId", ignoreCase = true) || name.equals("videoId", ignoreCase = true) -> {
                                currentVideoId = parser.nextText()
                            }
                            name.equals("id", ignoreCase = true) -> {
                                val fullId = parser.nextText()
                                if (currentVideoId == null && fullId.startsWith("yt:video:")) {
                                    currentVideoId = fullId.substringAfterLast(":")
                                }
                            }
                            name.equals("published", ignoreCase = true) -> {
                                currentPublished = parser.nextText()
                            }
                            name.equals("link", ignoreCase = true) -> {
                                val rel = parser.getAttributeValue(null, "rel")
                                val href = parser.getAttributeValue(null, "href")
                                if (rel == "alternate" && href != null) {
                                    currentVideoUrl = href
                                    if (href.contains("/shorts/")) {
                                        currentIsShort = true
                                    }
                                }
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (name.equals("entry", ignoreCase = true)) {
                        inEntry = false
                        if (currentVideoId != null && currentTitle != null) {
                            val publishedStr = currentPublished ?: ""
                            val timestamp = parsePublishedDate(publishedStr)
                            val friendlyDate = getRelativeTimeSpan(timestamp)
                            val videoUrl = currentVideoUrl ?: "https://www.youtube.com/watch?v=$currentVideoId"
                            val thumbnailUrl = "https://img.youtube.com/vi/$currentVideoId/mqdefault.jpg"
                            
                            videos.add(
                                YouTubeVideo(
                                    videoId = currentVideoId,
                                    title = currentTitle,
                                    channelName = channelName,
                                    publishedDate = friendlyDate,
                                    videoUrl = videoUrl,
                                    thumbnailUrl = thumbnailUrl,
                                    timestampMs = timestamp,
                                    isShort = currentIsShort
                                )
                            )
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return videos
    }

    private fun parsePublishedDate(dateStr: String): Long {
        if (dateStr.isEmpty()) return 0L
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                java.time.OffsetDateTime.parse(dateStr).toInstant().toEpochMilli()
            } else {
                val cleanStr = dateStr.replace("Z", "+00:00")
                val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                format.parse(cleanStr)?.time ?: 0L
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    private fun getRelativeTimeSpan(timeMs: Long): String {
        if (timeMs == 0L) return "Recent"
        val now = System.currentTimeMillis()
        val diff = now - timeMs
        if (diff < 0) return "Just now"
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours hours ago"
            days < 30 -> "$days days ago"
            else -> {
                val format = java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                format.format(java.util.Date(timeMs))
            }
        }
    }

    private fun decodeXmlString(text: String): String {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                android.text.Html.fromHtml(text, android.text.Html.FROM_HTML_MODE_LEGACY).toString()
            } else {
                @Suppress("DEPRECATION")
                android.text.Html.fromHtml(text).toString()
            }
        } catch (e: Exception) {
            text
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
        }
    }

    suspend fun fetchChannelName(channelId: String): String? = withContext(Dispatchers.IO) {
        val urlString = "https://www.youtube.com/feeds/videos.xml?channel_id=$channelId"
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                readTimeout = 8000
                connectTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            }
            connection.connect()
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val xml = connection.inputStream.bufferedReader().use { it.readText() }
                val feedTitle = xml.substringBefore("<entry>")
                val titleMarker = "<title>"
                val titleEndMarker = "</title>"
                if (feedTitle.contains(titleMarker)) {
                    val rawTitle = feedTitle.substringAfter(titleMarker).substringBefore(titleEndMarker).trim()
                    if (rawTitle.isNotEmpty()) {
                        return@withContext decodeXmlString(rawTitle)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }
        null
    }

    suspend fun searchChannelsOnYouTube(topic: String): List<YouTubeChannelSearchInfo> = withContext(Dispatchers.IO) {
        val encodedQuery = java.net.URLEncoder.encode(topic, "UTF-8")
        val searchUrl = "https://www.youtube.com/results?search_query=$encodedQuery&sp=EgIQAg%253D%253D"
        var connection: HttpURLConnection? = null
        try {
            val url = URL(searchUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                readTimeout = 10000
                connectTimeout = 10000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            }
            connection.connect()
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val html = connection.inputStream.bufferedReader().use { it.readText() }
                
                val index = html.indexOf("ytInitialData")
                if (index != -1) {
                    val jsonStart = html.indexOf("{", index)
                    if (jsonStart != -1) {
                        var jsonEnd = html.indexOf(";</script>", jsonStart)
                        if (jsonEnd == -1) {
                            jsonEnd = html.indexOf("</script>", jsonStart)
                        }
                        if (jsonEnd != -1) {
                            var jsonStr = html.substring(jsonStart, jsonEnd).trim()
                            if (jsonStr.endsWith(";")) {
                                jsonStr = jsonStr.substring(0, jsonStr.length - 1).trim()
                            }
                            return@withContext parseChannelSearchJson(jsonStr)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }
        emptyList()
    }

    private fun parseChannelSearchJson(jsonStr: String): List<YouTubeChannelSearchInfo> {
        val results = mutableListOf<YouTubeChannelSearchInfo>()
        try {
            val root = org.json.JSONObject(jsonStr)
            val contentsArray = root.optJSONObject("contents")
                ?.optJSONObject("twoColumnSearchResultsRenderer")
                ?.optJSONObject("primaryContents")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents")
            
            if (contentsArray != null) {
                for (i in 0 until contentsArray.length()) {
                    val sectionItem = contentsArray.optJSONObject(i) ?: continue
                    val itemSection = sectionItem.optJSONObject("itemSectionRenderer") ?: continue
                    val items = itemSection.optJSONArray("contents") ?: continue
                    
                    for (j in 0 until items.length()) {
                        val item = items.optJSONObject(j) ?: continue
                        val channelRenderer = item.optJSONObject("channelRenderer") ?: continue
                        
                        val channelId = channelRenderer.optString("channelId") ?: ""
                        if (channelId.isEmpty()) continue
                        
                        // Extract Title
                        val titleObj = channelRenderer.optJSONObject("title")
                        var title = titleObj?.optString("simpleText") ?: ""
                        if (title.isEmpty()) {
                            val runs = titleObj?.optJSONArray("runs")
                            if (runs != null && runs.length() > 0) {
                                title = runs.optJSONObject(0)?.optString("text") ?: ""
                            }
                        }
                        
                        // Extract Handle/Subscriber Count
                        val subTextObj = channelRenderer.optJSONObject("subscriberCountText")
                        var handle = subTextObj?.optString("simpleText") ?: ""
                        
                        val videoCountObj = channelRenderer.optJSONObject("videoCountText")
                        var subCount = videoCountObj?.optString("simpleText") ?: ""
                        if (subCount.isEmpty()) {
                            val accessibility = videoCountObj?.optJSONObject("accessibility")
                                ?.optJSONObject("accessibilityData")
                            subCount = accessibility?.optString("label") ?: ""
                        }
                        
                        // Extract Thumbnail
                        val thumbnailObj = channelRenderer.optJSONObject("thumbnail")
                        val thumbnails = thumbnailObj?.optJSONArray("thumbnails")
                        var thumbnailUrl = ""
                        if (thumbnails != null && thumbnails.length() > 0) {
                            thumbnailUrl = thumbnails.optJSONObject(0)?.optString("url") ?: ""
                            if (thumbnailUrl.startsWith("//")) {
                                thumbnailUrl = "https:$thumbnailUrl"
                            }
                        }
                        
                        results.add(
                            YouTubeChannelSearchInfo(
                                id = channelId,
                                name = title,
                                handle = handle,
                                subCount = subCount,
                                thumbnailUrl = thumbnailUrl
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return results
    }

    suspend fun searchVideosOnYouTube(topic: String, countryCode: String): List<YouTubeVideo> = withContext(Dispatchers.IO) {
        val encodedQuery = java.net.URLEncoder.encode(topic, "UTF-8")
        val searchUrl = "https://www.youtube.com/results?search_query=$encodedQuery&gl=${countryCode.uppercase().trim()}"
        var connection: HttpURLConnection? = null
        try {
            val url = URL(searchUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                readTimeout = 10000
                connectTimeout = 10000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            }
            connection.connect()
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val html = connection.inputStream.bufferedReader().use { it.readText() }
                
                val index = html.indexOf("ytInitialData")
                if (index != -1) {
                    val jsonStart = html.indexOf("{", index)
                    if (jsonStart != -1) {
                        var jsonEnd = html.indexOf(";</script>", jsonStart)
                        if (jsonEnd == -1) {
                            jsonEnd = html.indexOf("</script>", jsonStart)
                        }
                        if (jsonEnd != -1) {
                            var jsonStr = html.substring(jsonStart, jsonEnd).trim()
                            if (jsonStr.endsWith(";")) {
                                jsonStr = jsonStr.substring(0, jsonStr.length - 1).trim()
                            }
                            return@withContext parseVideoSearchJson(jsonStr)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }
        emptyList()
    }

    private fun parseVideoSearchJson(jsonStr: String): List<YouTubeVideo> {
        val results = mutableListOf<YouTubeVideo>()
        try {
            val root = org.json.JSONObject(jsonStr)
            val contentsArray = root.optJSONObject("contents")
                ?.optJSONObject("twoColumnSearchResultsRenderer")
                ?.optJSONObject("primaryContents")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents")
            
            if (contentsArray != null) {
                for (i in 0 until contentsArray.length()) {
                    val sectionItem = contentsArray.optJSONObject(i) ?: continue
                    val itemSection = sectionItem.optJSONObject("itemSectionRenderer") ?: continue
                    val items = itemSection.optJSONArray("contents") ?: continue
                    
                    for (j in 0 until items.length()) {
                        val item = items.optJSONObject(j) ?: continue
                        val videoRenderer = item.optJSONObject("videoRenderer") ?: continue
                        
                        val videoId = videoRenderer.optString("videoId") ?: ""
                        if (videoId.isEmpty()) continue
                        
                        // Extract Title
                        val titleObj = videoRenderer.optJSONObject("title")
                        var title = titleObj?.optString("simpleText") ?: ""
                        if (title.isEmpty()) {
                            val runs = titleObj?.optJSONArray("runs")
                            if (runs != null && runs.length() > 0) {
                                title = runs.optJSONObject(0)?.optString("text") ?: ""
                            }
                        }
                        
                        // Extract Channel Name
                        val ownerTextObj = videoRenderer.optJSONObject("ownerText")
                        var channelName = ""
                        val ownerRuns = ownerTextObj?.optJSONArray("runs")
                        if (ownerRuns != null && ownerRuns.length() > 0) {
                            channelName = ownerRuns.optJSONObject(0)?.optString("text") ?: ""
                        }
                        
                        // Extract Channel ID (browseId)
                        var channelId = ""
                        if (ownerRuns != null && ownerRuns.length() > 0) {
                            val browseEndpoint = ownerRuns.optJSONObject(0)
                                ?.optJSONObject("navigationEndpoint")
                                ?.optJSONObject("browseEndpoint")
                            channelId = browseEndpoint?.optString("browseId") ?: ""
                        }
                        
                        // Extract Published Date/Time
                        val publishedTextObj = videoRenderer.optJSONObject("publishedTimeText")
                        val publishedDate = publishedTextObj?.optString("simpleText") ?: "Recent"
                        
                        // Extract Thumbnail
                        val thumbnailObj = videoRenderer.optJSONObject("thumbnail")
                        val thumbnails = thumbnailObj?.optJSONArray("thumbnails")
                        var thumbnailUrl = ""
                        if (thumbnails != null && thumbnails.length() > 0) {
                            thumbnailUrl = thumbnails.optJSONObject(0)?.optString("url") ?: ""
                            if (thumbnailUrl.startsWith("//")) {
                                thumbnailUrl = "https:$thumbnailUrl"
                            }
                        }
                        
                        // Extract Duration for Shorts check
                        val lengthTextObj = videoRenderer.optJSONObject("lengthText")
                        val lengthStr = lengthTextObj?.optString("simpleText") ?: ""
                        val isShort = lengthStr.contains("0:") || (lengthStr.length <= 4 && !lengthStr.contains(":"))
                        
                        // Extract Description Snippet
                        var description = ""
                        val descSnippetObj = videoRenderer.optJSONObject("descriptionSnippet")
                        if (descSnippetObj != null) {
                            val runs = descSnippetObj.optJSONArray("runs")
                            if (runs != null && runs.length() > 0) {
                                val sb = java.lang.StringBuilder()
                                for (k in 0 until runs.length()) {
                                    val runItem = runs.optJSONObject(k)
                                    val runText = runItem?.optString("text") ?: ""
                                    sb.append(runText)
                                }
                                description = sb.toString()
                            } else {
                                description = descSnippetObj.optString("simpleText") ?: ""
                            }
                        }
                        
                        val videoUrl = "https://www.youtube.com/watch?v=$videoId"
                        
                        results.add(
                            YouTubeVideo(
                                videoId = videoId,
                                title = title,
                                channelName = channelName,
                                publishedDate = publishedDate,
                                videoUrl = videoUrl,
                                thumbnailUrl = thumbnailUrl,
                                isShort = isShort,
                                channelId = channelId,
                                description = description
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return results
    }

    private fun findVideoRenderers(obj: Any, results: MutableList<org.json.JSONObject>, depth: Int = 0) {
        if (depth > 30) return // Prevent infinite recursion
        when (obj) {
            is org.json.JSONObject -> {
                // Check for richItemRenderer wrapper (new YouTube channel layout)
                val richItem = obj.optJSONObject("richItemRenderer")
                if (richItem != null) {
                    val content = richItem.optJSONObject("content")
                    if (content != null) {
                        val vr = content.optJSONObject("videoRenderer")
                        if (vr != null) {
                            results.add(vr)
                            return
                        }
                        // Also check for reelItemRenderer (Shorts)
                        val rr = content.optJSONObject("reelItemRenderer")
                        if (rr != null) {
                            // Convert reelItemRenderer to a video-like object
                            val videoId = rr.optString("videoId", "")
                            if (videoId.isNotEmpty()) {
                                results.add(rr)
                                return
                            }
                        }
                    }
                }

                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (key == "videoRenderer" || key == "gridVideoRenderer") {
                        val renderer = obj.optJSONObject(key)
                        if (renderer != null) {
                            results.add(renderer)
                        }
                    } else {
                        val value = obj.get(key)
                        findVideoRenderers(value, results, depth + 1)
                    }
                }
            }
            is org.json.JSONArray -> {
                for (i in 0 until obj.length()) {
                    val value = obj.get(i)
                    findVideoRenderers(value, results, depth + 1)
                }
            }
        }
    }

    private fun parseSingleVideoRenderer(videoRenderer: org.json.JSONObject, defaultChannelName: String): YouTubeVideo? {
        val videoId = videoRenderer.optString("videoId") ?: ""
        if (videoId.isEmpty()) return null
        
        val titleObj = videoRenderer.optJSONObject("title")
        var title = titleObj?.optString("simpleText") ?: ""
        if (title.isEmpty()) {
            val runs = titleObj?.optJSONArray("runs")
            if (runs != null && runs.length() > 0) {
                title = runs.optJSONObject(0)?.optString("text") ?: ""
            }
        }
        
        val ownerTextObj = videoRenderer.optJSONObject("ownerText") ?: videoRenderer.optJSONObject("shortBylineText")
        var channelName = defaultChannelName
        val ownerRuns = ownerTextObj?.optJSONArray("runs")
        if (ownerRuns != null && ownerRuns.length() > 0) {
            channelName = ownerRuns.optJSONObject(0)?.optString("text") ?: ""
        }
        
        var channelId = ""
        if (ownerRuns != null && ownerRuns.length() > 0) {
            val browseEndpoint = ownerRuns.optJSONObject(0)
                ?.optJSONObject("navigationEndpoint")
                ?.optJSONObject("browseEndpoint")
            channelId = browseEndpoint?.optString("browseId") ?: ""
        }
        
        val publishedTextObj = videoRenderer.optJSONObject("publishedTimeText")
        val publishedDate = publishedTextObj?.optString("simpleText") ?: "Recent"
        
        val thumbnailObj = videoRenderer.optJSONObject("thumbnail")
        val thumbnails = thumbnailObj?.optJSONArray("thumbnails")
        var thumbnailUrl = ""
        if (thumbnails != null && thumbnails.length() > 0) {
            thumbnailUrl = thumbnails.optJSONObject(thumbnails.length() - 1)?.optString("url") ?: ""
            if (thumbnailUrl.startsWith("//")) {
                thumbnailUrl = "https:$thumbnailUrl"
            }
        }
        if (thumbnailUrl.isEmpty()) {
            thumbnailUrl = "https://img.youtube.com/vi/$videoId/mqdefault.jpg"
        }
        
        val lengthTextObj = videoRenderer.optJSONObject("lengthText")
        val lengthStr = lengthTextObj?.optString("simpleText") ?: ""
        val isShort = lengthStr.contains("0:") || (lengthStr.length <= 4 && !lengthStr.contains(":"))
        
        var description = ""
        val descSnippetObj = videoRenderer.optJSONObject("descriptionSnippet")
        if (descSnippetObj != null) {
            val runs = descSnippetObj.optJSONArray("runs")
            if (runs != null && runs.length() > 0) {
                val sb = java.lang.StringBuilder()
                for (k in 0 until runs.length()) {
                    val runItem = runs.optJSONObject(k)
                    val runText = runItem?.optString("text") ?: ""
                    sb.append(runText)
                }
                description = sb.toString()
            } else {
                description = descSnippetObj.optString("simpleText") ?: ""
            }
        }
        
        val videoUrl = "https://www.youtube.com/watch?v=$videoId"
        
        return YouTubeVideo(
            videoId = videoId,
            title = title,
            channelName = channelName,
            publishedDate = publishedDate,
            videoUrl = videoUrl,
            thumbnailUrl = thumbnailUrl,
            isShort = isShort,
            channelId = channelId,
            description = description
        )
    }

    suspend fun fetchChannelFeedScrape(channelId: String, defaultChannelName: String): List<YouTubeVideo> = withContext(Dispatchers.IO) {
        val urlString = "https://www.youtube.com/channel/$channelId/videos"
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                readTimeout = 15000
                connectTimeout = 15000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            }
            connection.connect()
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val html = connection.inputStream.bufferedReader().use { it.readText() }
                val index = html.indexOf("ytInitialData")
                if (index != -1) {
                    val jsonStart = html.indexOf("{", index)
                    if (jsonStart != -1) {
                        var jsonEnd = html.indexOf(";</script>", jsonStart)
                        if (jsonEnd == -1) {
                            jsonEnd = html.indexOf("</script>", jsonStart)
                        }
                        if (jsonEnd != -1) {
                            var jsonStr = html.substring(jsonStart, jsonEnd).trim()
                            if (jsonStr.endsWith(";")) {
                                jsonStr = jsonStr.substring(0, jsonStr.length - 1).trim()
                            }
                            
                            val root = org.json.JSONObject(jsonStr)
                            
                            // Try targeted extraction from tabs > Videos tab first
                            val tabVideos = extractVideosFromTabs(root, defaultChannelName)
                            if (tabVideos.isNotEmpty()) {
                                return@withContext tabVideos.distinctBy { it.videoId }
                            }
                            
                            // Fallback: recursive search for any video renderers
                            val renderers = mutableListOf<org.json.JSONObject>()
                            findVideoRenderers(root, renderers)
                            
                            val videos = renderers.mapNotNull { parseSingleVideoRenderer(it, defaultChannelName) }
                            return@withContext videos.distinctBy { it.videoId }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }
        emptyList()
    }

    /**
     * Extract videos from the YouTube channel page's tab structure.
     * YouTube stores videos under: contents > twoColumnBrowseResultsRenderer > tabs[] >
     * tabRenderer > content > richGridRenderer > contents[] > richItemRenderer > content > videoRenderer
     */
    private fun extractVideosFromTabs(root: org.json.JSONObject, defaultChannelName: String): List<YouTubeVideo> {
        val results = mutableListOf<YouTubeVideo>()
        try {
            val tabs = root.optJSONObject("contents")
                ?.optJSONObject("twoColumnBrowseResultsRenderer")
                ?.optJSONArray("tabs")
            
            if (tabs == null) return results
            
            // Find the "Videos" tab (usually index 1, but search to be safe)
            for (t in 0 until tabs.length()) {
                val tabObj = tabs.optJSONObject(t) ?: continue
                val tabRenderer = tabObj.optJSONObject("tabRenderer") ?: continue
                
                // Check if this is the Videos tab
                val tabTitle = tabRenderer.optString("title", "")
                val isSelected = tabRenderer.optBoolean("selected", false)
                
                // On /videos URL, the Videos tab should be selected
                // Also match by title for robustness
                if (!isSelected && !tabTitle.equals("Videos", ignoreCase = true) && !tabTitle.equals("Video", ignoreCase = true)) {
                    continue
                }
                
                val tabContent = tabRenderer.optJSONObject("content") ?: continue
                
                // New layout: richGridRenderer
                val richGrid = tabContent.optJSONObject("richGridRenderer")
                if (richGrid != null) {
                    val gridContents = richGrid.optJSONArray("contents")
                    if (gridContents != null) {
                        for (i in 0 until gridContents.length()) {
                            val item = gridContents.optJSONObject(i) ?: continue
                            
                            // richItemRenderer wrapping a videoRenderer
                            val richItemRenderer = item.optJSONObject("richItemRenderer")
                            if (richItemRenderer != null) {
                                val content = richItemRenderer.optJSONObject("content") ?: continue
                                val videoRenderer = content.optJSONObject("videoRenderer")
                                if (videoRenderer != null) {
                                    val video = parseSingleVideoRenderer(videoRenderer, defaultChannelName)
                                    if (video != null) results.add(video)
                                }
                                continue
                            }
                            
                            // continuationItemRenderer (pagination token - skip)
                            if (item.has("continuationItemRenderer")) continue
                            
                            // Direct videoRenderer or gridVideoRenderer
                            val directVideo = item.optJSONObject("videoRenderer")
                            if (directVideo != null) {
                                val video = parseSingleVideoRenderer(directVideo, defaultChannelName)
                                if (video != null) results.add(video)
                                continue
                            }
                            val gridVideo = item.optJSONObject("gridVideoRenderer")
                            if (gridVideo != null) {
                                val video = parseSingleVideoRenderer(gridVideo, defaultChannelName)
                                if (video != null) results.add(video)
                            }
                        }
                    }
                }
                
                // Legacy layout: sectionListRenderer
                val sectionList = tabContent.optJSONObject("sectionListRenderer")
                if (sectionList != null) {
                    val sectionContents = sectionList.optJSONArray("contents")
                    if (sectionContents != null) {
                        for (i in 0 until sectionContents.length()) {
                            val sectionItem = sectionContents.optJSONObject(i) ?: continue
                            val itemSection = sectionItem.optJSONObject("itemSectionRenderer")
                            if (itemSection != null) {
                                val items = itemSection.optJSONArray("contents")
                                if (items != null) {
                                    val renderers = mutableListOf<org.json.JSONObject>()
                                    findVideoRenderers(items, renderers)
                                    renderers.forEach { renderer ->
                                        val video = parseSingleVideoRenderer(renderer, defaultChannelName)
                                        if (video != null) results.add(video)
                                    }
                                }
                            }
                            // Also check for gridRenderer directly
                            val gridRenderer = sectionItem.optJSONObject("gridRenderer")
                            if (gridRenderer != null) {
                                val gridItems = gridRenderer.optJSONArray("items")
                                if (gridItems != null) {
                                    for (j in 0 until gridItems.length()) {
                                        val gridItem = gridItems.optJSONObject(j) ?: continue
                                        val gvr = gridItem.optJSONObject("gridVideoRenderer")
                                        if (gvr != null) {
                                            val video = parseSingleVideoRenderer(gvr, defaultChannelName)
                                            if (video != null) results.add(video)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // If we found videos in this tab, stop looking at other tabs
                if (results.isNotEmpty()) break
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return results
    }
}

