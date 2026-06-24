package com.oshoplayer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oshoplayer.app.ui.PlayerScreen
import com.oshoplayer.app.ui.theme.OshoPlayerTheme
import com.oshoplayer.app.viewmodel.AudioMixerViewModel

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.attributes = window.attributes.apply {
                val display = windowManager.defaultDisplay
                val modes = display.supportedModes
                val highestRefreshRateMode = modes.maxByOrNull { it.refreshRate }
                highestRefreshRateMode?.let {
                    preferredDisplayModeId = it.modeId
                }
                layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 0)
            }
        }
        
        // enableEdgeToEdge() - Replaced by manual immersive mode
        setContent {
            val viewModel: AudioMixerViewModel = viewModel()
            val state by viewModel.uiState.collectAsState()
            var isShowSplash by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }

            OshoPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AnimatedContent(
                        targetState = isShowSplash,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                        },
                        label = "splash_transition"
                    ) { showSplash ->
                        if (showSplash) {
                            com.oshoplayer.app.ui.AnimatedSplashScreen(
                                onSplashComplete = { isShowSplash = false }
                            )
                        } else {
                            AudioMixerApp(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioMixerApp(
    viewModel: AudioMixerViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    PlayerScreen(
        state = state,
        playerStateFlow = viewModel.playerState,
        progressStateFlow = viewModel.progressState,
        onScanAudio = viewModel::scanDeviceAudio,
        onDeviceAudioSelected = viewModel::selectDeviceAudio,
        onAudioRoleSelected = viewModel::chooseAudioRole,
        onConfirmAudioSelection = viewModel::confirmSelectedAudio,
        onDismissAudioSelection = viewModel::dismissAudioAssignment,
        onPlayPause = viewModel::togglePlayPause,
        onPlayAudio = viewModel::playAudio,
        onSeekPrimary = viewModel::seekPrimaryTo,
        onSkipPrimaryBy = viewModel::skipPrimaryBy,
        onPrimaryVolumeChange = viewModel::setPrimaryVolume,
        onSecondaryVolumeChange = viewModel::setSecondaryVolume,
        onSleepTimerSelected = viewModel::startSleepTimer,
        onCancelSleepTimer = viewModel::cancelSleepTimer,
        onToggleStopAtEndOfDiscourse = viewModel::toggleStopAtEndOfDiscourse,
        onHapticsToggle = viewModel::setHapticsEnabled,
        onCheckForUpdates = viewModel::checkForUpdates,
        onSearchQueryChange = viewModel::updateSearchQuery,
        onSaveToLibrary = viewModel::saveToLibrary,
        onRemoveFromLibrary = viewModel::removeFromLibrary,
        onSelectPrimaryTrack = viewModel::selectPrimaryTrack,
        onSelectSecondaryTrack = viewModel::selectSecondaryTrack,
        onClearSecondaryTrack = viewModel::clearSecondaryTrack,
        onSavePlaylist = { name, photoUri -> viewModel.savePlaylist(name, photoUri) },
        onUpdatePlaylist = viewModel::updatePlaylist,
        onDeletePlaylist = viewModel::deletePlaylist,
        onPlayOshoPlusDiscourse = viewModel::playOshoPlusDiscourse,
        onDismissPaywall = viewModel::dismissPaywall,
        onVerifyPurchase = viewModel::verifyPurchase,
        onConsumeSessionKickoutToast = viewModel::consumeSessionKickoutToast,
        onSubmitPayment = viewModel::submitPayment,
        onDeactivatePremium = viewModel::deactivatePremium
    )
}
