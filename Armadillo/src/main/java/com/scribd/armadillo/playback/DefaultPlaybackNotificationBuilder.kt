package com.scribd.armadillo.playback

import android.app.Notification
import android.content.Context
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.session.MediaButtonReceiver
import com.scribd.armadillo.models.AudioPlayable


class DefaultPlaybackNotificationBuilder(private val context: Context) : PlaybackNotificationBuilder {
    override val notificationId = 415
    override val channelId = "playback_notification"

    override fun build(
        audioPlayable: AudioPlayable, currentChapterIndex: Int, isPlaying: Boolean, token: MediaSessionCompat.Token): Notification {
        return androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(audioPlayable.title)
            .setContentText(audioPlayable.title)
            .setTicker(audioPlayable.title)
            .setAutoCancel(false)
            .setLocalOnly(true)
            .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
            .setOngoing(isPlaying)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(token)
                .setShowActionsInCompactView(0, 1, 2)  //Indexes to items added in addAction(). Max 3 items.
                .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP))
                .setShowCancelButton(true))
            .build()
    }
}