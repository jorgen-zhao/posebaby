package io.jorgen.posebaby

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Overlay that displays a semi-transparent reference image.
 * Supports displaying only a cropped portion when cropRegion is specified.
 */
@Composable
fun ImageReferenceOverlay(
    imageUrl: String,
    cropRegion: CropRegion? = null,
    modifier: Modifier = Modifier
) {
    var opacity by remember { mutableStateOf(0.5f) }

    Box(modifier = modifier.fillMaxSize()) {
        // Reference image (cropped if region specified)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            if (cropRegion != null) {
                // Show only the cropped portion
                val cropWidth = cropRegion.right - cropRegion.left
                val cropHeight = cropRegion.bottom - cropRegion.top
                
                // Calculate scale to fill viewport with crop region
                // Calculate scale to fill viewport with crop region
                val cropScaleX = 1f / cropWidth
                val cropScaleY = 1f / cropHeight
                val cropScale = minOf(cropScaleX, cropScaleY)
                
                // Calculate center of crop region
                val cropCenterX = (cropRegion.left + cropRegion.right) / 2f
                val cropCenterY = (cropRegion.top + cropRegion.bottom) / 2f
                
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Reference Pose Image (Cropped)",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = cropScale
                            scaleY = cropScale
                            transformOrigin = TransformOrigin.Center
                            translationX = (0.5f - cropCenterX) * size.width * cropScale
                            translationY = (0.5f - cropCenterY) * size.height * cropScale
                            clip = true
                        },
                    alpha = opacity,
                    contentScale = ContentScale.Fit
                )
            } else {
                // Show full image
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Reference Pose Image",
                    modifier = Modifier.fillMaxSize(),
                    alpha = opacity,
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Opacity slider at the bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "透明度: ${(opacity * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Slider(
                value = opacity,
                onValueChange = { opacity = it },
                valueRange = 0f..1f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
