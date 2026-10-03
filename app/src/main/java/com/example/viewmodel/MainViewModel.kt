package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiApiClient
import com.example.data.local.AppDatabase
import com.example.data.local.PromptHistoryEntity
import com.example.data.local.PromptRepository
import com.example.data.model.ImageAnalysisResult
import com.example.data.model.PromptDetailLevel
import com.example.data.model.PromptMode
import com.example.data.model.SelectedImageInfo
import com.example.data.preferences.ThemeMode
import com.example.data.preferences.UserPreferencesManager
import com.example.util.ImageUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed interface AppScreen {
    data object Intro : AppScreen
    data object Home : AppScreen
    data object Result : AppScreen
    data object History : AppScreen
    data object Settings : AppScreen
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = PromptRepository(db.promptHistoryDao())
    private val prefs = UserPreferencesManager(application)
    private val apiClient = GeminiApiClient()

    // Screen navigation state
    private val _currentScreen = MutableStateFlow<AppScreen>(
        if (prefs.hasSeenIntro()) AppScreen.Home else AppScreen.Intro
    )
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Image state
    private val _selectedImage = MutableStateFlow<SelectedImageInfo?>(null)
    val selectedImage: StateFlow<SelectedImageInfo?> = _selectedImage.asStateFlow()

    // Configuration
    private val _promptMode = MutableStateFlow(prefs.defaultMode.value)
    val promptMode: StateFlow<PromptMode> = _promptMode.asStateFlow()

    private val _detailLevel = MutableStateFlow(prefs.defaultDetailLevel.value)
    val detailLevel: StateFlow<PromptDetailLevel> = _detailLevel.asStateFlow()

    // Analysis State
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analysisStatusMessage = MutableStateFlow("")
    val analysisStatusMessage: StateFlow<String> = _analysisStatusMessage.asStateFlow()

    private val _currentResult = MutableStateFlow<ImageAnalysisResult?>(null)
    val currentResult: StateFlow<ImageAnalysisResult?> = _currentResult.asStateFlow()

    private val _currentSavedHistoryId = MutableStateFlow<Long?>(null)
    val currentSavedHistoryId: StateFlow<Long?> = _currentSavedHistoryId.asStateFlow()

    private val _currentResultImagePath = MutableStateFlow<String?>(null)
    val currentResultImagePath: StateFlow<String?> = _currentResultImagePath.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Preferences & History
    val themeMode: StateFlow<ThemeMode> = prefs.themeMode
    val customApiKey: StateFlow<String> = prefs.customApiKey

    val historyList: StateFlow<List<PromptHistoryEntity>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var analysisJob: Job? = null

    fun onIntroFinished() {
        prefs.setHasSeenIntro(true)
        _currentScreen.value = AppScreen.Home
    }

    fun navigateTo(screen: AppScreen) {
        _errorMessage.value = null
        _currentScreen.value = screen
    }

    fun handleBackPress(): Boolean {
        return when (_currentScreen.value) {
            AppScreen.Result, AppScreen.History, AppScreen.Settings -> {
                _currentScreen.value = AppScreen.Home
                true
            }
            AppScreen.Intro -> {
                onIntroFinished()
                true
            }
            AppScreen.Home -> false
        }
    }

    fun onImageSelected(uri: Uri) {
        viewModelScope.launch {
            try {
                _errorMessage.value = null
                val info = ImageUtils.processSelectedUri(getApplication(), uri)
                _selectedImage.value = info
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load selected image: ${e.message}"
            }
        }
    }

    fun onRemoveImage() {
        _selectedImage.value = null
        _errorMessage.value = null
    }

    fun setPromptMode(mode: PromptMode) {
        _promptMode.value = mode
    }

    fun setDetailLevel(level: PromptDetailLevel) {
        _detailLevel.value = level
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun generatePrompt() {
        val imageInfo = _selectedImage.value ?: run {
            _errorMessage.value = "Please select an image first."
            return
        }

        val localPath = imageInfo.localCachedPath ?: run {
            _errorMessage.value = "Image file is unavailable. Please reselect."
            return
        }

        val imageFile = File(localPath)
        if (!imageFile.exists()) {
            _errorMessage.value = "Image file could not be found."
            return
        }

        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            _isAnalyzing.value = true
            _errorMessage.value = null

            // Realistic multi-stage progress status
            launch {
                val stages = listOf(
                    "Parsing visual geometry & composition...",
                    "Analyzing light vectors & chromatic palette...",
                    "Detecting subject identity & textures...",
                    "Synthesizing recreation prompt..."
                )
                for (stage in stages) {
                    if (!_isAnalyzing.value) break
                    _analysisStatusMessage.value = stage
                    delay(700)
                }
            }

            val result = apiClient.analyzeImageAndGeneratePrompt(
                imageFile = imageFile,
                mode = _promptMode.value,
                detailLevel = _detailLevel.value,
                customApiKey = prefs.customApiKey.value
            )

            _isAnalyzing.value = false

            result.onSuccess { analysis ->
                _currentResult.value = analysis
                _currentResultImagePath.value = localPath

                // Save to local history automatically
                saveResultToHistory(analysis, localPath)

                _currentScreen.value = AppScreen.Result
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "Analysis failed. Please try again."
            }
        }
    }

    private suspend fun saveResultToHistory(analysis: ImageAnalysisResult, imagePath: String) {
        try {
            val permanentPath = ImageUtils.saveImageForHistory(getApplication(), imagePath)
            val entity = PromptHistoryEntity(
                imagePath = permanentPath,
                timestamp = System.currentTimeMillis(),
                masterPrompt = analysis.masterPrompt,
                negativePrompt = analysis.negativePrompt,
                mode = _promptMode.value.displayName,
                detailLevel = _detailLevel.value.displayName,
                subject = analysis.subject,
                composition = analysis.composition,
                lighting = analysis.lighting,
                colors = analysis.colors,
                camera = analysis.camera,
                environment = analysis.environment,
                style = analysis.style,
                analysis = analysis.analysis
            )
            val newId = repository.savePrompt(entity)
            _currentSavedHistoryId.value = newId
            _currentResultImagePath.value = permanentPath
        } catch (_: Exception) {
            // non-fatal if history save fails
        }
    }

    fun toggleSaveCurrentPrompt() {
        val analysis = _currentResult.value ?: return
        val currentId = _currentSavedHistoryId.value

        viewModelScope.launch {
            if (currentId != null) {
                // Delete from history
                repository.deleteById(currentId)
                _currentSavedHistoryId.value = null
            } else {
                // Re-save
                val imgPath = _currentResultImagePath.value ?: _selectedImage.value?.localCachedPath ?: ""
                val permanentPath = if (imgPath.isNotBlank()) {
                    ImageUtils.saveImageForHistory(getApplication(), imgPath)
                } else ""

                val entity = PromptHistoryEntity(
                    imagePath = permanentPath,
                    timestamp = System.currentTimeMillis(),
                    masterPrompt = analysis.masterPrompt,
                    negativePrompt = analysis.negativePrompt,
                    mode = _promptMode.value.displayName,
                    detailLevel = _detailLevel.value.displayName,
                    subject = analysis.subject,
                    composition = analysis.composition,
                    lighting = analysis.lighting,
                    colors = analysis.colors,
                    camera = analysis.camera,
                    environment = analysis.environment,
                    style = analysis.style,
                    analysis = analysis.analysis
                )
                val newId = repository.savePrompt(entity)
                _currentSavedHistoryId.value = newId
            }
        }
    }

    fun selectHistoryItem(item: PromptHistoryEntity) {
        val analysis = ImageAnalysisResult(
            masterPrompt = item.masterPrompt,
            negativePrompt = item.negativePrompt,
            analysis = item.analysis,
            style = item.style,
            subject = item.subject,
            composition = item.composition,
            lighting = item.lighting,
            colors = item.colors,
            camera = item.camera,
            environment = item.environment,
            isPersonDetected = item.subject.contains("person", ignoreCase = true) ||
                    item.masterPrompt.contains("facial structure", ignoreCase = true)
        )
        _currentResult.value = analysis
        _currentResultImagePath.value = item.imagePath
        _currentSavedHistoryId.value = item.id
        _promptMode.value = PromptMode.fromName(item.mode)
        _detailLevel.value = PromptDetailLevel.fromName(item.detailLevel)
        _currentScreen.value = AppScreen.Result
    }

    fun deleteHistoryItem(item: PromptHistoryEntity) {
        viewModelScope.launch {
            repository.delete(item)
            if (_currentSavedHistoryId.value == item.id) {
                _currentSavedHistoryId.value = null
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
            _currentSavedHistoryId.value = null
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.setThemeMode(mode)
    }

    fun setDefaultMode(mode: PromptMode) {
        prefs.setDefaultMode(mode)
        _promptMode.value = mode
    }

    fun setDefaultDetailLevel(level: PromptDetailLevel) {
        prefs.setDefaultDetailLevel(level)
        _detailLevel.value = level
    }

    fun setCustomApiKey(key: String) {
        prefs.setCustomApiKey(key)
    }
}
