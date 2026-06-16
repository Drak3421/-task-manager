package com.example.myapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import coil.compose.rememberAsyncImagePainter
import java.io.File

@Composable
fun UserAvatar(
    username: String,
    modifier: Modifier = Modifier,
    viewModel: TaskViewModel,
    fontSize: TextUnit,
    textColor: Color = Color.White,
    fallbackBrush: Brush? = null,
    fallbackColor: Color? = null
) {
    val context = LocalContext.current
    val trigger by viewModel.profileUpdateTrigger.collectAsState()
    
    // Construct local path for user avatar
    val avatarFile = File(context.filesDir, "profile_pics/${username.lowercase()}.jpg")
    
    Box(
        modifier = modifier
            .clip(CircleShape)
            .let {
                if (avatarFile.exists()) {
                    it
                } else if (fallbackBrush != null) {
                    it.background(fallbackBrush)
                } else if (fallbackColor != null) {
                    it.background(fallbackColor)
                } else {
                    it.background(MaterialTheme.colorScheme.secondary)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (avatarFile.exists()) {
            val painter = rememberAsyncImagePainter(
                model = coil.request.ImageRequest.Builder(context)
                    .data(avatarFile)
                    .memoryCacheKey("${avatarFile.absolutePath}_$trigger")
                    .build()
            )
            Image(
                painter = painter,
                contentDescription = "Profile Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = username.take(2).uppercase(),
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize
            )
        }
    }
}
