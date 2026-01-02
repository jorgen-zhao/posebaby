package io.jorgen.posebaby

import android.content.Context
import android.graphics.PointF
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import android.graphics.Bitmap
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

data class BodySkeleton(
    val leftShoulder: PointF?,
    val rightShoulder: PointF?,
    val leftElbow: PointF?,
    val rightElbow: PointF?,
    val leftWrist: PointF?,
    val rightWrist: PointF?,
    val leftHip: PointF?,
    val rightHip: PointF?,
    val leftKnee: PointF?,
    val rightKnee: PointF?,
    val leftAnkle: PointF?,
    val rightAnkle: PointF?
)

class CameraManager(private val context: Context) {

    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val poseDetector = PoseDetection.getClient(
        PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
    )

    private val imageCapture: ImageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()

    private val _skeletonFlow = MutableSharedFlow<BodySkeleton>(replay = 1)
    val skeletonFlow: Flow<BodySkeleton> = _skeletonFlow

    @Composable
    fun CameraPreview(
        modifier: Modifier = Modifier
    ) {
        val lifecycleOwner = LocalLifecycleOwner.current
        val context = LocalContext.current
        val previewView = remember { PreviewView(context) }

        LaunchedEffect(lifecycleOwner) {
            val cameraProvider = ProcessCameraProvider.getInstance(context).await()

            val preview = CameraPreview.Builder()
                .build()
                .apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(analysisExecutor, PoseAnalyzer { skeleton ->
                _skeletonFlow.tryEmit(skeleton)
            })

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis,
                    imageCapture
                )
            } catch (exc: Exception) {
                Log.e("CameraManager", "Use case binding failed", exc)
            }
        }

        AndroidView(
            factory = { previewView },
            modifier = modifier
        )
    }

    suspend fun takePicture(): Bitmap? {
        return suspendCoroutine { continuation ->
            imageCapture.takePicture(
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        try {
                            val bitmap = image.toBitmapInternal()
                            continuation.resume(bitmap)
                        } catch (e: Exception) {
                            Log.e("CameraManager", "Error converting image to bitmap", e)
                            continuation.resume(null)
                        } finally {
                            image.close()
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e("CameraManager", "Image capture failed", exception)
                        continuation.resumeWithException(exception)
                    }
                }
            )
        }
    }

    private fun ImageProxy.toBitmapInternal(): Bitmap {
        val buffer = planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private inner class PoseAnalyzer(
        private val onSkeletonDetected: (BodySkeleton) -> Unit
    ) : ImageAnalysis.Analyzer {

        @OptIn(ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                poseDetector.process(image)
                    .addOnSuccessListener { pose ->
                        val skeleton = extractSkeleton(pose)
                        onSkeletonDetected(skeleton)
                    }
                    .addOnFailureListener { e ->
                        Log.e("CameraManager", "Pose detection failed", e)
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }

        private fun extractSkeleton(pose: Pose): BodySkeleton {
            fun getPoint(landmarkType: Int): PointF? {
                val landmark = pose.getPoseLandmark(landmarkType)
                return if (landmark != null && landmark.inFrameLikelihood > 0.5f) {
                    landmark.position
                } else {
                    null
                }
            }

            return BodySkeleton(
                leftShoulder = getPoint(PoseLandmark.LEFT_SHOULDER),
                rightShoulder = getPoint(PoseLandmark.RIGHT_SHOULDER),
                leftElbow = getPoint(PoseLandmark.LEFT_ELBOW),
                rightElbow = getPoint(PoseLandmark.RIGHT_ELBOW),
                leftWrist = getPoint(PoseLandmark.LEFT_WRIST),
                rightWrist = getPoint(PoseLandmark.RIGHT_WRIST),
                leftHip = getPoint(PoseLandmark.LEFT_HIP),
                rightHip = getPoint(PoseLandmark.RIGHT_HIP),
                leftKnee = getPoint(PoseLandmark.LEFT_KNEE),
                rightKnee = getPoint(PoseLandmark.RIGHT_KNEE),
                leftAnkle = getPoint(PoseLandmark.LEFT_ANKLE),
                rightAnkle = getPoint(PoseLandmark.RIGHT_ANKLE)
            )
        }
    }
}

suspend fun <T> ListenableFuture<T>.await(): T {
    return suspendCoroutine { continuation ->
        addListener({
            try {
                continuation.resume(get())
            } catch (exc: Exception) {
                continuation.resumeWithException(exc)
            }
        }, { command -> command.run() })
    }
}
