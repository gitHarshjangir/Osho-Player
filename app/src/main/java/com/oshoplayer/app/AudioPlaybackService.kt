package com.oshoplayer.app

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.oshoplayer.app.service.AudioPlayerManager
import com.oshoplayer.app.widget.PlayerWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaStyleNotificationHelper

class AudioPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var stateJob: Job? = null

    companion object {
        const val ACTION_PLAY_PAUSE = "com.oshoplayer.app.ACTION_PLAY_PAUSE"
        const val ACTION_SKIP_FORWARD = "com.oshoplayer.app.ACTION_SKIP_FORWARD"
        const val ACTION_SKIP_BACKWARD = "com.oshoplayer.app.ACTION_SKIP_BACKWARD"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        AudioPlayerManager.initialize(this)
        
        // Wrap the primary ExoPlayer with a MediaSession
        val player = AudioPlayerManager.primary
        if (player != null) {
            val forwardingPlayer = object : androidx.media3.common.ForwardingPlayer(player) {
                override fun play() {
                    AudioPlayerManager.playBoth()
                }

                override fun pause() {
                    AudioPlayerManager.pauseBoth()
                }
            }
            
            // Create a PendingIntent for the activity to be launched when user taps the notification
            val sessionActivityPendingIntent = android.app.PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                android.app.PendingIntent.FLAG_IMMUTABLE
            )

            // Build the MediaSession and bind the activity
            mediaSession = MediaSession.Builder(this, forwardingPlayer)
                .setSessionActivity(sessionActivityPendingIntent)
                .build()

            // Create notification channel
            val channel = android.app.NotificationChannel(
                "OshoPlayerChannel",
                "Osho Player",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Media playback controls"
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager?.createNotificationChannel(channel)

            // Start foreground with an initial notification
            startForeground(1, buildNotification(AudioPlayerManager.playerState.value))
        }

        stateJob = serviceScope.launch {
            AudioPlayerManager.playerState.collect { state ->
                // Update the system notification dynamically with the new state
                val manager = getSystemService(android.app.NotificationManager::class.java)
                manager?.notify(1, buildNotification(state))

                // Broadcast widget updates on state changes
                val intent = Intent(this@AudioPlaybackService, PlayerWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                }
                val ids = AppWidgetManager.getInstance(this@AudioPlaybackService)
                    .getAppWidgetIds(ComponentName(this@AudioPlaybackService, PlayerWidgetProvider::class.java))
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                sendBroadcast(intent)
            }
        }
    }

    private val largeIconBitmap by lazy {
        android.graphics.BitmapFactory.decodeResource(resources, com.oshoplayer.app.R.drawable.logo1)
    }

    private fun buildNotification(state: com.oshoplayer.app.service.PlayerState): android.app.Notification {
        val playIntent = android.app.PendingIntent.getService(
            this,
            0,
            Intent(this, AudioPlaybackService::class.java).setAction(ACTION_PLAY_PAUSE),
            android.app.PendingIntent.FLAG_IMMUTABLE
        )

        // Metadata extraction
        val discourseTitle = state.primaryTrackTitle ?: "Osho Player"
        val discourseArtist = state.primaryTrackArtist ?: "Osho Player"

        return androidx.core.app.NotificationCompat.Builder(this, "OshoPlayerChannel")
            .setSmallIcon(com.oshoplayer.app.R.drawable.ic_launcher_foreground)
            .setLargeIcon(largeIconBitmap)
            .setContentTitle(discourseTitle)    // Track name (primary)
            .setContentText(discourseArtist)    // Creator name (secondary)
            .setSubText("The Morning Discourse") // Album Title (container)
            .setOnlyAlertOnce(true)
            .addAction(androidx.core.app.NotificationCompat.Action(
                0, 
                if (state.isPlaying) "Pause" else "Play", 
                playIntent
            ))
            .setStyle(
                androidx.media3.session.MediaStyleNotificationHelper.MediaStyle(mediaSession!!)
                    .setShowActionsInCompactView(0)
            )
            // Unified Glassmorphism & Gold/White Accents
            .setColorized(true)
            .setColor(android.graphics.Color.parseColor("#E6D3A8")) // Light gold accent
            .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> AudioPlayerManager.togglePlayPause()
            ACTION_SKIP_FORWARD -> AudioPlayerManager.skipPrimaryBy(15_000L)
            ACTION_SKIP_BACKWARD -> AudioPlayerManager.skipPrimaryBy(-15_000L)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stateJob?.cancel()
        mediaSession?.release()
        mediaSession = null
        AudioPlayerManager.release()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!AudioPlayerManager.playerState.value.isPlaying) {
            stopSelf()
        }
    }
}
