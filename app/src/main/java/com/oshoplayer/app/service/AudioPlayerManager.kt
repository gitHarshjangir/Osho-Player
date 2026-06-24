package com.oshoplayer.app.service

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlaybackProgress(
    val primaryProgressMs: Long = 0L,
    val secondaryProgressMs: Long = 0L
)

data class PlayerState(
    val isPlaying: Boolean = false,
    val primaryDurationMs: Long = 0L,
    val secondaryDurationMs: Long = 0L,
    val primaryVolume: Float = 0.9f,
    val secondaryVolume: Float = 0.35f,
    val errorMessage: String? = null,
    val isFadingOut: Boolean = false,
    val primaryTrackUri: Uri? = null,
    val primaryTrackTitle: String = "Osho Player",
    val primaryTrackArtist: String = "Discourse",
    val secondaryTrackUri: Uri? = null,
    val secondaryTrackTitle: String = "Background",
    val secondaryTrackArtist: String = "Music"
)

object AudioPlayerManager {
    private var primaryPlayer: ExoPlayer? = null
    private var secondaryPlayer: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null
    private var fadeOutJob: Job? = null
    private var prefs: android.content.SharedPreferences? = null
    private var downloadCache: androidx.media3.datasource.cache.SimpleCache? = null
    
    private var audioManager: android.media.AudioManager? = null
    private var audioFocusRequest: android.media.AudioFocusRequest? = null
    private var wasPlayingBeforeFocusLoss = false

    private val audioFocusChangeListener = android.media.AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            android.media.AudioManager.AUDIOFOCUS_LOSS,
            android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                wasPlayingBeforeFocusLoss = _playerState.value.isPlaying
                pauseBoth()
            }
            android.media.AudioManager.AUDIOFOCUS_GAIN -> {
                if (wasPlayingBeforeFocusLoss) {
                    playBoth()
                }
            }
        }
    }

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _progressState = MutableStateFlow(PlaybackProgress())
    val progressState: StateFlow<PlaybackProgress> = _progressState.asStateFlow()

    fun initialize(context: Context) {
        if (primaryPlayer != null) return
        prefs = context.applicationContext.getSharedPreferences("OshoPlayerState", Context.MODE_PRIVATE)

        val cacheSize = 500L * 1024 * 1024 // 500MB
        val cacheEvictor = androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor(cacheSize)
        val databaseProvider = androidx.media3.database.StandaloneDatabaseProvider(context)
        val cacheDir = java.io.File(context.cacheDir, "media3_cache")
        
        if (downloadCache == null) {
            downloadCache = androidx.media3.datasource.cache.SimpleCache(cacheDir, cacheEvictor, databaseProvider)
        }

        val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
        val defaultDataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)

        val cacheDataSourceFactory = androidx.media3.datasource.cache.CacheDataSource.Factory()
            .setCache(downloadCache!!)
            .setUpstreamDataSourceFactory(defaultDataSourceFactory)
            .setFlags(androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
            .setDataSourceFactory(cacheDataSourceFactory)

        primaryPlayer = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                false
            )
            repeatMode = Player.REPEAT_MODE_OFF
            volume = _playerState.value.primaryVolume
        }

        secondaryPlayer = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                false
            )
            repeatMode = Player.REPEAT_MODE_ONE
            volume = _playerState.value.secondaryVolume
        }

        attachListeners()
        restoreState()
        startProgressLoop()
        
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            audioFocusRequest = android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
        }
    }

    val primary: ExoPlayer? get() = primaryPlayer
    val secondary: ExoPlayer? get() = secondaryPlayer

    fun preWarmPlayers() {
        // Ensure players are initialized without loading any media items.
        // This will allocate buffers and set audio attributes ahead of time.
        // Called from the service before the UI is displayed.
        primaryPlayer?.prepare()
        secondaryPlayer?.prepare()
    }

    fun setPrimaryTrack(uri: Uri, title: String = "Osho Player", artist: String = "Discourse") {
        val metadata = androidx.media3.common.MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle("The Morning Discourse")
            .setArtworkUri(Uri.parse("android.resource://com.oshoplayer.app/drawable/osho_logo"))
            .build()
        val mediaItem = MediaItem.Builder().setUri(uri).setMediaMetadata(metadata).build()
        primaryPlayer?.setMediaItem(mediaItem)
        primaryPlayer?.prepare()
        _playerState.update { it.copy(
            primaryTrackUri = uri, 
            primaryTrackTitle = title,
            primaryTrackArtist = artist,
            errorMessage = null
        ) }
    }

    fun setSecondaryTrack(uri: Uri, title: String = "Background", artist: String = "Music") {
        val metadata = androidx.media3.common.MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .build()
        val mediaItem = MediaItem.Builder().setUri(uri).setMediaMetadata(metadata).build()
        secondaryPlayer?.setMediaItem(mediaItem)
        secondaryPlayer?.prepare()
        _playerState.update { it.copy(
            secondaryTrackUri = uri, 
            secondaryTrackTitle = title,
            secondaryTrackArtist = artist,
            errorMessage = null
        ) }
    }

    fun clearSecondaryTrack() {
        secondaryPlayer?.stop()
        secondaryPlayer?.clearMediaItems()
        _playerState.update { it.copy(
            secondaryTrackUri = null,
            secondaryTrackTitle = "Background",
            secondaryTrackArtist = "Music"
        ) }
    }

    fun playBoth() {
        if (primaryPlayer?.playbackState == Player.STATE_ENDED) {
            primaryPlayer?.seekTo(0L)
        }
        
        val focusGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.requestAudioFocus(it) } == android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(
                audioFocusChangeListener,
                android.media.AudioManager.STREAM_MUSIC,
                android.media.AudioManager.AUDIOFOCUS_GAIN
            ) == android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
        
        if (focusGranted) {
            primaryPlayer?.playWhenReady = true
            secondaryPlayer?.playWhenReady = true
            _playerState.update { it.copy(isPlaying = true, errorMessage = null) }
        }
    }

    fun pauseBoth() {
        primaryPlayer?.pause()
        secondaryPlayer?.pause()
        _playerState.update { it.copy(isPlaying = false) }
    }

    fun togglePlayPause() {
        if (_playerState.value.isPlaying) {
            pauseBoth()
        } else {
            playBoth()
        }
        saveState()
    }

    fun seekPrimaryTo(positionMs: Long) {
        val duration = primaryPlayer?.duration?.takeIf { it > 0 } ?: Long.MAX_VALUE
        primaryPlayer?.seekTo(positionMs.coerceIn(0L, duration))
    }

    fun skipPrimaryBy(deltaMs: Long) {
        val current = primaryPlayer?.currentPosition ?: 0L
        seekPrimaryTo(current + deltaMs)
    }

    fun setPrimaryVolume(value: Float) {
        val normalized = value.coerceIn(0f, 1f)
        primaryPlayer?.volume = normalized
        _playerState.update { it.copy(primaryVolume = normalized) }
    }

    fun setSecondaryVolume(value: Float) {
        val normalized = value.coerceIn(0f, 1f)
        secondaryPlayer?.volume = normalized
        _playerState.update { it.copy(secondaryVolume = normalized) }
    }

    fun fadeOutAndStop(onComplete: () -> Unit) {
        fadeOutJob?.cancel()
        fadeOutJob = scope.launch {
            val startPrimary = primaryPlayer?.volume ?: 0f
            val startSecondary = secondaryPlayer?.volume ?: 0f
            _playerState.update { it.copy(isFadingOut = true) }

            val steps = 100
            repeat(steps + 1) { step ->
                val factor = 1f - (step / steps.toFloat())
                primaryPlayer?.volume = startPrimary * factor
                secondaryPlayer?.volume = startSecondary * factor
                delay(100L)
            }

            pauseBoth()
            restoreConfiguredVolumes()
            _playerState.update { it.copy(isFadingOut = false) }
            onComplete()
        }
    }

    fun restoreConfiguredVolumes() {
        primaryPlayer?.volume = _playerState.value.primaryVolume
        secondaryPlayer?.volume = _playerState.value.secondaryVolume
    }

    private fun attachListeners() {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                updateDurations()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val pPlaying = primaryPlayer?.isPlaying == true
                val sPlaying = secondaryPlayer?.isPlaying == true
                _playerState.update { it.copy(isPlaying = pPlaying || sPlaying) }
            }

            override fun onPlayerError(error: PlaybackException) {
                pauseBoth()
                _playerState.update { it.copy(errorMessage = error.message ?: "Audio playback failed.") }
            }
        }
        primaryPlayer?.addListener(listener)
        secondaryPlayer?.addListener(listener)
    }

    private fun updateDurations() {
        val primaryDuration = primaryPlayer?.duration?.takeIf { it > 0L } ?: 0L
        val secondaryDuration = secondaryPlayer?.duration?.takeIf { it > 0L } ?: 0L
        _playerState.update {
            it.copy(
                primaryDurationMs = primaryDuration,
                secondaryDurationMs = secondaryDuration
            )
        }
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = scope.launch {
            var counter = 0
            while (true) {
                delay(120L) // UI refresh rate optimized
                _progressState.update {
                    it.copy(
                        primaryProgressMs = primaryPlayer?.currentPosition?.coerceAtLeast(0L) ?: 0L,
                        secondaryProgressMs = secondaryPlayer?.currentPosition?.coerceAtLeast(0L) ?: 0L
                    )
                }
                counter++
                if (counter >= 40) { // save state every ~4.8 seconds
                    counter = 0
                    saveState()
                }
            }
        }
    }

    private fun saveState() {
        val p = prefs ?: return
        val state = _playerState.value
        val progress = _progressState.value
        p.edit().apply {
            if (state.primaryTrackUri != null) {
                putString("primaryTrackUri", state.primaryTrackUri.toString())
                putString("primaryTrackTitle", state.primaryTrackTitle)
                putString("primaryTrackArtist", state.primaryTrackArtist)
                putLong("primaryPositionMs", progress.primaryProgressMs)
            }
            if (state.secondaryTrackUri != null) {
                putString("secondaryTrackUri", state.secondaryTrackUri.toString())
                putString("secondaryTrackTitle", state.secondaryTrackTitle)
                putString("secondaryTrackArtist", state.secondaryTrackArtist)
                putLong("secondaryPositionMs", progress.secondaryProgressMs)
            }
            apply()
        }
    }

    private fun restoreState() {
        val p = prefs ?: return
        val pUri = p.getString("primaryTrackUri", null)
        if (pUri != null) {
            val pTitle = p.getString("primaryTrackTitle", "Osho Player") ?: "Osho Player"
            val pArtist = p.getString("primaryTrackArtist", "Discourse") ?: "Discourse"
            val pPos = p.getLong("primaryPositionMs", 0L)
            setPrimaryTrack(Uri.parse(pUri), pTitle, pArtist)
            primaryPlayer?.seekTo(pPos)
        }
        val sUri = p.getString("secondaryTrackUri", null)
        if (sUri != null) {
            val sTitle = p.getString("secondaryTrackTitle", "Background") ?: "Background"
            val sArtist = p.getString("secondaryTrackArtist", "Music") ?: "Music"
            val sPos = p.getLong("secondaryPositionMs", 0L)
            setSecondaryTrack(Uri.parse(sUri), sTitle, sArtist)
            secondaryPlayer?.seekTo(sPos)
        }
    }

    fun release() {
        progressJob?.cancel()
        fadeOutJob?.cancel()
        primaryPlayer?.release()
        secondaryPlayer?.release()
        primaryPlayer = null
        secondaryPlayer = null
        downloadCache?.release()
        downloadCache = null
    }
}
