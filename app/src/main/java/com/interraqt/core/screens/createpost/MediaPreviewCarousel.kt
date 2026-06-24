package com.interraqt.core.screens.createpost

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MediaPreviewCarousel(
    selectedMedia: List<MediaAttachment>,
    onMediaClick: (Int) -> Unit,
    onRemoveMedia: (MediaAttachment) -> Unit
) {
    val context = LocalContext.current

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp) 
    ) {
        items(selectedMedia) { media ->
            Box(
                modifier = Modifier
                    .width(84.dp)
                    .height(112.dp) 
            ) {
                if (media.isVideo) {
                    var videoThumbnail by remember { mutableStateOf<Bitmap?>(null) }
                    var durationText by remember { mutableStateOf("") }

                    LaunchedEffect(media.uri) {
                        withContext(Dispatchers.IO) {
                            try {
                                val retriever = MediaMetadataRetriever()
                                retriever.setDataSource(context, media.uri)
                                videoThumbnail = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                
                                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                                if (durationMs > 0) {
                                    val seconds = (durationMs / 1000) % 60
                                    val minutes = (durationMs / 1000) / 60
                                    durationText = String.format("%d:%02d", minutes, seconds)
                                }
                                retriever.release()
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                    }

                    AsyncImage(
                        model = videoThumbnail,
                        contentDescription = "Video Preview",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp)) 
                            .clickable { onMediaClick(selectedMedia.indexOf(media)) },
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.2f))
                            .clickable { onMediaClick(selectedMedia.indexOf(media)) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.PlayCircle, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(32.dp))
                    }

                    if (durationText.isNotEmpty()) {
                        Text(
                            text = durationText,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(media.uri).crossfade(true).build(),
                        contentDescription = "Media Preview",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp)) 
                            .clickable { onMediaClick(selectedMedia.indexOf(media)) },
                        contentScale = ContentScale.Crop
                    )
                }
                
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable { onRemoveMedia(media) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
