package com.oshoplayer.app.viewmodel

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oshoplayer.app.service.AudioPlayerManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import io.github.jan.supabase.postgrest.postgrest
import java.util.Locale
import kotlin.math.roundToLong
import com.oshoplayer.app.data.RemoteAudio
import com.oshoplayer.app.data.CloudflareR2Repository

data class MixerTrack(
    val uri: Uri,
    val displayName: String,
    val durationMs: Long = 0L
)

data class DeviceAudio(
    val uri: Uri,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val sizeBytes: Long
)

enum class AudioRole {
    Discourse,
    Background
}

enum class PaymentStatus {
    None, Submitting, Success, Error
}

data class Playlist(
    val id: String,
    val name: String,
    val discourseUris: List<String>,
    val backgroundUris: List<String>,
    val photoUri: String? = null
) {
    fun toSerializedString(): String {
        val dStr = discourseUris.joinToString(",")
        val bStr = backgroundUris.joinToString(",")
        val pStr = photoUri ?: ""
        return "$id|$name|$dStr|$bStr|$pStr"
    }

    companion object {
        fun fromSerializedString(str: String): Playlist? {
            val parts = str.split("|")
            if (parts.size >= 4) {
                val dList = if (parts[2].isBlank()) emptyList() else parts[2].split(",")
                val bList = if (parts[3].isBlank()) emptyList() else parts[3].split(",")
                val pStr = if (parts.size >= 5 && parts[4].isNotBlank()) parts[4] else null
                return Playlist(parts[0], parts[1], dList, bList, pStr)
            }
            return null
        }
    }
}

data class AudioMixerUiState(
    val primaryTrack: MixerTrack? = null,
    val secondaryTrack: MixerTrack? = null,
    val deviceAudio: List<DeviceAudio> = emptyList(),
    val selectedDeviceAudio: DeviceAudio? = null,
    val pendingAudioRole: AudioRole? = null,
    val isScanningAudio: Boolean = false,
    val sleepTimerRemainingMs: Long = 0L,
    val hapticsEnabled: Boolean = true,
    val isActivating: Boolean = false,
    val updateMessage: String? = null,
    val stopAtEndOfDiscourse: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val savedDiscourseUris: Set<String> = emptySet(),
    val savedBackgroundUris: Set<String> = emptySet(),
    val savedPlaylistStrings: Set<String> = emptySet(),
    val oshoPlusDiscourses: List<RemoteAudio> = emptyList(),
    val isOshoPlusLoading: Boolean = false,
    val builtInBackgrounds: List<DeviceAudio> = emptyList(),
    val builtInDiscourses: List<DeviceAudio> = emptyList(),
    val isPremium: Boolean = false,
    val showPaywall: Boolean = false,
    val deviceId: String = "",
    val showSessionKickoutToast: Boolean = false,
    val activationCode: String = "",
    val activationError: String? = null,
    val paymentSubmissionStatus: PaymentStatus = PaymentStatus.None,
    val paymentThrottleTimerMs: Long = 0L,
    val isPaymentPending: Boolean = false
) {
    val canPlay: Boolean
        get() = primaryTrack != null && secondaryTrack != null

    val savedDiscourses: List<DeviceAudio>
        get() = deviceAudio.filter { savedDiscourseUris.contains(it.uri.toString()) }
        
    val savedBackgrounds: List<DeviceAudio>
        get() = deviceAudio.filter { savedBackgroundUris.contains(it.uri.toString()) }

    val savedPlaylists: List<Playlist>
        get() = savedPlaylistStrings.mapNotNull { Playlist.fromSerializedString(it) }

    val filteredDeviceAudio: List<DeviceAudio>
        get() = if (searchQuery.isBlank()) {
            deviceAudio
        } else {
            deviceAudio.filter { 
                it.title.contains(searchQuery, ignoreCase = true) || 
                it.artist.contains(searchQuery, ignoreCase = true) 
            }
        }
}

class AudioMixerViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext

    private val _uiState = MutableStateFlow(AudioMixerUiState())
    val uiState: StateFlow<AudioMixerUiState> = _uiState.asStateFlow()

    // Expose manager states for UI to collect efficiently without full recomposition
    val playerState = AudioPlayerManager.playerState
    val progressState = AudioPlayerManager.progressState

    private var sleepTimerJob: Job? = null
    private var lastPrimarySliderTick = -1
    private var lastSecondarySliderTick = -1

    private val prefs = context.getSharedPreferences("OshoLibrary", Context.MODE_PRIVATE)
    private val r2Repository = CloudflareR2Repository()

    init {
        AudioPlayerManager.initialize(context)
        
        // Restore tracks if AudioPlayerManager (foreground service) is already running
        val initialPlayerState = AudioPlayerManager.playerState.value
        if (initialPlayerState.primaryTrackUri != null) {
            _uiState.update { 
                it.copy(primaryTrack = MixerTrack(
                    uri = initialPlayerState.primaryTrackUri, 
                    displayName = initialPlayerState.primaryTrackTitle
                )) 
            }
        }
        if (initialPlayerState.secondaryTrackUri != null) {
            _uiState.update { 
                it.copy(secondaryTrack = MixerTrack(
                    uri = initialPlayerState.secondaryTrackUri, 
                    displayName = initialPlayerState.secondaryTrackTitle
                )) 
            }
        }

        // Start foreground service
        val intent = android.content.Intent(context, com.oshoplayer.app.AudioPlaybackService::class.java)
        context.startService(intent)
        
        val builtin = listOf(
            DeviceAudio(
                uri = Uri.parse("android.resource://${context.packageName}/${com.oshoplayer.app.R.raw.hotline_bling}"),
                title = "Hotline Bling",
                artist = "Osho Player",
                durationMs = 0L,
                sizeBytes = 4369900L
            ),
            DeviceAudio(
                uri = Uri.parse("android.resource://${context.packageName}/${com.oshoplayer.app.R.raw.icarus}"),
                title = "ICARUS",
                artist = "Osho Player",
                durationMs = 0L,
                sizeBytes = 4858608L
            ),
            DeviceAudio(
                uri = Uri.parse("android.resource://${context.packageName}/${com.oshoplayer.app.R.raw.interstellar}"),
                title = "Interstellar",
                artist = "Osho Player",
                durationMs = 0L,
                sizeBytes = 3597511L
            )
        )
        
        val builtinDiscourses = listOf(
            DeviceAudio(
                uri = Uri.parse("android.resource://${context.packageName}/${com.oshoplayer.app.R.raw.ashtavakra_maha_geeta_01}"),
                title = "Ashtavakra Maha Geeta 01",
                artist = "Osho",
                durationMs = 0L,
                sizeBytes = 39621073L
            ),
            DeviceAudio(
                uri = Uri.parse("android.resource://${context.packageName}/${com.oshoplayer.app.R.raw.es_dhammo_sanantano}"),
                title = "Es dhammo sanantano",
                artist = "Osho",
                durationMs = 0L,
                sizeBytes = 38204296L
            )
        )

        val localSessionToken = prefs.getString("local_session_token", null) ?: java.util.UUID.randomUUID().toString().also {
            prefs.edit().putString("local_session_token", it).apply()
        }

        var fallbackKey = prefs.getString("fallback_osho_key", null)
        if (fallbackKey != null && fallbackKey.length > 6) {
            fallbackKey = null // Clear any UUIDs generated during testing
        }
        val oshoKey = prefs.getString("custom_osho_key", null) ?: fallbackKey ?: (100000..999999).random().toString().also {
            prefs.edit().putString("fallback_osho_key", it).apply()
        }

        // Load library from SharedPreferences
        _uiState.update { 
            it.copy(
                builtInBackgrounds = builtin,
                builtInDiscourses = builtinDiscourses,
                savedDiscourseUris = prefs.getStringSet("saved_discourses", emptySet())?.toSet() ?: emptySet(),
                savedBackgroundUris = prefs.getStringSet("saved_backgrounds", emptySet())?.toSet() ?: emptySet(),
                savedPlaylistStrings = prefs.getStringSet("saved_playlists", emptySet())?.toSet() ?: emptySet(),
                deviceId = oshoKey,
                isPremium = prefs.getBoolean("is_premium", false)
            )
        }
        
        viewModelScope.launch {
            try {
                com.oshoplayer.app.data.Supabase.client.postgrest["app_analytics"].upsert(
                    com.oshoplayer.app.data.AppAnalytics(
                        osho_key = oshoKey,
                        device_id = localSessionToken,
                        device_name = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
                    )
                )
            } catch (e: Exception) {
                // Ignore analytics error
            }
            
            try {
                val sub = com.oshoplayer.app.data.Supabase.client.postgrest["osho_subscriptions"]
                    .select { filter { eq("osho_key", oshoKey) } }
                    .decodeSingleOrNull<com.oshoplayer.app.data.Subscription>()
                
                if (sub == null || !sub.is_activated) {
                    prefs.edit().putBoolean("is_premium", false).apply()
                    val isPending = sub != null && !sub.is_activated && sub.utr_number != null
                    _uiState.update { it.copy(isPremium = false, isPaymentPending = isPending) }
                } else if (sub.current_session_token != null && sub.current_session_token != localSessionToken) {
                    // SILENT DISCONNECTION
                    prefs.edit().putBoolean("is_premium", false).apply()
                    _uiState.update { it.copy(isPremium = false, showSessionKickoutToast = true) }
                } else {
                    prefs.edit().putBoolean("is_premium", true).apply()
                    _uiState.update { it.copy(isPremium = true) }
                }
            } catch (e: Exception) {
                // Offline or network error: gracefully rely strictly on local state
            }
        }

        val lastSubTime = prefs.getLong("last_payment_submission", 0L)
        val elapsed = System.currentTimeMillis() - lastSubTime
        if (elapsed < 9 * 60 * 1000L) {
            startThrottleTimer((9 * 60 * 1000L) - elapsed)
        }

        // Eagerly pre-warm local audio lists
        scanDeviceAudio()
        fetchOshoPlusDiscourses()
        
        // Listen to player state to check if we should stop at end of discourse
        viewModelScope.launch {
            playerState.collect { pState ->
                if (!pState.isPlaying && pState.primaryDurationMs > 0 && pState.primaryDurationMs == progressState.value.primaryProgressMs) {
                    if (_uiState.value.stopAtEndOfDiscourse) {
                        AudioPlayerManager.fadeOutAndStop {
                            _uiState.update { it.copy(stopAtEndOfDiscourse = false) }
                        }
                    }
                    
                    // Show paywall if 3rd discourse just finished and not premium
                    val ui = _uiState.value
                    if (!ui.isPremium) {
                        val currentName = ui.primaryTrack?.displayName
                        val currentAudio = ui.oshoPlusDiscourses.find { it.title == currentName }
                        if (currentAudio != null) {
                            val folderDiscourses = ui.oshoPlusDiscourses.filter { it.folder == currentAudio.folder }
                            val top3Keys = folderDiscourses.take(3).map { it.key }
                            if (top3Keys.lastOrNull() == currentAudio.key) {
                                _uiState.update { it.copy(showPaywall = true) }
                            }
                        }
                    }
                }
            }
        }
    }

    fun selectPrimaryTrack(uri: Uri) {
        persistReadPermission(uri)
        val ui = _uiState.value
        val audio = ui.deviceAudio.find { it.uri == uri }
            ?: ui.builtInDiscourses.find { it.uri == uri }
            ?: ui.savedDiscourses.find { it.uri == uri }
        val name = audio?.title ?: resolveDisplayName(uri)
        val artist = audio?.artist ?: "Discourse"
        AudioPlayerManager.setPrimaryTrack(uri, name, artist)
        _uiState.update {
            it.copy(
                primaryTrack = MixerTrack(uri = uri, displayName = name),
                errorMessage = null
            )
        }
        performHaptic(HapticPattern.Button)
    }

    fun fetchOshoPlusDiscourses() {
        if (_uiState.value.oshoPlusDiscourses.isNotEmpty()) return
        _uiState.update { it.copy(isOshoPlusLoading = true) }
        viewModelScope.launch {
            try {
                val discourses = r2Repository.listDiscourses()
                _uiState.update { it.copy(oshoPlusDiscourses = discourses, isOshoPlusLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isOshoPlusLoading = false, errorMessage = "Osho+ Error: ${e.message ?: e.toString()}") }
            }
        }
    }

    fun playOshoPlusDiscourse(audio: RemoteAudio) {
        val ui = _uiState.value
        if (!ui.isPremium) {
            val folderDiscourses = ui.oshoPlusDiscourses.filter { it.folder == audio.folder }
            val top3Keys = folderDiscourses.take(3).map { it.key }
            if (!top3Keys.contains(audio.key)) {
                _uiState.update { it.copy(showPaywall = true) }
                return
            }
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(updateMessage = "Loading streaming URL...") }
                val url = r2Repository.getStreamingUrl(audio.key)
                val uri = Uri.parse(url)
                val name = audio.title
                AudioPlayerManager.setPrimaryTrack(uri, name, "Osho+")
                _uiState.update {
                    it.copy(
                        primaryTrack = MixerTrack(uri = uri, displayName = name),
                        errorMessage = null,
                        updateMessage = "Playing ${audio.title}"
                    )
                }
                performHaptic(HapticPattern.Button)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to play discourse: ${e.message}", updateMessage = null) }
            }
        }
    }

    fun selectSecondaryTrack(uri: Uri) {
        persistReadPermission(uri)
        val ui = _uiState.value
        val audio = ui.deviceAudio.find { it.uri == uri }
            ?: ui.builtInBackgrounds.find { it.uri == uri }
            ?: ui.savedBackgrounds.find { it.uri == uri }
        val name = audio?.title ?: resolveDisplayName(uri)
        val artist = audio?.artist ?: "Background"
        AudioPlayerManager.setSecondaryTrack(uri, name, artist)
        _uiState.update {
            it.copy(
                secondaryTrack = MixerTrack(uri = uri, displayName = name),
                errorMessage = null
            )
        }
        performHaptic(HapticPattern.Button)
    }

    fun clearSecondaryTrack() {
        AudioPlayerManager.clearSecondaryTrack()
        _uiState.update {
            it.copy(
                secondaryTrack = null,
                errorMessage = null
            )
        }
        performHaptic(HapticPattern.Button)
    }

    fun scanDeviceAudio() {
        if (_uiState.value.deviceAudio.isNotEmpty() && !_uiState.value.isScanningAudio) return
        _uiState.update {
            it.copy(
                isScanningAudio = true,
                errorMessage = null,
                updateMessage = null
            )
        }
        viewModelScope.launch {
            val audio = loadDeviceAudio()
            _uiState.update {
                it.copy(
                    deviceAudio = audio,
                    isScanningAudio = false,
                    errorMessage = if (audio.isEmpty() && it.errorMessage == null) "No audio files were found on this device." else it.errorMessage
                )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectDeviceAudio(audio: DeviceAudio) {
        _uiState.update {
            it.copy(
                selectedDeviceAudio = audio,
                pendingAudioRole = null,
                errorMessage = null
            )
        }
        performHaptic(HapticPattern.Button)
    }

    fun chooseAudioRole(role: AudioRole) {
        _uiState.update { it.copy(pendingAudioRole = role) }
        performHaptic(HapticPattern.Switch)
    }

    fun confirmSelectedAudio() {
        val audio = _uiState.value.selectedDeviceAudio ?: return
        when (_uiState.value.pendingAudioRole) {
            AudioRole.Discourse -> assignPrimaryTrack(audio)
            AudioRole.Background -> assignSecondaryTrack(audio)
            null -> {
                _uiState.update { it.copy(errorMessage = "Choose how to use this audio first.") }
                return
            }
        }
        _uiState.update {
            it.copy(
                selectedDeviceAudio = null,
                pendingAudioRole = null,
                errorMessage = null
            )
        }
        performHaptic(HapticPattern.Button)
    }

    fun dismissAudioAssignment() {
        _uiState.update {
            it.copy(
                selectedDeviceAudio = null,
                pendingAudioRole = null
            )
        }
    }

    fun saveToLibrary(audio: DeviceAudio, role: AudioRole) {
        val uriStr = audio.uri.toString()
        _uiState.update { state ->
            val newDiscourses = if (role == AudioRole.Discourse) state.savedDiscourseUris + uriStr else state.savedDiscourseUris
            val newBackgrounds = if (role == AudioRole.Background) state.savedBackgroundUris + uriStr else state.savedBackgroundUris
            
            prefs.edit()
                .putStringSet("saved_discourses", newDiscourses)
                .putStringSet("saved_backgrounds", newBackgrounds)
                .apply()
                
            state.copy(
                savedDiscourseUris = newDiscourses,
                savedBackgroundUris = newBackgrounds,
                updateMessage = "Added to Library"
            )
        }
        performHaptic(HapticPattern.Tick)
    }

    fun removeFromLibrary(audio: DeviceAudio, role: AudioRole) {
        val uriStr = audio.uri.toString()
        _uiState.update { state ->
            val newDiscourses = if (role == AudioRole.Discourse) state.savedDiscourseUris - uriStr else state.savedDiscourseUris
            val newBackgrounds = if (role == AudioRole.Background) state.savedBackgroundUris - uriStr else state.savedBackgroundUris
            
            prefs.edit()
                .putStringSet("saved_discourses", newDiscourses)
                .putStringSet("saved_backgrounds", newBackgrounds)
                .apply()
                
            state.copy(
                savedDiscourseUris = newDiscourses,
                savedBackgroundUris = newBackgrounds
            )
        }
        performHaptic(HapticPattern.Tick)
    }

    fun dismissPaywall() {
        _uiState.update { it.copy(showPaywall = false) }
    }

    fun consumeSessionKickoutToast() {
        _uiState.update { it.copy(showSessionKickoutToast = false) }
    }

    fun verifyPurchase(customKey: String? = null) {
        _uiState.update { it.copy(isActivating = true, activationError = null, updateMessage = null) }
        viewModelScope.launch {
            try {
                val oshoKeyToVerify = if (!customKey.isNullOrBlank()) customKey else _uiState.value.deviceId
                val sub = com.oshoplayer.app.data.Supabase.client.postgrest["osho_subscriptions"]
                    .select { filter { eq("osho_key", oshoKeyToVerify) } }
                    .decodeSingleOrNull<com.oshoplayer.app.data.Subscription>()
                
                if (sub == null || !sub.is_activated) {
                    _uiState.update { it.copy(isActivating = false, activationError = "Not activated yet", updateMessage = null) }
                    return@launch
                }
                
                val localSessionToken = prefs.getString("local_session_token", "") ?: ""
                com.oshoplayer.app.data.Supabase.client.postgrest["osho_subscriptions"].update(
                    com.oshoplayer.app.data.SubscriptionTokenUpdate(current_session_token = localSessionToken)
                ) {
                    filter { eq("osho_key", oshoKeyToVerify) }
                }
                
                if (!customKey.isNullOrBlank()) {
                    prefs.edit().putString("custom_osho_key", customKey).apply()
                }
                
                prefs.edit()
                    .putBoolean("is_premium", true)
                    .apply()
                    
                _uiState.update { 
                    it.copy(
                        deviceId = oshoKeyToVerify,
                        isPremium = true,
                        isActivating = false,
                        activationError = null, 
                        updateMessage = "Successfully activated premium features!",
                        showPaywall = false
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isActivating = false, activationError = "Error: ${e.message}", updateMessage = null) }
            }
        }
    }

    private var throttleJob: Job? = null
    private fun startThrottleTimer(durationMs: Long) {
        throttleJob?.cancel()
        _uiState.update { it.copy(paymentThrottleTimerMs = durationMs) }
        throttleJob = viewModelScope.launch {
            while (_uiState.value.paymentThrottleTimerMs > 0) {
                delay(1000)
                _uiState.update { it.copy(paymentThrottleTimerMs = (it.paymentThrottleTimerMs - 1000).coerceAtLeast(0)) }
            }
        }
    }

    fun submitPayment(utr: String, imageUri: Uri) {
        val ui = _uiState.value
        val lastSubTime = prefs.getLong("last_payment_submission", 0L)
        val now = System.currentTimeMillis()
        val elapsed = now - lastSubTime
        if (elapsed < 9 * 60 * 1000L) {
            val remaining = (9 * 60 * 1000L) - elapsed
            _uiState.update { it.copy(activationError = "Please wait before trying again.", paymentThrottleTimerMs = remaining) }
            startThrottleTimer(remaining)
            return
        }

        _uiState.update { it.copy(paymentSubmissionStatus = PaymentStatus.Submitting, activationError = null) }
        viewModelScope.launch {
            try {
                val sub = com.oshoplayer.app.data.Supabase.client.postgrest["osho_subscriptions"]
                    .select { filter { eq("osho_key", ui.deviceId) } }
                    .decodeSingleOrNull<com.oshoplayer.app.data.Subscription>()
                
                if (sub != null && !sub.is_activated && sub.utr_number != null) {
                    _uiState.update { it.copy(paymentSubmissionStatus = PaymentStatus.None, isPaymentPending = true, activationError = "Request already pending.") }
                    return@launch
                }

                val bytes = context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
                if (bytes == null) {
                    _uiState.update { it.copy(paymentSubmissionStatus = PaymentStatus.Error, activationError = "Failed to read image") }
                    return@launch
                }

                val fileName = "${ui.deviceId}_${System.currentTimeMillis()}.jpg"
                val url = r2Repository.uploadScreenshot(fileName, bytes)

                val newSub = com.oshoplayer.app.data.Subscription(
                    osho_key = ui.deviceId,
                    is_activated = false,
                    current_session_token = prefs.getString("local_session_token", ""),
                    utr_number = utr
                )
                
                if (sub != null) {
                    com.oshoplayer.app.data.Supabase.client.postgrest["osho_subscriptions"].update(
                        {
                            set("is_activated", false)
                            set("utr_number", utr)
                            set("current_session_token", prefs.getString("local_session_token", ""))
                        }
                    ) {
                        filter { eq("osho_key", ui.deviceId) }
                    }
                } else {
                    com.oshoplayer.app.data.Supabase.client.postgrest["osho_subscriptions"].insert(newSub)
                }

                prefs.edit().putLong("last_payment_submission", System.currentTimeMillis()).apply()
                _uiState.update { it.copy(paymentSubmissionStatus = PaymentStatus.Success, isPaymentPending = true) }
                startThrottleTimer(9 * 60 * 1000L)

            } catch (e: Exception) {
                _uiState.update { it.copy(paymentSubmissionStatus = PaymentStatus.Error, activationError = e.message ?: "Submission failed") }
            }
        }
    }

    fun deactivatePremium() {
        val ui = _uiState.value
        viewModelScope.launch {
            try {
                com.oshoplayer.app.data.Supabase.client.postgrest["osho_subscriptions"].update(
                    { set("current_session_token", null as String?) }
                ) {
                    filter { eq("osho_key", ui.deviceId) }
                }
            } catch (e: Exception) {
                // Best effort
            }
            prefs.edit().putBoolean("is_premium", false).apply()
            _uiState.update { it.copy(isPremium = false, updateMessage = "Premium deactivated on this device.") }
        }
    }

    fun savePlaylist(name: String, photoUri: String? = null, discourseUris: List<String> = emptyList(), backgroundUris: List<String> = emptyList()) {
        val id = java.util.UUID.randomUUID().toString()
        val playlist = Playlist(id, name, discourseUris, backgroundUris, photoUri)
        _uiState.update { state ->
            val newPlaylists = state.savedPlaylistStrings + playlist.toSerializedString()
            prefs.edit().putStringSet("saved_playlists", newPlaylists).apply()
            state.copy(
                savedPlaylistStrings = newPlaylists,
                updateMessage = "Playlist saved"
            )
        }
        performHaptic(HapticPattern.Tick)
    }

    fun updatePlaylist(playlist: Playlist) {
        _uiState.update { state ->
            // Find existing playlist string with this ID
            val existingStrings = state.savedPlaylistStrings.filter { it.startsWith("${playlist.id}|") }
            val newPlaylists = state.savedPlaylistStrings - existingStrings.toSet() + playlist.toSerializedString()
            prefs.edit().putStringSet("saved_playlists", newPlaylists).apply()
            state.copy(
                savedPlaylistStrings = newPlaylists
            )
        }
        performHaptic(HapticPattern.Tick)
    }

    fun deletePlaylist(playlist: Playlist) {
        _uiState.update { state ->
            val existingStrings = state.savedPlaylistStrings.filter { it.startsWith("${playlist.id}|") }
            val newPlaylists = state.savedPlaylistStrings - existingStrings.toSet()
            prefs.edit().putStringSet("saved_playlists", newPlaylists).apply()
            state.copy(
                savedPlaylistStrings = newPlaylists
            )
        }
        performHaptic(HapticPattern.Tick)
    }

    fun togglePlayPause() {
        if (!_uiState.value.canPlay) {
            _uiState.update { it.copy(errorMessage = "Select both audio files before playback.") }
            return
        }
        AudioPlayerManager.togglePlayPause()
        performHaptic(HapticPattern.Button)
    }

    fun playAudio() {
        AudioPlayerManager.playBoth()
        performHaptic(HapticPattern.Button)
    }

    fun seekPrimaryTo(positionMs: Long) {
        AudioPlayerManager.seekPrimaryTo(positionMs)
        performHaptic(HapticPattern.Tick)
    }

    fun skipPrimaryBy(deltaMs: Long) {
        AudioPlayerManager.skipPrimaryBy(deltaMs)
    }

    fun setPrimaryVolume(value: Float) {
        AudioPlayerManager.setPrimaryVolume(value)
        performSliderHaptic(value, isPrimary = true)
    }

    fun setSecondaryVolume(value: Float) {
        AudioPlayerManager.setSecondaryVolume(value)
        performSliderHaptic(value, isPrimary = false)
    }

    fun startSleepTimer(minutes: Long) {
        if (minutes <= 0L) return
        sleepTimerJob?.cancel()
        _uiState.update {
            it.copy(
                sleepTimerRemainingMs = minutes * 60_000L
            )
        }
        sleepTimerJob = viewModelScope.launch {
            while (_uiState.value.sleepTimerRemainingMs > 0L) {
                delay(1_000L)
                _uiState.update {
                    it.copy(
                        sleepTimerRemainingMs = (it.sleepTimerRemainingMs - 1_000L).coerceAtLeast(0L)
                    )
                }
            }
            AudioPlayerManager.fadeOutAndStop {
                _uiState.update { it.copy(sleepTimerRemainingMs = 0L) }
            }
        }
        performHaptic(HapticPattern.Button)
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _uiState.update {
            it.copy(
                sleepTimerRemainingMs = 0L,
                stopAtEndOfDiscourse = false
            )
        }
        AudioPlayerManager.restoreConfiguredVolumes()
        performHaptic(HapticPattern.Button)
    }

    fun toggleStopAtEndOfDiscourse() {
        _uiState.update { it.copy(stopAtEndOfDiscourse = !it.stopAtEndOfDiscourse, sleepTimerRemainingMs = 0L) }
        sleepTimerJob?.cancel()
        performHaptic(HapticPattern.Switch)
    }

    fun setHapticsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(hapticsEnabled = enabled) }
        if (enabled) performHaptic(HapticPattern.Switch)
    }



    fun checkForUpdates() {
        _uiState.update { it.copy(updateMessage = "You are using the latest version.") }
        performHaptic(HapticPattern.Button)
    }

    private fun assignPrimaryTrack(audio: DeviceAudio) {
        AudioPlayerManager.setPrimaryTrack(audio.uri, audio.title, audio.artist)
        _uiState.update {
            it.copy(
                primaryTrack = MixerTrack(audio.uri, audio.title, audio.durationMs)
            )
        }
    }

    private fun assignSecondaryTrack(audio: DeviceAudio) {
        AudioPlayerManager.setSecondaryTrack(audio.uri, audio.title, audio.artist)
        _uiState.update {
            it.copy(
                secondaryTrack = MixerTrack(audio.uri, audio.title, audio.durationMs)
            )
        }
    }

    private suspend fun loadDeviceAudio(): List<DeviceAudio> {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE
        )
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        return runCatching {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                context.contentResolver.query(
                    collection,
                    projection,
                    "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                    null,
                    sortOrder
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                    val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                    val items = mutableListOf<DeviceAudio>()
    
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val title = cursor.getString(titleColumn)
                            ?: cursor.getString(displayNameColumn)
                            ?: "Untitled audio"
                        val artist = cursor.getString(artistColumn) ?: "Unknown artist"
                        items += DeviceAudio(
                            uri = ContentUris.withAppendedId(collection, id),
                            title = title,
                            artist = artist,
                            durationMs = cursor.getLong(durationColumn).coerceAtLeast(0L),
                            sizeBytes = cursor.getLong(sizeColumn).coerceAtLeast(0L)
                        )
                    }
                    items
                } ?: emptyList()
            }
        }.getOrElse { error ->
            _uiState.update {
                it.copy(
                    isScanningAudio = false,
                    errorMessage = error.message ?: "Unable to scan audio files."
                )
            }
            emptyList()
        }
    }

    private fun persistReadPermission(uri: Uri) {
        if (uri.scheme == "http" || uri.scheme == "https") return
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    private fun resolveDisplayName(uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (column >= 0) {
                        return cursor.getString(column)
                    }
                }
            }
        return uri.lastPathSegment ?: "Selected audio"
    }

    private fun performSliderHaptic(value: Float, isPrimary: Boolean) {
        val tick = (value * 20f).roundToLong().toInt()
        if (isPrimary) {
            if (tick == lastPrimarySliderTick) return
            lastPrimarySliderTick = tick
        } else {
            if (tick == lastSecondarySliderTick) return
            lastSecondarySliderTick = tick
        }
        performHaptic(HapticPattern.Tick)
    }

    private fun performHaptic(pattern: HapticPattern) {
        if (!_uiState.value.hapticsEnabled) return

        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val timings = when (pattern) {
            HapticPattern.Button -> longArrayOf(0L, 9L, 18L, 9L)
            HapticPattern.Tick -> longArrayOf(0L, 7L)
            HapticPattern.Switch -> longArrayOf(0L, 12L, 24L, 10L)
        }
        val amplitudes = when (pattern) {
            HapticPattern.Button -> intArrayOf(0, 110, 0, 70)
            HapticPattern.Tick -> intArrayOf(0, 65)
            HapticPattern.Switch -> intArrayOf(0, 125, 0, 85)
        }

        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    }

    override fun onCleared() {
        sleepTimerJob?.cancel()
        super.onCleared()
    }

    private enum class HapticPattern {
        Button,
        Tick,
        Switch
    }
}
