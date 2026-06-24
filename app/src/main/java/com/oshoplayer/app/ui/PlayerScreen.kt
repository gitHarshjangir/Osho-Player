package com.oshoplayer.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.animation.core.animateFloat
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Search

import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oshoplayer.app.service.PlaybackProgress
import kotlinx.coroutines.launch
import com.oshoplayer.app.service.PlayerState
import com.oshoplayer.app.viewmodel.AudioMixerUiState
import com.oshoplayer.app.viewmodel.AudioRole
import com.oshoplayer.app.viewmodel.DeviceAudio
import com.oshoplayer.app.viewmodel.Playlist

private enum class MixerTab(
    val label: String,
    val icon: ImageVector
) {
    Library("Library", Icons.Default.LibraryMusic),
    Home("Home", Icons.Default.Home),
    Explore("Explore", Icons.Default.Explore),
    Settings("Settings", Icons.Default.Settings)
}

@Composable
fun PlayerScreen(
    state: AudioMixerUiState,
    playerStateFlow: kotlinx.coroutines.flow.StateFlow<PlayerState>,
    progressStateFlow: kotlinx.coroutines.flow.StateFlow<PlaybackProgress>,
    onScanAudio: () -> Unit,
    onDeviceAudioSelected: (DeviceAudio) -> Unit,
    onAudioRoleSelected: (AudioRole) -> Unit,
    onConfirmAudioSelection: () -> Unit,
    onDismissAudioSelection: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekPrimary: (Long) -> Unit,
    onSkipPrimaryBy: (Long) -> Unit,
    onPrimaryVolumeChange: (Float) -> Unit,
    onSecondaryVolumeChange: (Float) -> Unit,
    onSleepTimerSelected: (Long) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onToggleStopAtEndOfDiscourse: () -> Unit,
    onHapticsToggle: (Boolean) -> Unit,
    onCheckForUpdates: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSaveToLibrary: (DeviceAudio, AudioRole) -> Unit,
    onRemoveFromLibrary: (DeviceAudio, AudioRole) -> Unit,
    onSelectPrimaryTrack: (android.net.Uri) -> Unit,
    onSelectSecondaryTrack: (android.net.Uri) -> Unit,
    onClearSecondaryTrack: () -> Unit,
    onPlayAudio: () -> Unit,
    onSavePlaylist: (String, String?) -> Unit,
    onUpdatePlaylist: (Playlist) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit,
    onPlayOshoPlusDiscourse: (com.oshoplayer.app.data.RemoteAudio) -> Unit,
    onDismissPaywall: () -> Unit,
    onVerifyPurchase: (String?) -> Unit,
    onConsumeSessionKickoutToast: () -> Unit,
    onSubmitPayment: (String, android.net.Uri) -> Unit,
    onDeactivatePremium: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(MixerTab.Home) }
    var initialLibraryPage by remember { mutableStateOf(0) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var showPaymentWebView by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(state.showSessionKickoutToast) {
        if (state.showSessionKickoutToast) {
            android.widget.Toast.makeText(
                context,
                "This key is now active on another device.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            onConsumeSessionKickoutToast()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        StarryBackground()
        if (state.showPaywall && !showPaymentWebView) {
            PaywallDialog(
                onDismiss = onDismissPaywall,
                onPay = {
                    showPaymentWebView = true
                }
            )
        }
        
        if (showPaymentWebView) {
            PaymentScreen(
                state = state,
                onClose = { showPaymentWebView = false },
                onSubmit = onSubmitPayment
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
            targetState = selectedTab,
            modifier = Modifier.weight(1f).padding(top = 32.dp),
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "tab_transition"
        ) { tab ->
            when (tab) {
            MixerTab.Library -> LibraryTab(
                state = state,
                initialPage = initialLibraryPage,
                onSaveToLibrary = onSaveToLibrary,
                onRemoveFromLibrary = onRemoveFromLibrary,
                onSelectPrimaryTrack = onSelectPrimaryTrack,
                onSelectSecondaryTrack = onSelectSecondaryTrack,
                onPlayAudio = onPlayAudio,
                onSavePlaylist = onSavePlaylist,
                onUpdatePlaylist = onUpdatePlaylist,
                onDeletePlaylist = onDeletePlaylist,
                onPlayOshoPlusDiscourse = onPlayOshoPlusDiscourse,
                onGoExplore = { selectedTab = MixerTab.Explore },
                onGoHome = { selectedTab = MixerTab.Home },
                onGoSettings = { selectedTab = MixerTab.Settings }
            )

            MixerTab.Home -> HomeTab(
                state = state,
                playerStateFlow = playerStateFlow,
                progressStateFlow = progressStateFlow,
                onPlayPause = onPlayPause,
                onSeekPrimary = onSeekPrimary,
                onSkipPrimaryBy = onSkipPrimaryBy,
                onPrimaryVolumeChange = onPrimaryVolumeChange,
                onSecondaryVolumeChange = onSecondaryVolumeChange,
                onSleepTimerSelected = onSleepTimerSelected,
                onCancelSleepTimer = onCancelSleepTimer,
                onToggleStopAtEndOfDiscourse = onToggleStopAtEndOfDiscourse,
                onGoLibrary = { pageIndex ->
                    initialLibraryPage = pageIndex
                    selectedTab = MixerTab.Library
                },
                onClearSecondaryTrack = onClearSecondaryTrack
            )

            MixerTab.Explore -> ExploreTab(
                state = state,
                onScanAudio = onScanAudio,
                onDeviceAudioSelected = onDeviceAudioSelected,
                onAudioRoleSelected = onAudioRoleSelected,
                onConfirmAudioSelection = onConfirmAudioSelection,
                onDismissAudioSelection = onDismissAudioSelection,
                onSearchQueryChange = onSearchQueryChange,
                onSaveToLibrary = onSaveToLibrary,
                onGoHome = { selectedTab = MixerTab.Home }
            )

            MixerTab.Settings -> SettingsTab(
                state = state,
                onHapticsToggle = onHapticsToggle,
                onCheckForUpdates = onCheckForUpdates,
                onVerifyPurchase = onVerifyPurchase,
                onShowPaymentWebView = { showPaymentWebView = true },
                onDeactivatePremium = onDeactivatePremium
            )
        }
        }

            BottomGlassNav(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            )
        }
    }
}

@Composable
private fun HomeTab(
    state: AudioMixerUiState,
    playerStateFlow: kotlinx.coroutines.flow.StateFlow<PlayerState>,
    progressStateFlow: kotlinx.coroutines.flow.StateFlow<PlaybackProgress>,
    onPlayPause: () -> Unit,
    onSeekPrimary: (Long) -> Unit,
    onSkipPrimaryBy: (Long) -> Unit,
    onPrimaryVolumeChange: (Float) -> Unit,
    onSecondaryVolumeChange: (Float) -> Unit,
    onSleepTimerSelected: (Long) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onToggleStopAtEndOfDiscourse: () -> Unit,
    onGoLibrary: (Int) -> Unit,
    onClearSecondaryTrack: () -> Unit
) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val scrollState = androidx.compose.foundation.rememberScrollState()
        val playerState by playerStateFlow.collectAsStateWithLifecycle()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 0.dp)
                .animateContentSize(animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow))
        ) {
            Column(modifier = Modifier.heightIn(min = this@BoxWithConstraints.maxHeight)) {
        val discourseName = playerState.primaryTrackTitle.takeIf { it.isNotBlank() && !it.matches(Regex("\\d+")) } ?: state.primaryTrack?.displayName

        if (!discourseName.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .darkGlassSurface(cornerRadius = 24.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Recently Played",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = discourseName,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        PlayerMixerCapsule(
            state = state,
            playerStateFlow = playerStateFlow,
            progressStateFlow = progressStateFlow,
            onPlayPause = onPlayPause,
            onSeekPrimary = onSeekPrimary,
            onSkipPrimaryBy = onSkipPrimaryBy,
            onPrimaryVolumeChange = onPrimaryVolumeChange,
            onSecondaryVolumeChange = onSecondaryVolumeChange,
            onGoLibrary = onGoLibrary,
            onClearSecondaryTrack = onClearSecondaryTrack
        )

        Spacer(Modifier.height(24.dp))
        Spacer(Modifier.weight(1f))

        SleepTimerPanel(
            state = state,
            playerStateFlow = playerStateFlow,
            onSleepTimerSelected = onSleepTimerSelected,
            onCancelSleepTimer = onCancelSleepTimer,
            onToggleStopAtEndOfDiscourse = onToggleStopAtEndOfDiscourse
        )

        state.errorMessage?.let {
            CapsuleMessage(text = it, color = Color(0xFFFFA0A0))
        }
            }
        }
    }
}

@Composable
private fun PlayerMixerCapsule(
    state: AudioMixerUiState,
    playerStateFlow: kotlinx.coroutines.flow.StateFlow<PlayerState>,
    progressStateFlow: kotlinx.coroutines.flow.StateFlow<PlaybackProgress>,
    onPlayPause: () -> Unit,
    onSeekPrimary: (Long) -> Unit,
    onSkipPrimaryBy: (Long) -> Unit,
    onPrimaryVolumeChange: (Float) -> Unit,
    onSecondaryVolumeChange: (Float) -> Unit,
    onGoLibrary: (Int) -> Unit,
    onClearSecondaryTrack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .darkGlassSurface(cornerRadius = 32.dp, overlayAlpha = 0.76f, borderAlpha = 0.12f)
            .padding(horizontal = 13.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        val playerState by playerStateFlow.collectAsStateWithLifecycle()
        val progressState by progressStateFlow.collectAsStateWithLifecycle()
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "ADD Discourse",
                color = Color(0xFF48A1FF),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onGoLibrary(0) }
                    .padding(8.dp)
            )
            Box(
                modifier = Modifier
                    .size(101.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color.White.copy(alpha = 0.18f),
                                Color(0xFF2A2A2D),
                                Color.Black
                            )
                        )
                    )
                    .edgeLightBorder(cornerRadius = 50.dp, alpha = 0.16f),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.oshoplayer.app.R.drawable.logo1),
                    contentDescription = "Main Logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "ADD Background",
                    color = Color(0xFF48A1FF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onGoLibrary(1) }
                        .padding(8.dp)
                )
                Text(
                    "Stop Audio",
                    color = Color(0xFFFF5252),
                    fontSize = 10.sp,
                    modifier = Modifier
                        .clickable { onClearSecondaryTrack() }
                        .padding(4.dp)
                )
            }
        }

        TrackLine(
            label = "Discourse",
            value = playerState.primaryTrackTitle.takeIf { it.isNotBlank() && !it.matches(Regex("\\d+")) } ?: "Choose from Explore"
        )
        TrackLine(
            label = "Background",
            value = playerState.secondaryTrackTitle.takeIf { it.isNotBlank() && !it.matches(Regex("\\d+")) } ?: "Choose from Explore"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatDuration(progressState.primaryProgressMs), color = MutedText, fontSize = 12.sp)
            Text(formatDuration(playerState.primaryDurationMs), color = MutedText, fontSize = 12.sp)
        }

        val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
        var seekJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
        var isSeeking by remember { mutableStateOf(false) }
        var seekProgress by remember { mutableFloatStateOf(0f) }

        val upstreamProgress = if (playerState.primaryDurationMs > 0L) {
            progressState.primaryProgressMs.toFloat() / playerState.primaryDurationMs.toFloat()
        } else 0f
        
        val currentProgress = if (isSeeking) seekProgress else upstreamProgress

        PremiumSlider(
            value = currentProgress,
            onValueChange = { newVal ->
                isSeeking = true
                seekProgress = newVal
                seekJob?.cancel()
                seekJob = coroutineScope.launch {
                    kotlinx.coroutines.delay(100L) // Throttle seek heavily to avoid Exoplayer lag
                    onSeekPrimary((newVal * playerState.primaryDurationMs).toLong())
                }
            },
            onValueChangeFinished = {
                isSeeking = false
                seekJob?.cancel()
                onSeekPrimary((seekProgress * playerState.primaryDurationMs).toLong())
            },
            enabled = playerState.primaryDurationMs > 0L
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onSkipPrimaryBy(-15_000L) },
                enabled = state.primaryTrack != null,
                modifier = Modifier
                    .size(43.dp)
                    .softRoundButton()
            ) {
                Icon(Icons.Default.Replay10, contentDescription = "Rewind 15 seconds", tint = Color.White, modifier = Modifier.size(19.dp))
            }

            val context = androidx.compose.ui.platform.LocalContext.current
            ElevatedButton(
                onClick = {
                    val hasDiscourse = playerState.primaryTrackTitle.isNotBlank() && !playerState.primaryTrackTitle.matches(Regex("\\d+"))
                    if (!hasDiscourse) {
                        android.widget.Toast.makeText(context, "Please select a discourse to play", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        onPlayPause()
                    }
                },
                enabled = state.canPlay && !playerState.isFadingOut,
                modifier = Modifier.size(61.dp),
                shape = CircleShape,
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = Color(0xFF2F8CFF),
                    contentColor = Color.White,
                    disabledContainerColor = Color.White.copy(alpha = 0.10f)
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play or pause",
                    modifier = Modifier.size(26.dp)
                )
            }

            IconButton(
                onClick = { onSkipPrimaryBy(15_000L) },
                enabled = state.primaryTrack != null,
                modifier = Modifier
                    .size(43.dp)
                    .softRoundButton()
            ) {
                Text("+15", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }

        VolumeRow("Discourse volume", playerState.primaryVolume, onPrimaryVolumeChange)
        VolumeRow("Background volume", playerState.secondaryVolume, onSecondaryVolumeChange)
    }
}

@Composable
private fun ExploreTab(
    state: AudioMixerUiState,
    onScanAudio: () -> Unit,
    onDeviceAudioSelected: (DeviceAudio) -> Unit,
    onAudioRoleSelected: (AudioRole) -> Unit,
    onConfirmAudioSelection: () -> Unit,
    onDismissAudioSelection: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSaveToLibrary: (DeviceAudio, AudioRole) -> Unit,
    onGoHome: () -> Unit
) {
    val context = LocalContext.current
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> if (granted) onScanAudio() }
    )

    LaunchedEffect(Unit) {
        if (state.deviceAudio.isEmpty() && !state.isScanningAudio) {
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                onScanAudio()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 0.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        PageTitle("Explore")

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .darkGlassSurface(cornerRadius = 30.dp, overlayAlpha = 0.74f)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Device audio", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Text(
                    text = if (state.deviceAudio.isEmpty()) "Scan local audio files" else "${state.deviceAudio.size} files found",
                    color = MutedText,
                    fontSize = 13.sp
                )
            }
            ElevatedButton(
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                        onScanAudio()
                    } else {
                        permissionLauncher.launch(permission)
                    }
                },
                colors = primaryButtonColors()
            ) {
                Text(if (state.isScanningAudio) "Scanning" else "Scan")
            }
        }

        if (state.deviceAudio.isNotEmpty()) {
            androidx.compose.material3.OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search for music...", color = Color.White.copy(alpha = 0.4f)) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.1f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val list = state.filteredDeviceAudio
            items(list, key = { it.uri.toString() }) { audio ->
                var showOptions by remember { mutableStateOf(false) }
                Box {
                    AudioRow(
                        audio = audio, 
                        onClick = { onDeviceAudioSelected(audio) },
                        onLongClick = { showOptions = true }
                    )
                    androidx.compose.material3.DropdownMenu(
                        expanded = showOptions,
                        onDismissRequest = { showOptions = false },
                        modifier = Modifier.background(Color(0xFF2A2A2D))
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Add to Discourse", color = Color.White) },
                            onClick = { 
                                onSaveToLibrary(audio, AudioRole.Discourse)
                                showOptions = false 
                            }
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Add to background", color = Color.White) },
                            onClick = { 
                                onSaveToLibrary(audio, AudioRole.Background)
                                showOptions = false 
                            }
                        )
                    }
                }
            }
        }
    }

    state.selectedDeviceAudio?.let { selected ->
        AudioAssignmentCapsule(
            audio = selected,
            role = state.pendingAudioRole,
            onRoleSelected = onAudioRoleSelected,
            onConfirm = {
                onConfirmAudioSelection()
                onGoHome()
            },
            onDismiss = onDismissAudioSelection
        )
    }
}

@Composable
private fun SettingsTab(
    state: AudioMixerUiState,
    onHapticsToggle: (Boolean) -> Unit,
    onCheckForUpdates: () -> Unit,
    onVerifyPurchase: (String?) -> Unit,
    onShowPaymentWebView: () -> Unit,
    onDeactivatePremium: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        PageTitle("Settings")

        // 1. Premium Paywall Card
        if (!state.isPremium) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .darkGlassSurface(cornerRadius = 24.dp, overlayAlpha = 0.8f, borderAlpha = 0.3f)
                    .background(Brush.linearGradient(listOf(Color(0xFF1E3C2B), Color(0xFF0F1A14))), shape = RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Unlock All the premium Discourses Lifetime for just Rs. 21 (Limited Offer).",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    androidx.compose.material3.Button(
                        onClick = onShowPaymentWebView,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF7F)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Activate Now", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Purchase Management
        SettingsGroup(
            title = "Purchase Management",
            badge = if (state.isPremium) "PREMIUM ACTIVATED" else null,
            alwaysOpen = true
        ) {
            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
            
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    "Osho Key:", 
                    color = MutedText, 
                    fontSize = 12.sp
                )
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        state.deviceId,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "COPY",
                        color = Color(0xFF00FF7F),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(state.deviceId))
                                android.widget.Toast.makeText(context, "Key copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            .padding(8.dp)
                    )
                }
                Text(
                    "Please save this key for future purchase activation", 
                    color = Color(0xFFFFB74D), 
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            if (state.isPremium) {
                SettingsActionRow(
                    icon = Icons.Default.Check,
                    title = "Premium Activated",
                    onClick = { }
                )
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.Button(
                    onClick = onDeactivatePremium,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Deactivate Premium", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else {
                SettingsActionRow(
                    icon = Icons.Default.Close,
                    title = "Status: Not Activated Yet",
                    onClick = { }
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                var codeInput by remember { mutableStateOf("") }
                
                androidx.compose.material3.OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it },
                    label = { Text("Have an Osho Key? Paste here", color = MutedText) },
                    singleLine = true,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00FF7F),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    trailingIcon = {
                        androidx.compose.material3.IconButton(
                            onClick = {
                                val clipData = clipboardManager.getText()
                                if (clipData != null) {
                                    codeInput = clipData.text
                                }
                            }
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = Color.White)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                androidx.compose.material3.Button(
                    onClick = { onVerifyPurchase(codeInput.takeIf { it.isNotBlank() }) },
                    enabled = !state.isActivating,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF7F), disabledContainerColor = Color(0xFF00FF7F).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (state.isActivating) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = Color.Black, 
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        androidx.compose.material3.Text("Activate Plan", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
                
                state.activationError?.let {
                    Text(it, color = Color(0xFFFF5252), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
            }
        }

        // 3. Community
        SettingsGroup(title = "Community") {
            SettingsActionRow(
                icon = Icons.Default.Explore,
                title = "Share Osho Player with Family & Devotees",
                onClick = {
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, "Listen to Osho discourses and backgrounds on the Osho Player app! Discover the truth.")
                        setPackage("com.whatsapp")
                    }
                    try {
                        context.startActivity(shareIntent)
                    } catch (e: Exception) {
                        val chooser = android.content.Intent.createChooser(
                            android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, "Listen to Osho discourses and backgrounds on the Osho Player app! Discover the truth.")
                            }, "Share Osho Player"
                        )
                        context.startActivity(chooser)
                    }
                }
            )
        }

        // 4. Support & Feedback
        SettingsGroup(title = "Support & Feedback or Contact Dev") {
            SettingsActionRow(
                icon = Icons.Default.Send,
                title = "Tap to send your message.",
                subtext = null,
                onClick = {
                    val uri = android.net.Uri.parse("https://docs.google.com/forms/d/e/1FAIpQLScdnBGbb1KJk1eRnMXkcsLTaz_3VtnMyiQjoo7WXaMMvBGWMw/viewform?usp=dialog")
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Ignore if no browser
                    }
                }
            )
        }

        // 5. System & Sound
        SettingsGroup(title = "System & Sound") {
            SettingsSwitchRow(
                icon = Icons.Default.Vibration,
                title = "Haptics",
                checked = state.hapticsEnabled,
                onCheckedChange = onHapticsToggle
            )
        }

        // 6. Update
        SettingsGroup(title = "Update") {
            SettingsActionRow(
                icon = Icons.Default.SystemUpdate,
                title = "Check for updates",
                subtext = "Stay updated for new discourses.",
                onClick = onCheckForUpdates
            )
        }

        state.updateMessage?.let {
            CapsuleMessage(text = it, color = MutedText)
        }
    }
}

@Composable
private fun BottomGlassNav(
    selectedTab: MixerTab,
    onTabSelected: (MixerTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(92.dp)
            .darkGlassSurface(cornerRadius = 46.dp, overlayAlpha = 0.72f, borderAlpha = 0.16f)
            .padding(7.dp)
    ) {
        val haptic = LocalHapticFeedback.current
        val tabIndex = MixerTab.entries.indexOf(selectedTab)

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val tabWidth = this.maxWidth / 4
            val targetOffset = tabWidth * tabIndex
            val animatedOffset by animateDpAsState(
                targetValue = targetOffset,
                animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow),
                label = "tab_pill_offset"
            )

            // Traveling Pill Background
            Box(
                modifier = Modifier
                    .offset(x = animatedOffset)
                    .width(tabWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(39.dp))
                    .background(Color.White.copy(alpha = 0.15f))
            )

            // Tab Items
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MixerTab.entries.forEach { tab ->
                    val selected = selectedTab == tab
                    val iconColor by animateColorAsState(targetValue = if (selected) Color(0xFF48A1FF) else Color.White)
                    val textColor by animateColorAsState(targetValue = if (selected) Color(0xFF48A1FF) else Color.White.copy(alpha = 0.72f))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .bounceClick { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTabSelected(tab) 
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = iconColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = tab.label,
                            color = textColor,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioAssignmentCapsule(
    audio: DeviceAudio,
    role: AudioRole?,
    onRoleSelected: (AudioRole) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .darkGlassSurface(cornerRadius = 32.dp, overlayAlpha = 0.86f)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(audio.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.softRoundButton()) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            RoleButton(
                title = "Treat as discourse",
                selected = role == AudioRole.Discourse,
                onClick = { onRoleSelected(AudioRole.Discourse) }
            )
            RoleButton(
                title = "Treat as background audio",
                selected = role == AudioRole.Background,
                onClick = { onRoleSelected(AudioRole.Background) }
            )

            ElevatedButton(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                enabled = role != null,
                colors = primaryButtonColors()
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Set")
            }
        }
    }
}

@Composable
private fun SleepTimerPanel(
    state: AudioMixerUiState,
    playerStateFlow: kotlinx.coroutines.flow.StateFlow<PlayerState>,
    onSleepTimerSelected: (Long) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onToggleStopAtEndOfDiscourse: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .darkGlassSurface(cornerRadius = 32.dp, overlayAlpha = 0.74f)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val playerState by playerStateFlow.collectAsStateWithLifecycle()
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.Timer, contentDescription = null, tint = Color.White)
            Text("Sleep timer", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Spacer(Modifier.weight(1f))
            if (state.sleepTimerRemainingMs > 0L || playerState.isFadingOut) {
                Text(
                    text = if (playerState.isFadingOut) "Fading" else formatDuration(state.sleepTimerRemainingMs),
                    color = Color(0xFF48A1FF),
                    fontWeight = FontWeight.Bold
                )
            } else if (state.stopAtEndOfDiscourse) {
                Text(
                    text = "End of discourse",
                    color = Color(0xFF48A1FF),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(15L, 30L, 45L, 60L).forEach { minutes ->
                ElevatedButton(
                    onClick = { onSleepTimerSelected(minutes) },
                    modifier = Modifier.weight(1f),
                    colors = mutedButtonColors(),
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {
                    Text(if (minutes == 60L) "1h" else "${minutes}m")
                }
            }
        }

        ElevatedButton(
            onClick = onToggleStopAtEndOfDiscourse,
            modifier = Modifier.fillMaxWidth(),
            colors = if (state.stopAtEndOfDiscourse) primaryButtonColors() else mutedButtonColors()
        ) {
            Text("End of discourse")
        }

        ElevatedButton(
            onClick = {
                onCancelSleepTimer()
                if (state.stopAtEndOfDiscourse) onToggleStopAtEndOfDiscourse()
            },
            enabled = state.sleepTimerRemainingMs > 0L || playerState.isFadingOut || state.stopAtEndOfDiscourse,
            modifier = Modifier.fillMaxWidth(),
            colors = mutedButtonColors()
        ) {
            Text("Cancel timer")
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    badge: String? = null,
    alwaysOpen: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(alwaysOpen) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .darkGlassSurface(cornerRadius = 32.dp, overlayAlpha = 0.78f)
            .clickable(
                enabled = !alwaysOpen,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { expanded = !expanded }
            .padding(vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (badge != null) {
                    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
                    val glowAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 0.8f,
                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                            animation = androidx.compose.animation.core.tween(1000),
                            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                        ),
                        label = "badgeGlow"
                    )
                    
                    Text(
                        text = badge,
                        color = Color(0xFF00FF7F),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color(0xFF00FF7F).copy(alpha = glowAlpha), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            if (!alwaysOpen) {
                Text(
                    text = if (expanded) "-" else "+",
                    color = MutedText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        androidx.compose.animation.AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CapsuleIcon(icon)
        Text(title, color = Color.White, fontSize = 20.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtext: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CapsuleIcon(icon)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 20.sp)
            if (subtext != null) {
                Text(subtext, color = MutedText, fontSize = 14.sp)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AudioRow(audio: DeviceAudio, onClick: () -> Unit, onLongClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .darkGlassSurface(cornerRadius = 24.dp, overlayAlpha = 0.70f, shadowElevation = 8.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CapsuleIcon(Icons.Default.MusicNote)
        Column(modifier = Modifier.weight(1f)) {
            Text(audio.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 16.sp)
        }
    }
}

@Composable
private fun RoleButton(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(if (selected) Color(0xFF2F8CFF).copy(alpha = 0.24f) else Color.White.copy(alpha = 0.07f))
            .edgeLightBorder(cornerRadius = 22.dp, alpha = if (selected) 0.28f else 0.10f)
            .clickable(onClick = onClick)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF48A1FF))
    }
}

@Composable
private fun TrackLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, color = MutedText, fontSize = 12.sp, modifier = Modifier.weight(0.32f))
        Text(
            value,
            color = Color.White,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.68f)
        )
    }
}

@Composable
private fun VolumeRow(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
        var job by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
        
        var isDragging by remember { mutableStateOf(false) }
        var dragValue by remember { mutableFloatStateOf(value) }
        
        val displayValue = if (isDragging) dragValue else value

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontSize = 13.sp)
            Text("${(displayValue * 100).toInt()}%", color = MutedText, fontSize = 13.sp)
        }
        PremiumSlider(
            value = displayValue,
            onValueChange = { newVal ->
                isDragging = true
                dragValue = newVal
                job?.cancel()
                job = coroutineScope.launch {
                    kotlinx.coroutines.delay(32L) // Throttle to 30fps to prevent lag
                    onValueChange(newVal)
                }
            },
            onValueChangeFinished = {
                isDragging = false
                job?.cancel()
                onValueChange(dragValue)
            }
        )
    }
}

@Composable
private fun PremiumSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null
) {
    var internalProgress by remember { mutableFloatStateOf(-1f) }
    val displayValue = if (internalProgress >= 0f) internalProgress else value
    
    val trackColor = if (enabled) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f)
    val activeTrackColor = if (enabled) Color(0xFF48A1FF) else Color.White.copy(alpha = 0.15f)
    val thumbColor = if (enabled) Color.White else Color.White.copy(alpha = 0.3f)

    androidx.compose.foundation.Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = { offset ->
                        internalProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onValueChange(internalProgress)
                        tryAwaitRelease()
                        onValueChangeFinished?.invoke()
                        internalProgress = -1f
                    }
                )
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        internalProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onValueChange(internalProgress)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        internalProgress = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onValueChange(internalProgress)
                    },
                    onDragEnd = { 
                        onValueChangeFinished?.invoke()
                        internalProgress = -1f 
                    },
                    onDragCancel = { 
                        onValueChangeFinished?.invoke()
                        internalProgress = -1f 
                    }
                )
            }
    ) {
        val trackHeight = 10.dp.toPx()
        val thumbRadius = 11.dp.toPx()
        
        val centerY = size.height / 2f
        val startX = thumbRadius
        val endX = size.width - thumbRadius
        val activeWidth = startX + (endX - startX) * displayValue.coerceIn(0f, 1f)

        // Inactive Track
        drawRoundRect(
            color = trackColor,
            topLeft = androidx.compose.ui.geometry.Offset(0f, centerY - trackHeight / 2f),
            size = androidx.compose.ui.geometry.Size(size.width, trackHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2f, trackHeight / 2f)
        )

        // Active Track
        if (activeWidth > 0f) {
            drawRoundRect(
                color = activeTrackColor,
                topLeft = androidx.compose.ui.geometry.Offset(0f, centerY - trackHeight / 2f),
                size = androidx.compose.ui.geometry.Size(activeWidth, trackHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2f, trackHeight / 2f)
            )
        }

        // Thumb
        drawCircle(
            color = thumbColor,
            radius = thumbRadius,
            center = androidx.compose.ui.geometry.Offset(activeWidth, centerY)
        )
    }
}

@Composable
private fun PageTitle(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp
    )
}

@Composable
private fun CapsuleIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.80f), modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun CapsuleMessage(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .darkGlassSurface(cornerRadius = 24.dp, overlayAlpha = 0.76f)
            .padding(16.dp)
    )
}

private fun Modifier.softRoundButton(): Modifier =
    this
        .clip(CircleShape)
        .background(Color.White.copy(alpha = 0.08f))
        .edgeLightBorder(cornerRadius = 40.dp, alpha = 0.10f)

@Composable
private fun Modifier.bounceClick(
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: () -> Unit
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.92f else 1f, animationSpec = tween(150), label = "bounce")
    return this
        .scale(scale)
        .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
}



@Composable
private fun primaryButtonColors() = ButtonDefaults.elevatedButtonColors(
    containerColor = Color(0xFF2F8CFF),
    contentColor = Color.White,
    disabledContainerColor = Color.White.copy(alpha = 0.10f),
    disabledContentColor = Color.White.copy(alpha = 0.34f)
)

@Composable
private fun mutedButtonColors() = ButtonDefaults.elevatedButtonColors(
    containerColor = Color.White.copy(alpha = 0.09f),
    contentColor = Color.White
)

private val MutedText = Color(0xFFA0A0A8)

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

private fun formatBytes(bytes: Long): String {
    val mb = bytes / (1024f * 1024f)
    return if (mb >= 1f) {
        "%.1f MB".format(mb)
    } else {
        "${(bytes / 1024L).coerceAtLeast(1L)} KB"
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryTab(
    state: AudioMixerUiState,
    onSaveToLibrary: (DeviceAudio, AudioRole) -> Unit,
    onRemoveFromLibrary: (DeviceAudio, AudioRole) -> Unit,
    onSelectPrimaryTrack: (android.net.Uri) -> Unit,
    onSelectSecondaryTrack: (android.net.Uri) -> Unit,
    onPlayAudio: () -> Unit,
    onSavePlaylist: (String, String?) -> Unit,
    onUpdatePlaylist: (Playlist) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit,
    onPlayOshoPlusDiscourse: (com.oshoplayer.app.data.RemoteAudio) -> Unit,
    initialPage: Int = 0,
    onGoExplore: () -> Unit,
    onGoHome: () -> Unit,
    onGoSettings: () -> Unit
) {
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var expandedFolders by remember { mutableStateOf(setOf<String>()) }
    var searchQueryDiscourses by remember { mutableStateOf("") }
    var searchQueryBackgrounds by remember { mutableStateOf("") }
    
    val currentPlaylist = selectedPlaylist?.let { sp -> state.savedPlaylists.find { it.id == sp.id } }

    if (currentPlaylist != null) {
        PlaylistDetailView(
            playlist = currentPlaylist,
            state = state,
            onBack = { selectedPlaylist = null },
            onUpdatePlaylist = onUpdatePlaylist,
            onSelectPrimaryTrack = onSelectPrimaryTrack,
            onSelectSecondaryTrack = onSelectSecondaryTrack,
            onPlayAudio = onPlayAudio
        )
        return
    }

    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 4 })
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(pagerState.currentPage) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    
    LaunchedEffect(initialPage) {
        if (pagerState.currentPage != initialPage) {
            pagerState.scrollToPage(initialPage)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp, bottom = 0.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = "Library",
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Animated Glass Top Navigation
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(52.dp)
                .darkGlassSurface(cornerRadius = 26.dp, overlayAlpha = 0.72f, borderAlpha = 0.16f)
                .padding(5.dp)
        ) {
            val tabTitles = listOf("Discourses", "Background", "Playlists", "Osho+")
            val tabWidth = this.maxWidth / tabTitles.size
            val targetOffset = tabWidth * pagerState.currentPage
            val animatedOffset by animateDpAsState(
                targetValue = targetOffset,
                animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow),
                label = "top_tab_pill_offset"
            )

            // Traveling Pill Background
            Box(
                modifier = Modifier
                    .offset(x = animatedOffset)
                    .width(tabWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(21.dp))
                    .background(Color.White.copy(alpha = 0.15f))
            )

            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                tabTitles.forEachIndexed { index, title ->
                    val selected = pagerState.currentPage == index
                    val textColor by animateColorAsState(targetValue = if (selected) Color(0xFF48A1FF) else Color.White.copy(alpha = 0.72f))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                coroutineScope.launch { pagerState.animateScrollToPage(index) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = textColor,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (page) {
                    3 -> {
                        if (state.isOshoPlusLoading) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                                    androidx.compose.material3.CircularProgressIndicator(color = Color(0xFF48A1FF))
                                }
                            }
                        } else if (state.oshoPlusDiscourses.isEmpty()) {
                            item {
                                Text(
                                    text = if (state.errorMessage?.contains("Osho+") == true) state.errorMessage else "No Osho+ discourses available.",
                                    color = MutedText,
                                    modifier = Modifier.padding(top = 20.dp)
                                )
                            }
                        } else {
                            val grouped = state.oshoPlusDiscourses.groupBy { it.folder }
                            grouped.forEach { (folder, audios) ->
                                val isExpanded = expandedFolders.contains(folder)
                                item(key = "folder_$folder") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .darkGlassSurface(cornerRadius = 24.dp, overlayAlpha = 0.8f, shadowElevation = 12.dp)
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                expandedFolders = if (isExpanded) expandedFolders - folder else expandedFolders + folder
                                            }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        CapsuleIcon(androidx.compose.material.icons.Icons.Default.Cloud)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(folder, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                            Text("${audios.size} discourses", color = MutedText, fontSize = 13.sp)
                                        }
                                    }
                                }
                                
                                if (isExpanded) {
                                    items(audios, key = { it.key }) { audio ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 24.dp)
                                                .darkGlassSurface(cornerRadius = 20.dp, overlayAlpha = 0.60f, shadowElevation = 4.dp)
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    onPlayOshoPlusDiscourse(audio)
                                                    onGoHome()
                                                }
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            CapsuleIcon(androidx.compose.material.icons.Icons.Default.MusicNote)
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(audio.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 16.sp)
                                                Text(
                                                    "Streaming | ${formatBytes(audio.sizeBytes)}",
                                                    color = MutedText,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    0, 1 -> {
                        val fullList = if (page == 0) state.builtInDiscourses + state.savedDiscourses else state.builtInBackgrounds + state.savedBackgrounds
                        val searchQuery = if (page == 0) searchQueryDiscourses else searchQueryBackgrounds
                        val list = if (searchQuery.isBlank()) fullList else fullList.filter { it.title.contains(searchQuery, ignoreCase = true) }
                        
                        item {
                            androidx.compose.material3.OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { 
                                    if (page == 0) searchQueryDiscourses = it else searchQueryBackgrounds = it 
                                },
                                placeholder = { Text("Search ${if (page == 0) "Discourses" else "Backgrounds"}...", color = MutedText) },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MutedText) },
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF48A1FF),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    cursorColor = Color(0xFF48A1FF)
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp)
                            )
                        }

                        if (list.isEmpty()) {
                            item {
                                Text(
                                    text = "No saved audio found. Go to Explore and long press a track to add it.",
                                    color = MutedText,
                                    modifier = Modifier.padding(top = 20.dp)
                                )
                                Spacer(Modifier.height(16.dp))
                                ElevatedButton(
                                    onClick = onGoExplore,
                                    colors = primaryButtonColors(),
                                    modifier = Modifier.fillMaxWidth().height(52.dp)
                                ) {
                                    Text("Find Audio in Explore")
                                }
                            }
                        } else {
                            items(list, key = { it.uri.toString() }) { audio ->
                                var showOptions by remember { mutableStateOf(false) }
                                Box {
                                    AudioRow(
                                        audio = audio,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            if (page == 0) {
                                                onSelectPrimaryTrack(audio.uri)
                                            } else {
                                                onSelectSecondaryTrack(audio.uri)
                                            }
                                            onGoHome()
                                        },
                                        onLongClick = { 
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showOptions = true 
                                        }
                                    )
                                    androidx.compose.material3.DropdownMenu(
                                        expanded = showOptions,
                                        onDismissRequest = { showOptions = false },
                                        modifier = Modifier.background(Color(0xFF2A2A2D))
                                    ) {
                                        val isBuiltIn = if (page == 0) {
                                            state.builtInDiscourses.any { it.uri == audio.uri }
                                        } else {
                                            state.builtInBackgrounds.any { it.uri == audio.uri }
                                        }
                                        if (!isBuiltIn) {
                                            androidx.compose.material3.DropdownMenuItem(
                                                text = { Text("Remove from Library", color = Color(0xFFFF5252)) },
                                                onClick = { 
                                                    onRemoveFromLibrary(audio, if (page == 0) AudioRole.Discourse else AudioRole.Background)
                                                    showOptions = false 
                                                }
                                            )
                                        } else {
                                            androidx.compose.material3.DropdownMenuItem(
                                                text = { Text("Built-in audio", color = MutedText) },
                                                onClick = { showOptions = false },
                                                enabled = false
                                            )
                                        }
                                    }
                                }
                            }
                            
                            if (page == 0) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp)
                                            .background(
                                                Brush.linearGradient(listOf(Color(0xFF1E3C2B), Color(0xFF0F1A14))), 
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { onGoSettings() }
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Explore Osho+ for more discourses", color = Color(0xFF00FF7F), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        item {
                            var showCreateDialog by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .darkGlassSurface(cornerRadius = 28.dp, overlayAlpha = 0.76f, shadowElevation = 12.dp)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        showCreateDialog = true 
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+ Create New Playlist", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }

                            if (showCreateDialog) {
                                CreatePlaylistDialog(
                                    onDismiss = { showCreateDialog = false },
                                    onSave = { name, photoUri ->
                                        onSavePlaylist(name, photoUri)
                                        showCreateDialog = false
                                    }
                                )
                            }
                        }

                        if (state.savedPlaylists.isEmpty()) {
                            item {
                                Text(
                                    text = "No playlists found. Create one to organize your discourses and background tracks.",
                                    color = MutedText,
                                    modifier = Modifier.padding(top = 20.dp)
                                )
                            }
                        } else {
                            items(state.savedPlaylists, key = { it.id }) { playlist ->
                                var showOptions by remember { mutableStateOf(false) }
                                Box {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .darkGlassSurface(cornerRadius = 24.dp, overlayAlpha = 0.70f, shadowElevation = 8.dp)
                                            .combinedClickable(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    selectedPlaylist = playlist
                                                },
                                                onLongClick = { 
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    showOptions = true 
                                                }
                                            )
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        if (playlist.photoUri != null) {
                                            androidx.compose.foundation.Image(
                                                painter = coil.compose.rememberAsyncImagePainter(playlist.photoUri),
                                                contentDescription = null,
                                                modifier = Modifier.size(48.dp).clip(CircleShape),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                        } else {
                                            CapsuleIcon(Icons.Default.LibraryMusic)
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(playlist.name, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 16.sp)
                                            Text("${playlist.discourseUris.size} Discourses, ${playlist.backgroundUris.size} Backgrounds", color = MutedText, fontSize = 12.sp)
                                        }
                                    }
                                    androidx.compose.material3.DropdownMenu(
                                        expanded = showOptions,
                                        onDismissRequest = { showOptions = false },
                                        modifier = Modifier.background(Color(0xFF2A2A2D))
                                    ) {
                                        androidx.compose.material3.DropdownMenuItem(
                                            text = { Text("Delete Playlist", color = Color(0xFFFF5252)) },
                                            onClick = { 
                                                onDeletePlaylist(playlist)
                                                showOptions = false 
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, photoUri: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            photoUri = uri.toString()
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .darkGlassSurface(cornerRadius = 32.dp, overlayAlpha = 0.85f, shadowElevation = 16.dp)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Create Playlist", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)

            // Photo picker
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .darkGlassSurface(cornerRadius = 50.dp, overlayAlpha = 0.5f)
                    .clickable { launcher.launch(arrayOf("image/*")) },
                contentAlignment = Alignment.Center
            ) {
                if (photoUri != null) {
                    androidx.compose.foundation.Image(
                        painter = coil.compose.rememberAsyncImagePainter(photoUri),
                        contentDescription = "Playlist Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add Photo", tint = Color.White, modifier = Modifier.size(40.dp))
                }
            }

            androidx.compose.material3.OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist Name") },
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MutedText)
                }
                androidx.compose.material3.Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSave(name, photoUri)
                        }
                    },
                    enabled = name.isNotBlank(),
                    colors = primaryButtonColors()
                ) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun PlaylistDetailView(
    playlist: Playlist,
    state: AudioMixerUiState,
    onBack: () -> Unit,
    onUpdatePlaylist: (Playlist) -> Unit,
    onSelectPrimaryTrack: (android.net.Uri) -> Unit,
    onSelectSecondaryTrack: (android.net.Uri) -> Unit,
    onPlayAudio: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var selectedDiscourseUri by remember { mutableStateOf<String?>(null) }
    var selectedBackgroundUri by remember { mutableStateOf<String?>(null) }

    var showDiscoursePicker by remember { mutableStateOf(false) }
    var showBackgroundPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { 
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onBack() 
            }) {
                Icon(Icons.Default.Close, contentDescription = "Back", tint = Color.White)
            }
            Text(playlist.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            
            // Discourses Section
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .darkGlassSurface(cornerRadius = 20.dp, overlayAlpha = 0.8f, borderAlpha = 0.2f)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text("Discourses", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    androidx.compose.material3.TextButton(onClick = { showDiscoursePicker = true }) {
                        Text("+ Add", color = Color(0xFF48A1FF))
                    }
                }
            }
            items(playlist.discourseUris, key = { "d_$it" }) { uriStr ->
                val audio = state.deviceAudio.find { it.uri.toString() == uriStr }
                if (audio != null) {
                    val isSelected = selectedDiscourseUri == uriStr
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .darkGlassSurface(cornerRadius = 20.dp, overlayAlpha = if (isSelected) 0.9f else 0.5f, borderAlpha = if (isSelected) 0.5f else 0.1f)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedDiscourseUri = uriStr
                                onSelectPrimaryTrack(android.net.Uri.parse(uriStr))
                                onPlayAudio()
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(audio.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            Text(audio.artist, color = MutedText, fontSize = 12.sp)
                        }
                        // Up/Down arrows
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val index = playlist.discourseUris.indexOf(uriStr)
                            if (index > 0) {
                                IconButton(onClick = {
                                    val newList = playlist.discourseUris.toMutableList()
                                    val tmp = newList[index]
                                    newList[index] = newList[index - 1]
                                    newList[index - 1] = tmp
                                    onUpdatePlaylist(playlist.copy(discourseUris = newList))
                                }, modifier = Modifier.size(24.dp)) {
                                    Text("?", color = Color.White)
                                }
                            }
                            if (index < playlist.discourseUris.size - 1) {
                                IconButton(onClick = {
                                    val newList = playlist.discourseUris.toMutableList()
                                    val tmp = newList[index]
                                    newList[index] = newList[index + 1]
                                    newList[index + 1] = tmp
                                    onUpdatePlaylist(playlist.copy(discourseUris = newList))
                                }, modifier = Modifier.size(24.dp)) {
                                    Text("?", color = Color.White)
                                }
                            }
                        }
                        IconButton(onClick = {
                            val newList = playlist.discourseUris - uriStr
                            onUpdatePlaylist(playlist.copy(discourseUris = newList))
                        }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // Background Section
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .darkGlassSurface(cornerRadius = 20.dp, overlayAlpha = 0.8f, borderAlpha = 0.2f)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text("Background Audio", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    androidx.compose.material3.TextButton(onClick = { showBackgroundPicker = true }) {
                        Text("+ Add", color = Color(0xFF48A1FF))
                    }
                }
            }
            items(playlist.backgroundUris, key = { "b_$it" }) { uriStr ->
                val audio = state.deviceAudio.find { it.uri.toString() == uriStr }
                if (audio != null) {
                    val isSelected = selectedBackgroundUri == uriStr
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .darkGlassSurface(cornerRadius = 20.dp, overlayAlpha = if (isSelected) 0.9f else 0.5f, borderAlpha = if (isSelected) 0.5f else 0.1f)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedBackgroundUri = uriStr
                                onSelectSecondaryTrack(android.net.Uri.parse(uriStr))
                                onPlayAudio()
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(audio.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            Text(audio.artist, color = MutedText, fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val index = playlist.backgroundUris.indexOf(uriStr)
                            if (index > 0) {
                                IconButton(onClick = {
                                    val newList = playlist.backgroundUris.toMutableList()
                                    val tmp = newList[index]
                                    newList[index] = newList[index - 1]
                                    newList[index - 1] = tmp
                                    onUpdatePlaylist(playlist.copy(backgroundUris = newList))
                                }, modifier = Modifier.size(24.dp)) {
                                    Text("?", color = Color.White)
                                }
                            }
                            if (index < playlist.backgroundUris.size - 1) {
                                IconButton(onClick = {
                                    val newList = playlist.backgroundUris.toMutableList()
                                    val tmp = newList[index]
                                    newList[index] = newList[index + 1]
                                    newList[index + 1] = tmp
                                    onUpdatePlaylist(playlist.copy(backgroundUris = newList))
                                }, modifier = Modifier.size(24.dp)) {
                                    Text("?", color = Color.White)
                                }
                            }
                        }
                        IconButton(onClick = {
                            val newList = playlist.backgroundUris - uriStr
                            onUpdatePlaylist(playlist.copy(backgroundUris = newList))
                        }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // Pickers
    if (showDiscoursePicker) {
        AudioPickerDialog(
            title = "Add Discourse",
            audios = state.savedDiscourses.filter { it.uri.toString() !in playlist.discourseUris },
            onDismiss = { showDiscoursePicker = false },
            onSelect = { uri ->
                onUpdatePlaylist(playlist.copy(discourseUris = playlist.discourseUris + uri))
                showDiscoursePicker = false
            }
        )
    }
    if (showBackgroundPicker) {
        AudioPickerDialog(
            title = "Add Background",
            audios = state.savedBackgrounds.filter { it.uri.toString() !in playlist.backgroundUris },
            onDismiss = { showBackgroundPicker = false },
            onSelect = { uri ->
                onUpdatePlaylist(playlist.copy(backgroundUris = playlist.backgroundUris + uri))
                showBackgroundPicker = false
            }
        )
    }
}

@Composable
private fun AudioPickerDialog(
    title: String,
    audios: List<DeviceAudio>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .darkGlassSurface(cornerRadius = 32.dp, overlayAlpha = 0.85f, shadowElevation = 16.dp)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (audios.isEmpty()) {
                    item { Text("No available audio. Save some to your library first.", color = MutedText) }
                }
                items(audios) { audio ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .darkGlassSurface(cornerRadius = 16.dp, overlayAlpha = 0.5f, borderAlpha = 0.1f)
                            .clickable { onSelect(audio.uri.toString()) }
                            .padding(12.dp)
                    ) {
                        Text(audio.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            androidx.compose.material3.TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Cancel", color = MutedText)
            }
        }
    }
}

@Composable
fun PaywallDialog(
    onDismiss: () -> Unit,
    onPay: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2A2A2D),
        title = { androidx.compose.material3.Text("Osho+ Premium", color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
        text = { androidx.compose.material3.Text("Unlock all the premium Discourses Lifetime for just Rs. 21.", color = Color(0xFFB0B0B0)) },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = onPay,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF7F))
            ) {
                androidx.compose.material3.Text("Pay with UPI", color = Color.Black, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                androidx.compose.material3.Text("Not now", color = Color(0xFFB0B0B0))
            }
        }
    )
}

@Composable
fun PaymentWebView(onClose: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            androidx.compose.ui.viewinterop.AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    android.webkit.WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = object : android.webkit.WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: android.webkit.WebView?,
                                request: android.webkit.WebResourceRequest?
                            ): Boolean {
                                val url = request?.url.toString()
                                if (url.startsWith("upi://")) {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                    try {
                                        ctx.startActivity(android.content.Intent.createChooser(intent, "Pay with"))
                                        onClose()
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(ctx, "No UPI app found", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                    return true
                                }
                                return super.shouldOverrideUrlLoading(view, request)
                            }
                        }
                    }
                },
                update = { webView ->
                    val txnId = "OSHO" + System.currentTimeMillis()
                    val uriStr = "upi://pay?pa=YOUR_UPI_ID@BANK&pn=YOUR_NAME&am=21.00&cu=INR&tn=Unlock_Premium&tr=$txnId"
                    val html = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1">
                            <style>
                                body {
                                    display: flex;
                                    flex-direction: column;
                                    justify-content: center;
                                    align-items: center;
                                    height: 100vh;
                                    background-color: #000;
                                    color: #fff;
                                    font-family: sans-serif;
                                    margin: 0;
                                    text-align: center;
                                    padding: 20px;
                                }
                                h1 { font-size: 24px; margin-bottom: 10px; }
                                p { font-size: 16px; color: #aaa; margin-bottom: 30px; }
                                a.pay-button {
                                    display: inline-block;
                                    padding: 16px 32px;
                                    font-size: 20px;
                                    font-weight: bold;
                                    color: #000;
                                    background-color: #00FF7F;
                                    text-decoration: none;
                                    border-radius: 12px;
                                    box-shadow: 0 4px 15px rgba(0, 255, 127, 0.4);
                                }
                                a.close-button {
                                    margin-top: 20px;
                                    color: #888;
                                    text-decoration: underline;
                                    font-size: 14px;
                                    display: block;
                                }
                            </style>
                        </head>
                        <body>
                            <h1>Secure Payment Gateway</h1>
                            <p>You are unlocking Osho Player Premium for ₹21.</p>
                            <a href="$uriStr" class="pay-button">Proceed to Pay Via UPI App</a>
                            <br/>
                            <a href="javascript:Android.close()" class="close-button" onclick="window.close()">Close Webpage</a>
                        </body>
                        </html>
                    """.trimIndent()
                    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                }
            )
            
            androidx.compose.material3.TextButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                androidx.compose.material3.Text("Cancel", color = Color.White)
            }
        }
    }
}
