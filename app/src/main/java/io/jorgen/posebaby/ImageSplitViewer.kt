package io.jorgen.posebaby

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Represents a crop region for one of the 9 parts of the image.
 */
data class ImagePart(
    val index: Int,
    val row: Int,
    val col: Int,
    val label: String
)

/**
 * Custom crop region defined by user (as percentage 0-1)
 */
data class CropRegion(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

enum class SplitMode {
    AUTO,
    MANUAL,
    PREVIEW
}

/**
 * Image split viewer with proper cropping using graphicsLayer
 * Supports dynamic grid sizes: 1x1, 1x2, 2x2, 3x3
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageSplitViewer(
    imageResult: ImageResult,
    gridOption: MainViewModel.GridOption,
    onPartSelected: (Int) -> Unit,
    onManualCropSelected: (CropRegion) -> Unit = {},
    onRegenerate: () -> Unit = {},
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var splitMode by remember { mutableStateOf(SplitMode.AUTO) }
    var pendingCropRegion by remember { mutableStateOf<CropRegion?>(null) }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (splitMode) {
            SplitMode.AUTO -> AutoSplitView(
                imageResult = imageResult,
                gridOption = gridOption,
                onPartSelected = onPartSelected,
                onSwitchToManual = { splitMode = SplitMode.MANUAL },
                onRegenerate = onRegenerate,
                onBack = onClose
            )
            SplitMode.MANUAL -> ManualSplitView(
                imageResult = imageResult,
                onCropConfirmed = { region ->
                    pendingCropRegion = region
                    splitMode = SplitMode.PREVIEW
                },
                onBack = { splitMode = SplitMode.AUTO }
            )
            SplitMode.PREVIEW -> {
                pendingCropRegion?.let { region ->
                    CropPreviewView(
                        imageResult = imageResult,
                        cropRegion = region,
                        onConfirm = { onManualCropSelected(region) },
                        onBack = { splitMode = SplitMode.MANUAL }
                    )
                }
            }
        }
        

    }
}

/**
 * Auto split view - 3x3 grid using graphicsLayer for proper cropping
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AutoSplitView(
    imageResult: ImageResult,
    gridOption: MainViewModel.GridOption,
    onPartSelected: (Int) -> Unit,
    onSwitchToManual: () -> Unit,
    onRegenerate: () -> Unit,
    onBack: () -> Unit
) {
    // Generate parts dynamically based on grid option
    val parts = remember(gridOption) {
        val list = mutableListOf(ImagePart(-1, -1, -1, "原图"))
        var index = 0
        for (row in 0 until gridOption.rows) {
            for (col in 0 until gridOption.cols) {
                list.add(ImagePart(index, row, col, "${index + 1}/${gridOption.totalParts}"))
                index++
            }
        }
        list
    }
    
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { parts.size }
    )
    
    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { it } // Force unique key per page
        ) { page ->
            val part = parts[page]
            
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (part.index == -1) {
                        // Full image - show complete grid
                        AsyncImage(
                            model = imageResult.url,
                            contentDescription = "Full ${gridOption.label}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        // CROPPED PART: Use graphicsLayer with clip=true
                        // Scale by grid dimensions and translate to show correct cell
                        val scaleFactorX = gridOption.cols.toFloat()
                        val scaleFactorY = gridOption.rows.toFloat()
                        
                        // Calculate center column/row for translation
                        val centerCol = (gridOption.cols - 1) / 2f
                        val centerRow = (gridOption.rows - 1) / 2f
                        
                        AsyncImage(
                            model = imageResult.url,
                            contentDescription = "Part ${part.index + 1}",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    // Scale up by grid dimensions
                                    scaleX = scaleFactorX
                                    scaleY = scaleFactorY
                                    
                                    transformOrigin = TransformOrigin.Center
                                    
                                    // Translate to show the correct cell
                                    translationX = (centerCol - part.col) * size.width
                                    translationY = (centerRow - part.row) * size.height
                                    
                                    clip = true
                                },
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
        
        // Back Button
        IconButton(
             onClick = onBack,
             modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
        ) {
             Icon(androidx.compose.material.icons.Icons.Default.ArrowBack, "Back", tint = Color.White)
        }

        // Top: Label
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = parts[pagerState.currentPage].label,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Text(
                text = "← 滑动查看 →",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        // Bottom: Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                parts.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(if (pagerState.currentPage == index) 10.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == index) Color.White 
                                else Color.White.copy(alpha = 0.4f)
                            )
                    )
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Regenerate
                Button(
                   onClick = onRegenerate,
                   colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                ) {
                    Text("继续生成")
                }

                Button(
                    onClick = onSwitchToManual,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242))
                ) {
                    Text("✂️ 裁剪")
                }
                
                if (pagerState.currentPage > 0) {
                    Button(onClick = { onPartSelected(parts[pagerState.currentPage].index) }) {
                        Text("选择此姿势")
                    }
                }
            }
        }
    }
}

/**
 * Manual split view - user drags crop box
 */
@Composable
private fun ManualSplitView(
    imageResult: ImageResult,
    onCropConfirmed: (CropRegion) -> Unit,
    onBack: () -> Unit
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    
    var cropLeft by remember { mutableFloatStateOf(0.25f) }
    var cropTop by remember { mutableFloatStateOf(0.25f) }
    var cropRight by remember { mutableFloatStateOf(0.75f) }
    var cropBottom by remember { mutableFloatStateOf(0.75f) }
    
    var draggingHandle by remember { mutableStateOf<String?>(null) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(16.dp)
                .align(Alignment.Center)
                .onSizeChanged { containerSize = it }
        ) {
            AsyncImage(
                model = imageResult.url,
                contentDescription = "Image for manual crop",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            
            if (containerSize != IntSize.Zero) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val x = offset.x / containerSize.width
                                    val y = offset.y / containerSize.height
                                    val threshold = 0.08f
                                    
                                    draggingHandle = when {
                                        kotlin.math.abs(x - cropLeft) < threshold && y in (cropTop - threshold)..(cropBottom + threshold) -> "left"
                                        kotlin.math.abs(x - cropRight) < threshold && y in (cropTop - threshold)..(cropBottom + threshold) -> "right"
                                        kotlin.math.abs(y - cropTop) < threshold && x in (cropLeft - threshold)..(cropRight + threshold) -> "top"
                                        kotlin.math.abs(y - cropBottom) < threshold && x in (cropLeft - threshold)..(cropRight + threshold) -> "bottom"
                                        x in cropLeft..cropRight && y in cropTop..cropBottom -> "move"
                                        else -> null
                                    }
                                },
                                onDragEnd = { draggingHandle = null },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val dx = dragAmount.x / containerSize.width
                                    val dy = dragAmount.y / containerSize.height
                                    
                                    when (draggingHandle) {
                                        "left" -> cropLeft = (cropLeft + dx).coerceIn(0f, cropRight - 0.1f)
                                        "right" -> cropRight = (cropRight + dx).coerceIn(cropLeft + 0.1f, 1f)
                                        "top" -> cropTop = (cropTop + dy).coerceIn(0f, cropBottom - 0.1f)
                                        "bottom" -> cropBottom = (cropBottom + dy).coerceIn(cropTop + 0.1f, 1f)
                                        "move" -> {
                                            val width = cropRight - cropLeft
                                            val height = cropBottom - cropTop
                                            val newLeft = (cropLeft + dx).coerceIn(0f, 1f - width)
                                            val newTop = (cropTop + dy).coerceIn(0f, 1f - height)
                                            cropLeft = newLeft
                                            cropRight = newLeft + width
                                            cropTop = newTop
                                            cropBottom = newTop + height
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    
                    // Dim outside
                    drawRect(Color.Black.copy(alpha = 0.6f), Offset.Zero, androidx.compose.ui.geometry.Size(w, cropTop * h))
                    drawRect(Color.Black.copy(alpha = 0.6f), Offset(0f, cropBottom * h), androidx.compose.ui.geometry.Size(w, h - cropBottom * h))
                    drawRect(Color.Black.copy(alpha = 0.6f), Offset(0f, cropTop * h), androidx.compose.ui.geometry.Size(cropLeft * w, (cropBottom - cropTop) * h))
                    drawRect(Color.Black.copy(alpha = 0.6f), Offset(cropRight * w, cropTop * h), androidx.compose.ui.geometry.Size((1 - cropRight) * w, (cropBottom - cropTop) * h))
                    
                    // Border
                    drawRect(
                        Color.White,
                        Offset(cropLeft * w, cropTop * h),
                        androidx.compose.ui.geometry.Size((cropRight - cropLeft) * w, (cropBottom - cropTop) * h),
                        style = Stroke(3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                    )
                    
                    // Corner handles
                    listOf(
                        Offset(cropLeft * w, cropTop * h),
                        Offset(cropRight * w, cropTop * h),
                        Offset(cropLeft * w, cropBottom * h),
                        Offset(cropRight * w, cropBottom * h)
                    ).forEach {
                        drawCircle(Color.White, 14f, it)
                        drawCircle(Color(0xFFE91E63), 10f, it)
                    }
                }
            }
        }
        
        // Back Button
        IconButton(
             onClick = onBack,
             modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
        ) {
             Icon(androidx.compose.material.icons.Icons.Default.ArrowBack, "Back", tint = Color.White)
        }

        // Top
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("手动裁剪", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Text("拖动边框调整裁剪区域", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
        }
        
        // Bottom
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(onClick = { onCropConfirmed(CropRegion(cropLeft, cropTop, cropRight, cropBottom)) }) {
                Text("预览裁剪")
            }
        }
    }
}

/**
 * Preview the cropped result - FIXED to show only cropped region
 */
@Composable
private fun CropPreviewView(
    imageResult: ImageResult,
    cropRegion: CropRegion,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    val cropWidth = cropRegion.right - cropRegion.left
    val cropHeight = cropRegion.bottom - cropRegion.top
    
    // Calculate scale needed to fill the viewport with just the crop region
    // If crop is 50% of image, we need 2x scale
    val cropScaleX = 1f / cropWidth
    val cropScaleY = 1f / cropHeight
    val cropScale = minOf(cropScaleX, cropScaleY)
    
    // Calculate center of crop region (0-1)
    val cropCenterX = (cropRegion.left + cropRegion.right) / 2f
    val cropCenterY = (cropRegion.top + cropRegion.bottom) / 2f
    
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(cropWidth / cropHeight) // Match crop aspect ratio
                .padding(16.dp)
                .align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageResult.url,
                contentDescription = "Cropped Preview",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Scale up to show only the cropped region
                        scaleX = cropScale
                        scaleY = cropScale
                        
                        // Transform from center
                        transformOrigin = TransformOrigin.Center
                        
                        // Translate so crop region center is at viewport center
                        translationX = (0.5f - cropCenterX) * size.width * cropScale
                        translationY = (0.5f - cropCenterY) * size.height * cropScale
                        
                        clip = true
                    },
                contentScale = ContentScale.Fit
            )
        }
        
        // Back Button
        IconButton(
             onClick = onBack,
             modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
        ) {
             Icon(androidx.compose.material.icons.Icons.Default.ArrowBack, "Back", tint = Color.White)
        }

        // Top
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("裁剪预览", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Text("确认选择此区域作为参考", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
        }
        
        // Bottom
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(onClick = onConfirm) {
                Text("确认选择")
            }
        }
    }
}
