package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.preferences.ThemeMode
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.IntroSplashScreen
import com.example.ui.screens.PromptResultScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDarkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> systemDark
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        RefPromptApp(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun RefPromptApp(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val selectedImage by viewModel.selectedImage.collectAsStateWithLifecycle()
    val promptMode by viewModel.promptMode.collectAsStateWithLifecycle()
    val detailLevel by viewModel.detailLevel.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val analysisStatusMessage by viewModel.analysisStatusMessage.collectAsStateWithLifecycle()
    val currentResult by viewModel.currentResult.collectAsStateWithLifecycle()
    val currentResultImagePath by viewModel.currentResultImagePath.collectAsStateWithLifecycle()
    val currentSavedHistoryId by viewModel.currentSavedHistoryId.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val historyList by viewModel.historyList.collectAsStateWithLifecycle()
    val currentTheme by viewModel.themeMode.collectAsStateWithLifecycle()
    val customApiKey by viewModel.customApiKey.collectAsStateWithLifecycle()

    BackHandler(enabled = currentScreen != AppScreen.Home && currentScreen != AppScreen.Intro) {
        viewModel.handleBackPress()
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            AppScreen.Intro -> {
                IntroSplashScreen(
                    onFinished = { viewModel.onIntroFinished() },
                    isReturningUser = historyList.isNotEmpty()
                )
            }

            AppScreen.Home -> {
                HomeScreen(
                    selectedImage = selectedImage,
                    promptMode = promptMode,
                    detailLevel = detailLevel,
                    isAnalyzing = isAnalyzing,
                    analysisStatusMessage = analysisStatusMessage,
                    errorMessage = errorMessage,
                    historyCount = historyList.size,
                    onImageSelected = { uri -> viewModel.onImageSelected(uri) },
                    onRemoveImage = { viewModel.onRemoveImage() },
                    onPromptModeChanged = { mode -> viewModel.setPromptMode(mode) },
                    onDetailLevelChanged = { level -> viewModel.setDetailLevel(level) },
                    onGeneratePrompt = { viewModel.generatePrompt() },
                    onOpenHistory = { viewModel.navigateTo(AppScreen.History) },
                    onOpenSettings = { viewModel.navigateTo(AppScreen.Settings) },
                    onClearError = { viewModel.clearError() }
                )
            }

            AppScreen.Result -> {
                currentResult?.let { result ->
                    PromptResultScreen(
                        analysisResult = result,
                        imagePath = currentResultImagePath,
                        mode = promptMode,
                        detailLevel = detailLevel,
                        isSaved = currentSavedHistoryId != null,
                        onBack = { viewModel.navigateTo(AppScreen.Home) },
                        onRegenerate = { viewModel.generatePrompt() },
                        onToggleSave = { viewModel.toggleSaveCurrentPrompt() }
                    )
                } ?: run {
                    // Fallback to Home if result is empty
                    viewModel.navigateTo(AppScreen.Home)
                }
            }

            AppScreen.History -> {
                HistoryScreen(
                    historyList = historyList,
                    onBack = { viewModel.navigateTo(AppScreen.Home) },
                    onSelectPrompt = { item -> viewModel.selectHistoryItem(item) },
                    onDeletePrompt = { item -> viewModel.deleteHistoryItem(item) },
                    onClearAll = { viewModel.clearAllHistory() }
                )
            }

            AppScreen.Settings -> {
                SettingsScreen(
                    currentTheme = currentTheme,
                    defaultMode = promptMode,
                    defaultDetailLevel = detailLevel,
                    customApiKey = customApiKey,
                    onBack = { viewModel.navigateTo(AppScreen.Home) },
                    onThemeChanged = { mode -> viewModel.setThemeMode(mode) },
                    onDefaultModeChanged = { mode -> viewModel.setDefaultMode(mode) },
                    onDefaultDetailLevelChanged = { level -> viewModel.setDefaultDetailLevel(level) },
                    onCustomApiKeyChanged = { key -> viewModel.setCustomApiKey(key) },
                    onClearHistory = { viewModel.clearAllHistory() }
                )
            }
        }
    }
}
