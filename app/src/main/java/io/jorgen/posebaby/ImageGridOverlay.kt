package io.jorgen.posebaby

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

/**
 * 2x2 grid displaying 4 generated reference images with floating animation.
 * User can tap one to select it as the overlay reference.
 */
@Composable
fun ImageGridOverlay(
    imageUrls: List<String>,
    onImageSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "选择参考图片",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // 2x2 Grid
            val rows = imageUrls.chunked(2)
            rows.forEachIndexed { rowIndex, rowImages ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    rowImages.forEachIndexed { colIndex, url ->
                        val index = rowIndex * 2 + colIndex
                        AnimatedImageCard(
                            imageUrl = url,
                            index = index,
                            onClick = { onImageSelected(url) }
                        )
                    }
                }
            }
            
            Text(
                text = "点击图片选择作为参考姿势",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun AnimatedImageCard(
    imageUrl: String,
    index: Int,
    onClick: () -> Unit
) {
    // Animation for floating in effect
    val offsetY = remember { Animatable(100f) }
    val alpha = remember { Animatable(0f) }
    
    LaunchedEffect(imageUrl) {
        // Staggered animation based on index
        delay(index * 150L)
        offsetY.animateTo(0f, animationSpec = tween(500))
    }
    
    LaunchedEffect(imageUrl) {
        delay(index * 150L)
        alpha.animateTo(1f, animationSpec = tween(500))
    }
    
    Box(
        modifier = Modifier
            .size(150.dp)
            .graphicsLayer(
                translationY = offsetY.value,
                alpha = alpha.value
            )
            .clip(RoundedCornerShape(12.dp))
            .background(Color.DarkGray)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Reference Image ${index + 1}",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Index badge
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
        }
    }
}
