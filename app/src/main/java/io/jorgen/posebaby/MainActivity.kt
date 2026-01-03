package io.jorgen.posebaby

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import io.jorgen.posebaby.ui.theme.PosebabyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PosebabyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PoseBabyApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PoseBabyApp(modifier: Modifier = Modifier, viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val scope = rememberCoroutineScope()
    
    // State collections
    val appState by viewModel.appState.collectAsState()
    val currentMode by viewModel.currentMode.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val targetSkeleton by viewModel.targetSkeleton.collectAsState()
    val recommendation by viewModel.recommendation.collectAsState()
    val tip by viewModel.tip.collectAsState()
    val referenceImageUrl by viewModel.referenceImageUrl.collectAsState()
    val generatedImageResult by viewModel.generatedImageResult.collectAsState()
    val selectedGridOption by viewModel.selectedGridOption.collectAsState()
    val pendingSuggestion by viewModel.pendingSuggestion.collectAsState()
    val displayCropRegion by viewModel.displayCropRegion.collectAsState()
    val isPoseMatched by viewModel.isPoseMatched.collectAsState()
    
    // Settings state
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsState()
    val zhipuApiKey by viewModel.zhipuApiKey.collectAsState()
    val doubaoApiKey by viewModel.doubaoApiKey.collectAsState()
    
    // Request camera permission
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }
    
    // Toast on pose match
    val finalCapturedBitmap by viewModel.finalCapturedBitmap.collectAsState()

    LaunchedEffect(isPoseMatched) {
        if (isPoseMatched) {
            android.widget.Toast.makeText(context, "完美！保持住！", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    if (showSettingsDialog) {
        val hasValidKeys = zhipuApiKey.isNotBlank() && doubaoApiKey.isNotBlank()
        
        SettingsDialog(
            initialZhipuKey = zhipuApiKey,
            initialDoubaoKey = doubaoApiKey,
            onDismiss = { 
                if (hasValidKeys) {
                    viewModel.closeSettings()
                } else {
                    android.widget.Toast.makeText(context, "请先配置 API Key 才能使用", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            onSave = { z, d -> viewModel.saveApiKeys(z, d) }
        )
    }
    
    if (!cameraPermissionState.status.isGranted) {
        // Permission request screen
        Box(
            modifier = modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("请允许相机权限", color = Color.White)
        }
        return
    }
    
    val cameraManager = remember { CameraManager(context) }
    val liveSkeleton by cameraManager.skeletonFlow.collectAsState(initial = null)
    
    // Image picker launcher
    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bitmap = android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            viewModel.captureAndAnalyze(bitmap)
        }
    }


    
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                // === MODE SELECTION (Home) ===
                appState == MainViewModel.AppState.MODE_SELECTION -> {
                    ModeSelectionScreen(
                        onTextModeSelected = { viewModel.selectMode(MainViewModel.Mode.TEXT_MODE) },
                        onImageModeSelected = { viewModel.selectMode(MainViewModel.Mode.IMAGE_MODE) },
                        onSettingsClicked = { viewModel.openSettings() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // === SOURCE SELECTION ===
                appState == MainViewModel.AppState.SOURCE_SELECTION -> {
                    Column {
                        // Header with back button to Mode Selection
                        Row(modifier = Modifier.padding(16.dp)) {
                            IconButton(onClick = { viewModel.goBackToModeSelection() }) {
                                Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                            }
                        }
                        SourceSelectionScreen(
                            onGallerySelected = {
                                viewModel.selectSource(useGallery = true)
                                imagePickerLauncher.launch("image/*")
                            },
                            onCameraSelected = {
                                viewModel.selectSource(useGallery = false)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                
                // === CAMERA PREVIEW (waiting for capture) ===
                appState == MainViewModel.AppState.CAMERA_PREVIEW -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        cameraManager.CameraPreview(modifier = Modifier.fillMaxSize())
                        
                        // Capture button
                        Button(
                            onClick = {
                                scope.launch {
                                    val bitmap = cameraManager.takePicture()
                                    if (bitmap != null) {
                                        viewModel.captureAndAnalyze(bitmap)
                                    }
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                        ) {
                            Text("📸 拍照分析", color = Color.White)
                        }
                        
                        // Back button
                        IconButton(
                            onClick = { viewModel.goBackToSourceSelection() },
                            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                        }
                    }
                }
                
                // === ANALYZING or GENERATING ===
                appState == MainViewModel.AppState.ANALYZING || appState == MainViewModel.AppState.GENERATING -> {
                    LoadingScreen(
                        title = if (appState == MainViewModel.AppState.GENERATING) "正在生成参考图..." else "正在分析场景...",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // === ANALYSIS FAILED ===
                appState == MainViewModel.AppState.ANALYSIS_FAILED -> {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = recommendation ?: "Something went wrong",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = { viewModel.goBackToSourceSelection() }) {
                                Text("重试")
                            }
                        }
                    }
                }
                
                // === POSE SELECTION ===
                appState == MainViewModel.AppState.POSE_SELECTION -> {
                    PoseSelectionScreen(
                        suggestions = suggestions,
                        onPoseSelected = { viewModel.selectPose(it) },
                        onBack = { viewModel.goBackToSourceSelection() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                // === GRID SELECTION (IMAGE_MODE only) ===
                pendingSuggestion != null && selectedGridOption == null -> {
                    GridSelectionScreen(
                        onGridSelected = { option ->
                            viewModel.selectGridOption(option)
                            viewModel.confirmGridAndGenerate()
                        },
                        onBack = { viewModel.backToPoseSelection() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // === PROPS SELECTION ===
                appState == MainViewModel.AppState.PROPS_SELECTION -> {
                    PropsSelectionScreen(
                        onGenerate = { props, custom ->
                            viewModel.generateFinalWithProps(props, custom)
                        },
                        onBack = { viewModel.backToGridSelection() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                // === IMAGE VIEWER (split selection) ===
                // Check if result exists and grid is selected, OR explicitly in IMAGE_VIEWER state (since we set that state now)
                (generatedImageResult != null && selectedGridOption != null) || appState == MainViewModel.AppState.IMAGE_VIEWER -> {
                    if (generatedImageResult != null && selectedGridOption != null) {
                         ImageSplitViewer(
                            imageResult = generatedImageResult!!,
                            gridOption = selectedGridOption!!,
                            onPartSelected = { viewModel.selectImagePart(it) },
                            onManualCropSelected = { viewModel.selectManualCrop(it) },
                            onRegenerate = { viewModel.regenerateImage() },
                            onClose = { viewModel.closeImageViewer() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // === PHOTO PREVIEW ===
                appState == MainViewModel.AppState.PHOTO_PREVIEW && finalCapturedBitmap != null -> {
                    PhotoPreviewScreen(
                        bitmap = finalCapturedBitmap!!,
                        onRetake = { viewModel.discardPhoto() },
                        onSave = {
                             saveBitmapToGallery(context, finalCapturedBitmap!!)
                             viewModel.photoSaved()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                // === TEXT_MODE OVERLAY (skeleton) ===
                currentMode == MainViewModel.Mode.TEXT_MODE && targetSkeleton != null -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Camera with skeleton overlay
                        Box(modifier = Modifier.weight(0.85f)) {
                            cameraManager.CameraPreview(modifier = Modifier.fillMaxSize())
                            OverlayScreen(
                                liveSkeleton = liveSkeleton,
                                targetSkeleton = targetSkeleton,
                                onMatchStatusChange = { viewModel.updateMatchStatus(it) },
                                modifier = Modifier.fillMaxSize()
                            )
                            
                            // Capture Button for Final Photo
                            androidx.compose.material3.FloatingActionButton(
                                onClick = {
                                    scope.launch {
                                        val bitmap = cameraManager.takePicture()
                                        if (bitmap != null) viewModel.reviewPhoto(bitmap)
                                    }
                                },
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                                containerColor = Color.White
                            ) {
                                Icon(Icons.Default.Face, "拍照", tint = Color.Black)
                            }
                        }
                        
                        // Scrollable text area
                        Row(
                            modifier = Modifier
                                .weight(0.15f)
                                .fillMaxWidth()
                                .background(Color.Black)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(0.7f)
                                    .fillMaxHeight()
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = recommendation ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                            }
                            
                            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Gray))
                            
                            Box(
                                modifier = Modifier
                                    .weight(0.3f)
                                    .fillMaxHeight()
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = tip ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Yellow
                                )
                            }
                        }
                    }
                    

                    
                    // Back Button (Top Left)
                    IconButton(
                        onClick = { viewModel.goBackToSourceSelection() },
                        modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                    }
                }
                
                // === IMAGE_MODE OVERLAY (reference image) ===
                currentMode == MainViewModel.Mode.IMAGE_MODE && referenceImageUrl != null -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Camera with image overlay
                        Box(modifier = Modifier.weight(0.85f)) {
                            cameraManager.CameraPreview(modifier = Modifier.fillMaxSize())
                            ImageReferenceOverlay(
                                imageUrl = referenceImageUrl!!,
                                cropRegion = displayCropRegion,
                                modifier = Modifier.fillMaxSize()
                            )
                            
                            // Capture Button for Final Photo
                            androidx.compose.material3.FloatingActionButton(
                                onClick = {
                                    scope.launch {
                                        val bitmap = cameraManager.takePicture()
                                        if (bitmap != null) viewModel.reviewPhoto(bitmap)
                                    }
                                },
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                                containerColor = Color.White
                            ) {
                                Icon(Icons.Default.Face, "拍照", tint = Color.Black)
                            }
                        }
                        
                        // Scrollable text area
                        Row(
                            modifier = Modifier
                                .weight(0.15f)
                                .fillMaxWidth()
                                .background(Color.Black)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(0.7f)
                                    .fillMaxHeight()
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = recommendation ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                            }
                            
                            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.Gray))
                            
                            Box(
                                modifier = Modifier
                                    .weight(0.3f)
                                    .fillMaxHeight()
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = tip ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Yellow
                                )
                            }
                        }
                    }
                    

                    
                    // Back Button (Top Left)
                    IconButton(
                        onClick = { viewModel.backToImageViewer() },
                        modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, "返回选择", tint = Color.White)
                    }
                }
                
                // === FALLBACK ===
                else -> {
                    ModeSelectionScreen(
                        onTextModeSelected = { viewModel.selectMode(MainViewModel.Mode.TEXT_MODE) },
                        onImageModeSelected = { viewModel.selectMode(MainViewModel.Mode.IMAGE_MODE) },
                        onSettingsClicked = { viewModel.openSettings() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
    }


private fun saveBitmapToGallery(context: android.content.Context, bitmap: android.graphics.Bitmap) {
    val filename = "PoseBaby_${System.currentTimeMillis()}.jpg"
    val contentValues = android.content.ContentValues().apply {
        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES)
        }
    }
    val resolver = context.contentResolver
    val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    uri?.let {
        resolver.openOutputStream(it)?.use { stream ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, stream)
        }
        android.widget.Toast.makeText(context, "照片已保存到相册", android.widget.Toast.LENGTH_SHORT).show()
    }
}