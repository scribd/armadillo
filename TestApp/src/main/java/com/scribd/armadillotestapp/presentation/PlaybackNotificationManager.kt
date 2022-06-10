package com.scribd.armadillotestapp.presentation

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import com.scribd.armadillo.hasSnowCone
import com.scribd.armadillo.models.AudioPlayable
import com.scribd.armadillo.playback.PlaybackNotificationBuilder
import com.scribd.armadillotestapp.R

class PlaybackNotificationManager(private val context: Context) : PlaybackNotificationBuilder {
    private val playAction = buildAction(
        R.drawable.arm_play,
        R.string.audio_notification_play_action,
        PlaybackStateCompat.ACTION_PLAY)

    private val pauseAction = buildAction(
        R.drawable.arm_pause,
        R.string.audio_notification_pause_action,
        PlaybackStateCompat.ACTION_PAUSE)

    private val forwardAction = buildAction(
        R.drawable.arm_skipforward,
        R.string.audio_skip_fwd_action,
        PlaybackStateCompat.ACTION_FAST_FORWARD)

    private val rewindAction = buildAction(
        R.drawable.arm_skipback,
        R.string.audio_skip_back_action,
        PlaybackStateCompat.ACTION_REWIND)

    override val notificationId: Int = 415
    override val channelId: String = "playback_notification"

    override fun build(audioPlayable: AudioPlayable,
                       currentChapterIndex: Int,
                       isPlaying: Boolean,
                       token: MediaSessionCompat.Token): Notification =
        androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Content Title")
            .setContentIntent(buildContentIntent(audioPlayable))
            .setContentText("Content Text")
            .setTicker("ticker")
            .setAutoCancel(false)
            .setLocalOnly(true)
            .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
            .addAction(rewindAction)
            .addAction(if (isPlaying) pauseAction else playAction)
            .addAction(forwardAction)
            .setOngoing(isPlaying)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(token)
                .setShowActionsInCompactView(0, 1, 2)  //Indexes to items added in addAction(). Max 3 items.
                .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP))
                .setShowCancelButton(true))
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_favicon_round))
            .build()

    private fun buildContentIntent(audioPlayable: AudioPlayable): PendingIntent {
        val i = Intent(context, AudioPlayerActivity::class.java)
        i.putExtra(MainActivity.AUDIOBOOK_EXTRA, audioPlayable)
        val flag = if (hasSnowCone()) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getActivity(context, 711, i, flag)
    }

    private fun buildAction(icon: Int, stringRes: Int, action: Long): NotificationCompat.Action {
        return NotificationCompat.Action(
            icon,
            context.getString(stringRes),
            MediaButtonReceiver.buildMediaButtonPendingIntent(context, action))
    }
}