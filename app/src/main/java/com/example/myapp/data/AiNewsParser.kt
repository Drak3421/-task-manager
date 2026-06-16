package com.example.myapp.data

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.Date
import java.text.SimpleDateFormat

import kotlinx.serialization.Serializable

@Serializable
data class AiNewsArticle(
    val title: String,
    val link: String,
    val pubDate: String,
    val source: String,
    val timestampMs: Long
)

object AiNewsParser {

    suspend fun fetchAiNews(query: String): List<AiNewsArticle> = withContext(Dispatchers.IO) {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val urlString = "https://news.google.com/rss/search?q=$encodedQuery&hl=en-US&gl=US&ceid=US:en"
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
            
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = connection.inputStream
                parseNewsXml(inputStream)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        } finally {
            inputStream?.close()
            connection?.disconnect()
        }
    }

    private fun parseNewsXml(inputStream: InputStream): List<AiNewsArticle> {
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setInput(inputStream, null)
        
        val articles = mutableListOf<AiNewsArticle>()
        var eventType = parser.eventType
        
        var currentTitle: String? = null
        var currentLink: String? = null
        var currentPubDate: String? = null
        var currentSource: String? = null
        
        var inItem = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val name = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (name.equals("item", ignoreCase = true)) {
                        inItem = true
                        currentTitle = null
                        currentLink = null
                        currentPubDate = null
                        currentSource = null
                    } else if (inItem) {
                        when {
                            name.equals("title", ignoreCase = true) -> {
                                currentTitle = parser.nextText()
                            }
                            name.equals("link", ignoreCase = true) -> {
                                currentLink = parser.nextText()
                            }
                            name.equals("pubDate", ignoreCase = true) -> {
                                currentPubDate = parser.nextText()
                            }
                            name.equals("source", ignoreCase = true) -> {
                                currentSource = parser.nextText()
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (name.equals("item", ignoreCase = true)) {
                        inItem = false
                        if (currentLink != null && currentTitle != null) {
                            val pubDateStr = currentPubDate ?: ""
                            val timestamp = parsePubDate(pubDateStr)
                            val friendlyDate = getRelativeTimeSpan(timestamp)
                            
                            val rawTitle = currentTitle
                            val sourceName = currentSource ?: rawTitle.substringAfterLast(" - ").trim()
                            
                            val cleanTitle = if (currentSource != null && rawTitle.endsWith(" - $currentSource")) {
                                rawTitle.substringBeforeLast(" - $currentSource").trim()
                            } else if (rawTitle.contains(" - ")) {
                                rawTitle.substringBeforeLast(" - ").trim()
                            } else {
                                rawTitle
                            }

                            articles.add(
                                AiNewsArticle(
                                    title = cleanTitle,
                                    link = currentLink,
                                    pubDate = friendlyDate,
                                    source = sourceName,
                                    timestampMs = timestamp
                                )
                            )
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return articles
    }

    private fun parsePubDate(dateStr: String): Long {
        if (dateStr.isEmpty()) return 0L
        val formats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss z",
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (pattern in formats) {
            try {
                val format = SimpleDateFormat(pattern, Locale.US)
                val date = format.parse(dateStr)
                if (date != null) return date.time
            } catch (e: Exception) {
                // Try next
            }
        }
        return 0L
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
                val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                format.format(Date(timeMs))
            }
        }
    }
}
