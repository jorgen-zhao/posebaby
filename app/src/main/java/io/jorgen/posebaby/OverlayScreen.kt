package io.jorgen.posebaby

import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun OverlayScreen(
    liveSkeleton: BodySkeleton?,
    targetSkeleton: BodySkeleton?,
    recommendation: String? = null,
    tip: String? = null,
    onRefresh: () -> Unit = {},
    onMatchStatusChange: (Boolean) -> Unit = {},
    imageWidth: Int = 480,
    imageHeight: Int = 640,
    modifier: Modifier = Modifier
) {
    // Pinch-to-zoom state
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    
    androidx.compose.foundation.layout.Box(modifier = modifier.fillMaxSize()) {
        // Zoomable skeleton canvas
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 3f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                        translationY = offsetY
                    }
            ) {
                val safeImageWidth = if (imageWidth > 0) imageWidth.toFloat() else 1f
                val safeImageHeight = if (imageHeight > 0) imageHeight.toFloat() else 1f
                
                val scaleX = size.width / safeImageWidth
                val scaleY = size.height / safeImageHeight

                // Draw Target Skeleton (Semi-transparent Blue, dashed)
                if (targetSkeleton != null) {
                    drawSkeleton(
                        skeleton = targetSkeleton,
                        color = Color.Blue.copy(alpha = 0.5f),
                        scaleX = scaleX,
                        scaleY = scaleY,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }

                // Draw Live Skeleton and calculate match
                if (liveSkeleton != null) {
                    val isMatch = if (targetSkeleton != null) {
                        calculateMatch(liveSkeleton, targetSkeleton) > 80.0
                    } else {
                        false
                    }
                    onMatchStatusChange(isMatch)
                }
            }
        }
        
        // Zoom hint
        if (scale == 1f) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                androidx.compose.material3.Text(
                    text = "👆 双指缩放调整",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // Text Overlay
        if (!recommendation.isNullOrEmpty() || !tip.isNullOrEmpty()) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(16.dp)
                    .align(androidx.compose.ui.Alignment.BottomCenter)
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        if (!recommendation.isNullOrEmpty()) {
                            androidx.compose.material3.Text(
                                text = recommendation,
                                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        if (!tip.isNullOrEmpty()) {
                            androidx.compose.material3.Text(
                                text = tip,
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                color = Color.Yellow
                            )
                        }
                    }
                    
                    androidx.compose.material3.IconButton(onClick = onRefresh) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Pose",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawSkeleton(
    skeleton: BodySkeleton,
    color: Color,
    scaleX: Float,
    scaleY: Float,
    pathEffect: PathEffect? = null
) {
    val strokeWidth = 8f
    val jointRadius = 10f

    fun getScaledPoint(point: PointF?): Offset? {
        return point?.let {
            Offset(it.x * scaleX, it.y * scaleY)
        }
    }

    val joints = listOf(
        skeleton.leftShoulder, skeleton.rightShoulder,
        skeleton.leftElbow, skeleton.rightElbow,
        skeleton.leftWrist, skeleton.rightWrist,
        skeleton.leftHip, skeleton.rightHip,
        skeleton.leftKnee, skeleton.rightKnee,
        skeleton.leftAnkle, skeleton.rightAnkle
    ).map { getScaledPoint(it) }

    // Draw Joints
    joints.forEach { offset ->
        if (offset != null) {
            drawCircle(
                color = color,
                radius = jointRadius,
                center = offset
            )
        }
    }

    // Connect joints
    fun connect(start: PointF?, end: PointF?) {
        val startOffset = getScaledPoint(start)
        val endOffset = getScaledPoint(end)
        if (startOffset != null && endOffset != null) {
            drawLine(
                color = color,
                start = startOffset,
                end = endOffset,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
                pathEffect = pathEffect
            )
        }
    }

    // Arms
    connect(skeleton.leftShoulder, skeleton.leftElbow)
    connect(skeleton.leftElbow, skeleton.leftWrist)
    connect(skeleton.rightShoulder, skeleton.rightElbow)
    connect(skeleton.rightElbow, skeleton.rightWrist)

    // Shoulders
    connect(skeleton.leftShoulder, skeleton.rightShoulder)

    // Body
    connect(skeleton.leftShoulder, skeleton.leftHip)
    connect(skeleton.rightShoulder, skeleton.rightHip)
    connect(skeleton.leftHip, skeleton.rightHip)

    // Legs
    connect(skeleton.leftHip, skeleton.leftKnee)
    connect(skeleton.leftKnee, skeleton.leftAnkle)
    connect(skeleton.rightHip, skeleton.rightKnee)
    connect(skeleton.rightKnee, skeleton.rightAnkle)
}

/**
 * Calculates a match percentage between live and target skeletons.
 * Returns a value between 0.0 and 100.0.
 * We use Cosine Similarity of limb vectors to determine pose similarity.
 */
fun calculateMatch(live: BodySkeleton, target: BodySkeleton): Double {
    // Define limbs as pairs of joints
    // Define limbs as pairs of accessors
    val limbs = listOf(
        Pair({ s: BodySkeleton -> s.leftShoulder }, { s: BodySkeleton -> s.leftElbow }),
        Pair({ s: BodySkeleton -> s.leftElbow }, { s: BodySkeleton -> s.leftWrist }),
        Pair({ s: BodySkeleton -> s.rightShoulder }, { s: BodySkeleton -> s.rightElbow }),
        Pair({ s: BodySkeleton -> s.rightElbow }, { s: BodySkeleton -> s.rightWrist }),
        Pair({ s: BodySkeleton -> s.leftHip }, { s: BodySkeleton -> s.leftKnee }),
        Pair({ s: BodySkeleton -> s.leftKnee }, { s: BodySkeleton -> s.leftAnkle }),
        Pair({ s: BodySkeleton -> s.rightHip }, { s: BodySkeleton -> s.rightKnee }),
        Pair({ s: BodySkeleton -> s.rightKnee }, { s: BodySkeleton -> s.rightAnkle }),
        // Optional: Body alignment
        Pair({ s: BodySkeleton -> s.leftShoulder }, { s: BodySkeleton -> s.leftHip }),
        Pair({ s: BodySkeleton -> s.rightShoulder }, { s: BodySkeleton -> s.rightHip })
    )

    var totalSimilarity = 0.0
    var count = 0

    for ((startProp, endProp) in limbs) {
        val liveStart = startProp(live)
        val liveEnd = endProp(live)
        val targetStart = startProp(target)
        val targetEnd = endProp(target)

        if (liveStart != null && liveEnd != null && targetStart != null && targetEnd != null) {
            val liveVectorKey = PointF(liveEnd.x - liveStart.x, liveEnd.y - liveStart.y)
            val targetVectorKey = PointF(targetEnd.x - targetStart.x, targetEnd.y - targetStart.y)

            val sim = cosineSimilarity(liveVectorKey, targetVectorKey)
            // Cosine similarity is -1 to 1. Map to 0 to 1 for percentage calculation
            // We care about direction, so 1 is perfect match, -1 is opposite.
            // Let's use (sim + 1) / 2 to map to 0..1?
            // Actually, for pose matching, we want 1.0 (0 degrees diff) to be 100%.
            // 0.0 (90 degrees) is 0%? Or maybe we just take the raw cosine if > 0?
            // If the user's arm is 90 degrees wrong, that's bad.
            // Let's treat standard cosine: 1.0 = 100%, 0.0 = 50%, -1.0 = 0%
            // Or simpler: max(0, sim) * 100
            
            // Standard approach for pose: 
            // We want angle difference. 
            // sim = cos(theta). Match score decreases as theta increases.
            // Let's normalize to 0..100 based on similarity.
            
            totalSimilarity += sim
            count++
        }
    }

    if (count == 0) return 0.0

    val averageSimilarity = totalSimilarity / count
    // Map average cosine (-1 to 1) to 0-100?
    // Doing (avg + 1) / 2 * 100 gives 0-100 linear.
    // However, we strictly want > 80% to mean "very close". 
    // If average cosine is 0.8, that means average angle is acos(0.8) ~= 36 degrees diff.
    // If average cosine is 0.95, acos(0.95) ~= 18 degrees diff.
    // Let's simply return averageCosine * 100. If it's negative, 0.
    
    return max(0.0, averageSimilarity * 100)
}

private fun cosineSimilarity(v1: PointF, v2: PointF): Double {
    val dot = v1.x * v2.x + v1.y * v2.y
    val mag1 = sqrt(v1.x * v1.x + v1.y * v1.y)
    val mag2 = sqrt(v2.x * v2.x + v2.y * v2.y)
    
    if (mag1 == 0f || mag2 == 0f) return 0.0
    
    return (dot / (mag1 * mag2)).toDouble()
}
