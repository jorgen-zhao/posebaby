package io.jorgen.posebaby

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


import android.app.Application
import androidx.lifecycle.AndroidViewModel

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val apiKeyManager = ApiKeyManager(application)

    enum class Mode {
        TEXT_MODE,
        IMAGE_MODE
    }

    /**
     * App state for flow control
     */
    enum class AppState {
        MODE_SELECTION,      // Initial: choose 文本模式 or 图片模式
        SOURCE_SELECTION,    // Choose gallery or camera
        CAMERA_PREVIEW,      // Taking photo
        ANALYZING,           // AI analyzing the image
        POSE_SELECTION,      // Choosing a pose suggestion
        GRID_SELECTION,      // IMAGE_MODE: choosing grid layout
        GENERATING,          // IMAGE_MODE: generating reference image
        IMAGE_VIEWER,        // IMAGE_MODE: viewing split images
        OVERLAY_ACTIVE,      // Showing overlay (skeleton or image)
        ANALYSIS_FAILED,     // Analysis failed
        PHOTO_PREVIEW,       // Reviewing taken photo
        PROPS_SELECTION      // Picking props
    }

    /**
     * Grid layout options for image generation
     */
    enum class GridOption(val rows: Int, val cols: Int, val label: String, val promptText: String) {
        SINGLE(1, 1, "1×1 单图", "单张全身照，展示一个完整姿势"),
        HORIZONTAL_2(1, 2, "1×2 横排", "横排两张，展示两个不同姿势"),
        GRID_4(2, 2, "2×2 四宫格", "等宽等高四宫格，一张图片分成4个部分，每个部分不同的姿势或者表情"),
        GRID_9(3, 3, "3×3 九宫格", "等宽等高九宫格，一张图片分成9个部分，每个部分不同的姿势或者表情");

        val totalParts: Int get() = rows * cols
    }

    // Repositories are now mutable to support API key updates
    private var repository = ZRepository()
    private var zhipuRepository = ZhipuRepository(apiKeyManager.getZhipuKey())
    private var doubaoRepository = DoubaoRepository(apiKeyManager.getDoubaoKey())
    
    // API Keys state
    private val _zhipuApiKey = MutableStateFlow(apiKeyManager.getZhipuKey())
    val zhipuApiKey: StateFlow<String> = _zhipuApiKey
    
    private val _doubaoApiKey = MutableStateFlow(apiKeyManager.getDoubaoKey())
    val doubaoApiKey: StateFlow<String> = _doubaoApiKey
    
    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog

    // App state management
    private val _appState = MutableStateFlow(AppState.MODE_SELECTION)
    val appState: StateFlow<AppState> = _appState

    private val _currentMode = MutableStateFlow(Mode.TEXT_MODE)
    val currentMode: StateFlow<Mode> = _currentMode

    // Whether user chose gallery (true) or camera (false)
    private val _useGallery = MutableStateFlow(false)
    val useGallery: StateFlow<Boolean> = _useGallery
    
    // Captured photo for final review
    private val _finalCapturedBitmap = MutableStateFlow<Bitmap?>(null)
    val finalCapturedBitmap: StateFlow<Bitmap?> = _finalCapturedBitmap

    // Selected grid option for image generation
    private val _selectedGridOption = MutableStateFlow<GridOption?>(null)
    val selectedGridOption: StateFlow<GridOption?> = _selectedGridOption

    // Display crop region for cropped overlay (percentage 0-1)
    private val _displayCropRegion = MutableStateFlow<CropRegion?>(null)
    val displayCropRegion: StateFlow<CropRegion?> = _displayCropRegion
    
    // Stored params for regeneration/back navigation
    private var _lastSuggestion: ZRepository.PoseSuggestion? = null
    private var _lastSelectedProps: List<String> = emptyList()
    private var _lastCustomText: String = ""

    private val _referenceImageUrl = MutableStateFlow<String?>(null)
    val referenceImageUrl: StateFlow<String?> = _referenceImageUrl

    // For Doubao: generated image result with URL and size for 9-part splitting
    private val _generatedImageResult = MutableStateFlow<ImageResult?>(null)
    val generatedImageResult: StateFlow<ImageResult?> = _generatedImageResult

    // Store previous image result for back navigation
    private val _previousImageResult = MutableStateFlow<ImageResult?>(null)

    // Selected part index (0-8) for reference overlay
    private val _selectedPartIndex = MutableStateFlow<Int?>(null)
    val selectedPartIndex: StateFlow<Int?> = _selectedPartIndex

    // Selected crop region for manual crop
    private val _selectedCropRegion = MutableStateFlow<CropRegion?>(null)
    val selectedCropRegion: StateFlow<CropRegion?> = _selectedCropRegion

    // Pending suggestion waiting for grid selection
    private val _pendingSuggestion = MutableStateFlow<ZRepository.PoseSuggestion?>(null)
    val pendingSuggestion: StateFlow<ZRepository.PoseSuggestion?> = _pendingSuggestion

    private val _sceneDescription = MutableStateFlow<String?>(null)
    val sceneDescription: StateFlow<String?> = _sceneDescription

    // Store the captured bitmap for IMAGE_MODE generation
    private val _capturedBitmapForGeneration = MutableStateFlow<Bitmap?>(null)

    private val _targetSkeleton = MutableStateFlow<BodySkeleton?>(null)
    val targetSkeleton: StateFlow<BodySkeleton?> = _targetSkeleton

    private val _analysisResult = MutableStateFlow<String?>(null)
    val analysisResult: StateFlow<String?> = _analysisResult

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing

    private val _recommendation = MutableStateFlow<String?>(null)
    val recommendation: StateFlow<String?> = _recommendation

    private val _tip = MutableStateFlow<String?>(null)
    val tip: StateFlow<String?> = _tip

    private val _suggestions = MutableStateFlow<List<ZRepository.PoseSuggestion>>(emptyList())
    val suggestions: StateFlow<List<ZRepository.PoseSuggestion>> = _suggestions

    private val _isPoseMatched = MutableStateFlow(false)
    val isPoseMatched: StateFlow<Boolean> = _isPoseMatched

    private val _capturedImage = MutableStateFlow<Bitmap?>(null)
    val capturedImage: StateFlow<Bitmap?> = _capturedImage
    
    init {
        if (!apiKeyManager.hasValidKeys()) {
            _showSettingsDialog.value = true
        }
    }
    
    fun openSettings() {
        _zhipuApiKey.value = apiKeyManager.getZhipuKey()
        _doubaoApiKey.value = apiKeyManager.getDoubaoKey()
        _showSettingsDialog.value = true
    }
    
    fun closeSettings() {
        _showSettingsDialog.value = false
    }
    
    fun saveApiKeys(zhipuKey: String, doubaoKey: String) {
        val trimmedZhipu = zhipuKey.trim()
        val trimmedDoubao = doubaoKey.trim()
        
        Log.d("MainViewModel", "Saving API Keys. Zhipu: ${trimmedZhipu.take(6)}..., Doubao: ${trimmedDoubao.take(6)}...")
        
        apiKeyManager.setZhipuKey(trimmedZhipu)
        apiKeyManager.setDoubaoKey(trimmedDoubao)
        
        // Update live state
        _zhipuApiKey.value = trimmedZhipu
        _doubaoApiKey.value = trimmedDoubao
        
        // Re-initialize repositories with new keys
        zhipuRepository = ZhipuRepository(trimmedZhipu)
        doubaoRepository = DoubaoRepository(trimmedDoubao)
        repository = ZRepository(trimmedZhipu)
        
        _showSettingsDialog.value = false
    }

    fun captureAndAnalyze(originalBitmap: Bitmap) {
        viewModelScope.launch {
            _appState.value = AppState.ANALYZING
            _isAnalyzing.value = true
            _capturedImage.value = originalBitmap // Freeze the frame
            try {
                // Resize logic: 1024px width, maintain aspect ratio
                val resizedBitmap = withContext(Dispatchers.Default) {
                    val width = originalBitmap.width
                    val height = originalBitmap.height
                    val targetWidth = 1024
                    
                    if (width > targetWidth) {
                        val aspectRatio = height.toFloat() / width.toFloat()
                        val targetHeight = (targetWidth * aspectRatio).toInt()
                        Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
                    } else {
                        originalBitmap
                    }
                }

                val newSuggestions = repository.analyzeScene(resizedBitmap)
                _suggestions.value = newSuggestions
                _appState.value = AppState.POSE_SELECTION
                
                // Store the resized bitmap for IMAGE_MODE generation
                _capturedBitmapForGeneration.value = resizedBitmap
                
                if (newSuggestions.isNotEmpty()) {
                    // Extract scene description from first suggestion for IMAGE_MODE
                    // This will be used in assembleCogViewPrompt
                    val sceneDesc = extractSceneDescription(newSuggestions)
                    _sceneDescription.value = sceneDesc
                    
                    Log.d("MainViewModel", "Scene description extracted: $sceneDesc")
                    
                    // Do not auto-select. Let user select from UI.
                    // selectPose(newSuggestions[0]) 
                    
                    // Store raw result for debugging if needed, or serialize list
                    _analysisResult.value = "Found ${newSuggestions.size} suggestions"
                } else {
                    _recommendation.value = "未找到合适的姿势建议，请尝试其他角度。"
                    _tip.value = ""
                    _appState.value = AppState.ANALYSIS_FAILED
                }
                
                Log.d("MainViewModel", "Analysis Result: Found ${newSuggestions.size} poses")

            } catch (e: Exception) {
                Log.i("MainViewModel", "Analysis failed", e)
                _analysisResult.value = "Error: ${e.message}"
                _recommendation.value = "分析失败，请检查网络连接或重试。"
                _appState.value = AppState.ANALYSIS_FAILED
            } finally {
                _isAnalyzing.value = false
            }
        }
    }


    /**
     * Extract scene description from suggestions for image generation.
     * Analyzes common elements in suggestions to build a scene description.
     */
    private fun extractSceneDescription(suggestions: List<ZRepository.PoseSuggestion>): String {
        // Try to extract scene keywords from the suggestions
        // Look for common location/lighting/mood descriptions
        val descriptions = suggestions.map { it.description }
        
        // Simple heuristic: use the description of the first suggestion
        // In a more advanced version, we could use NLP to extract common scene elements
        val firstDesc = descriptions.firstOrNull() ?: ""
        
        // Extract scene-related keywords (simplified)
        return when {
            firstDesc.contains("墙") || firstDesc.contains("wall") -> "红砖墙背景的现代都市环境"
            firstDesc.contains("咖啡") || firstDesc.contains("cafe") -> "温馨咖啡馆，柔和的灯光"
            firstDesc.contains("窗") || firstDesc.contains("window") -> "明亮的室内空间，大窗户采光"
            firstDesc.contains("街") || firstDesc.contains("street") -> "城市街道，自然光线"
            else -> "现代都市环境，自然光线"
        }
    }

    fun selectPose(suggestion: ZRepository.PoseSuggestion) {
        when (_currentMode.value) {
            Mode.TEXT_MODE -> {
                // Original text-based skeleton overlay mode
                _recommendation.value = "${suggestion.title}\n${suggestion.description}"
                _tip.value = suggestion.photographerTip

                // Map category to PoseLibrary keys
                val libraryKey = when (suggestion.technicalCategory) {
                    "standing_straight", "arms_crossed", "hands_in_pockets" -> "standing_confident"
                    "sitting_casual", "crouching" -> "sitting_casual"
                    "leaning_left", "leaning_right" -> "leaning"
                    "walking_away" -> "standing_confident" // Fallback
                    else -> "standing_confident"
                }

                _targetSkeleton.value = PoseLibrary.getScaledPose(libraryKey, 480, 640)
                _capturedImage.value = null // Clear frozen frame to return to live view for guiding
                _isPoseMatched.value = false // Reset match status on new selection
                _appState.value = AppState.OVERLAY_ACTIVE
            }
            Mode.IMAGE_MODE -> {
                // Store the suggestion and wait for grid selection
                _pendingSuggestion.value = suggestion
                _appState.value = AppState.GRID_SELECTION
            }
        }
    }

    /**
     * After grid selection, generate the image
     */
    fun confirmGridAndGenerate() {
        val suggestion = _pendingSuggestion.value
        if (suggestion != null && _selectedGridOption.value != null) {
            _appState.value = AppState.PROPS_SELECTION
        }
    }

    fun generateFinalWithProps(selectedProps: List<String>, customText: String) {
        val suggestion = _pendingSuggestion.value
        if (suggestion != null && _selectedGridOption.value != null) {
            // Save params for regeneration
            _lastSuggestion = suggestion
            _lastSelectedProps = selectedProps
            _lastCustomText = customText
            
            _appState.value = AppState.GENERATING
            generateReferenceImage(suggestion, selectedProps, customText)
            _pendingSuggestion.value = null
        }
    }

    fun updateMatchStatus(isMatched: Boolean) {
        _isPoseMatched.value = isMatched
    }

    // ============ State Transition Functions ============
    
    fun selectMode(mode: Mode) {
        _currentMode.value = mode
        _appState.value = AppState.SOURCE_SELECTION
    }
    
    fun selectSource(useGallery: Boolean) {
        _useGallery.value = useGallery
        if (!useGallery) {
            _appState.value = AppState.CAMERA_PREVIEW
        }
    }
    
    fun startAnalyzing() {
        _appState.value = AppState.ANALYZING
    }
    
    fun goBackToModeSelection() {
        clearAnalysisResult()
        _appState.value = AppState.MODE_SELECTION
    }
    
    fun goBackToSourceSelection() {
        clearAnalysisResult()
        _appState.value = AppState.SOURCE_SELECTION
    }

    fun clearAnalysisResult() {
        _analysisResult.value = null
        _targetSkeleton.value = null
        _recommendation.value = null
        _tip.value = null
        _suggestions.value = emptyList()
        _isPoseMatched.value = false
        _capturedImage.value = null
        _referenceImageUrl.value = null
        _sceneDescription.value = null
        _generatedImageResult.value = null
        _selectedPartIndex.value = null
        _capturedBitmapForGeneration.value = null
        _selectedGridOption.value = null
        _pendingSuggestion.value = null
        _displayCropRegion.value = null
    }

    fun backToGridSelection() {
        _selectedGridOption.value = null
        _appState.value = AppState.GRID_SELECTION
    }

    fun backToPoseSelection() {
        _pendingSuggestion.value = null
        _selectedGridOption.value = null
        _appState.value = AppState.POSE_SELECTION
    }



    fun switchMode(mode: Mode) {
        _currentMode.value = mode
        clearAnalysisResult()
        _appState.value = AppState.SOURCE_SELECTION
    }

    /**
     * Assemble a detailed prompt for image generation.
     * Combines scene description, pose, and grid layout preference.
     */
    fun assembleCogViewPrompt(
        sceneDescription: String,
        selectedStyle: ZRepository.PoseSuggestion,
        props: List<String> = emptyList(),
        customProp: String = ""
    ): String {
        val gridOption = _selectedGridOption.value ?: GridOption.GRID_9
        val propsString = (props + listOf(customProp).filter { it.isNotEmpty() }).joinToString(", ")
        val propsPrompt = if (propsString.isNotEmpty()) "，手持或使用以下道具：$propsString" else ""
        
        return """
一张专业的时尚人像摄影作品。
场景环境：$sceneDescription，真实的环境细节，自然光线。
拍摄主体：年轻亚洲女性model，时尚妆容，自然表情。
动作姿态：${selectedStyle.description}$propsPrompt
摄影风格：${selectedStyle.title}，杂志级别摄影，高分辨率8K画质，完美构图，专业色调，细节丰富，景深效果自然。
排列方式：${gridOption.promptText}。
        """.trimIndent()
    }

    /**
     * Set the grid option before generating image
     */
    fun selectGridOption(option: GridOption) {
        _selectedGridOption.value = option
    }

    /**
     * Generate reference image for IMAGE_MODE using Doubao API.
     * Called after user selects a pose suggestion.
     * Generates a 2048x2048 image that can be split into 9 parts.
     */
    fun generateReferenceImage(
        suggestion: ZRepository.PoseSuggestion,
        props: List<String> = emptyList(),
        customText: String = ""
    ) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                val bitmap = _capturedBitmapForGeneration.value
                if (bitmap == null) {
                    Log.e("MainViewModel", "No captured bitmap for image generation")
                    _recommendation.value = "请先拍摄或上传照片"
                    return@launch
                }
                
                // Assemble the prompt
                val sceneDesc = _sceneDescription.value ?: "现代都市环境"
                val prompt = assembleCogViewPrompt(sceneDesc, suggestion, props, customText)
                
                Log.d("MainViewModel", "Generating image with Doubao, prompt: $prompt")
                
                // Generate image using Doubao
                doubaoRepository.generateReferenceImage(prompt, bitmap).collect { result ->
                    _generatedImageResult.value = result
                    if (result != null) {
                        Log.d("MainViewModel", "Generated image: ${result.url}, size: ${result.width}x${result.height}")
                        // Set recommendation and tip for display
                        _recommendation.value = "${suggestion.title}\n${suggestion.description}"
                        _tip.value = suggestion.photographerTip
                        _capturedImage.value = null // Clear frozen frame
                        _appState.value = AppState.IMAGE_VIEWER
                    } else {
                        _recommendation.value = "图片生成失败，请重试"
                        _tip.value = ""
                        _appState.value = AppState.GRID_SELECTION // Go back to grid selection on failure?
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error generating reference image", e)
                _recommendation.value = "生成失败: ${e.message}"
                _tip.value = ""
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    /**
     * Select one of the image parts as the final reference overlay.
     * Calculates the crop region based on grid layout and selected part.
     */
    fun selectImagePart(partIndex: Int) {
        _selectedPartIndex.value = partIndex
        val result = _generatedImageResult.value
        val gridOption = _selectedGridOption.value
        
        if (result != null && gridOption != null) {
            // Save for back navigation
            _previousImageResult.value = result
            _referenceImageUrl.value = result.url
            
            // Calculate crop region based on grid layout
            val row = partIndex / gridOption.cols
            val col = partIndex % gridOption.cols
            
            val cellWidth = 1f / gridOption.cols
            val cellHeight = 1f / gridOption.rows
            
            _displayCropRegion.value = CropRegion(
                left = col * cellWidth,
                top = row * cellHeight,
                right = (col + 1) * cellWidth,
                bottom = (row + 1) * cellHeight
            )
            
            _appState.value = AppState.OVERLAY_ACTIVE
        }
        _generatedImageResult.value = null
    }

    /**
     * Select a manual crop region as the reference overlay.
     */
    fun selectManualCrop(cropRegion: CropRegion) {
        val result = _generatedImageResult.value
        if (result != null) {
            _previousImageResult.value = result
            _selectedCropRegion.value = cropRegion
            _displayCropRegion.value = cropRegion
            _referenceImageUrl.value = result.url
            _appState.value = AppState.OVERLAY_ACTIVE
        }
        _generatedImageResult.value = null
    }

    /**
     * Close the image split viewer without selecting a part.
     */
    /**
     * Close the image split viewer without selecting a part.
     * Go back to grid selection.
     */
    fun closeImageViewer() {
        _generatedImageResult.value = null
        // Restore pending suggestion for props screen
        if (_pendingSuggestion.value == null) {
            _pendingSuggestion.value = _lastSuggestion
        }
        _appState.value = AppState.PROPS_SELECTION
    }
    
    fun regenerateImage() {
        if (_lastSuggestion != null) {
            _appState.value = AppState.GENERATING
            generateReferenceImage(_lastSuggestion!!, _lastSelectedProps, _lastCustomText)
            // No need to clear _pendingSuggestion as it's already null or handled
        }
    }

    /**
     * Go back to the image viewer from the reference overlay.
     * Restores the previous generated image result.
     */
    fun backToImageViewer() {
        val previousResult = _previousImageResult.value
        if (previousResult != null) {
            _generatedImageResult.value = previousResult
            _referenceImageUrl.value = null // Clear overlay
            _selectedPartIndex.value = null
            _selectedCropRegion.value = null
            _displayCropRegion.value = null
            _appState.value = AppState.IMAGE_VIEWER
        } else {
             _appState.value = AppState.GRID_SELECTION
        }
    }

    fun reviewPhoto(bitmap: Bitmap) {
        _finalCapturedBitmap.value = bitmap
        _appState.value = AppState.PHOTO_PREVIEW
    }

    fun discardPhoto() {
        _finalCapturedBitmap.value = null
        _appState.value = AppState.OVERLAY_ACTIVE
    }

    fun photoSaved() {
        _finalCapturedBitmap.value = null
        _appState.value = AppState.OVERLAY_ACTIVE
    }
}
