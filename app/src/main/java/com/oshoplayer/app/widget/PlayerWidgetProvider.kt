package com.oshoplayer.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.oshoplayer.app.AudioPlaybackService
import com.oshoplayer.app.MainActivity
import com.oshoplayer.app.R
import com.oshoplayer.app.service.AudioPlayerManager

class PlayerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_player)

            // Setup Intents
            val launchIntent = Intent(context, MainActivity::class.java)
            val pendingLaunch = PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

            val playPauseIntent = Intent(context, AudioPlaybackService::class.java).apply { action = AudioPlaybackService.ACTION_PLAY_PAUSE }
            val pPlayPause = PendingIntent.getService(context, 1, playPauseIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

            val skipForwardIntent = Intent(context, AudioPlaybackService::class.java).apply { action = AudioPlaybackService.ACTION_SKIP_FORWARD }
            val pSkipForward = PendingIntent.getService(context, 2, skipForwardIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

            val skipBackIntent = Intent(context, AudioPlaybackService::class.java).apply { action = AudioPlaybackService.ACTION_SKIP_BACKWARD }
            val pSkipBack = PendingIntent.getService(context, 3, skipBackIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

            // Bind Intents
            views.setOnClickPendingIntent(R.id.widget_title, pendingLaunch)
            views.setOnClickPendingIntent(R.id.widget_btn_play_pause, pPlayPause)
            views.setOnClickPendingIntent(R.id.widget_btn_skip_forward, pSkipForward)
            views.setOnClickPendingIntent(R.id.widget_btn_skip_back, pSkipBack)

            // Update UI based on state
            val state = AudioPlayerManager.playerState.value
            val title = state.primaryTrackUri?.lastPathSegment ?: "Osho Player"
            views.setTextViewText(R.id.widget_title, title)
            
            val playPauseIcon = if (state.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            views.setImageViewResource(R.id.widget_btn_play_pause, playPauseIcon)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
