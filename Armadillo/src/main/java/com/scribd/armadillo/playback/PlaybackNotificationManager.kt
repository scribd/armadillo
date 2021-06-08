package com.scribd.armadillo.playback

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import com.scribd.armadillo.R
import com.scribd.armadillo.models.AudioPlayable

/**
 * This class:
 * - builds a notification for a given [MediaBrowserServiceCompat]
 * - creates a notification channel for Oreo and greater devices
 */
internal class PlaybackNotificationManager(private val context: Context,
                                           private val playbackNotificationBuilder: PlaybackNotificationBuilder) {

    companion object {
        private val TAG = "PlaybackNotificationMgr"
    }

    var notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val hasOreo = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    init {
        // Cancel all notifications to handle the case where the Service was killed and
        // restarted by the system.
        notificationManager.cancelAll()
    }

    fun getNotification(audioPlayable: AudioPlayable,
                        currentChapterIndex: Int,
                        isPlaying: Boolean,
                        token: MediaSessionCompat.Token): Notification {
        maybeCreateChannel()
        return playbackNotificationBuilder.build(audioPlayable, currentChapterIndex, isPlaying, token)
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun maybeCreateChannel() {
        if (!hasOreo) {
            return
        }
        if (notificationManager.getNotificationChannel(playbackNotificationBuilder.channelId) == null) {
            val channel = NotificationChannel(playbackNotificationBuilder.channelId,
                    context.getString(R.string.arm_playback_notification_channel),
                    NotificationManager.IMPORTANCE_LOW)
            channel.enableLights(false)
            channel.enableVibration(false)
            channel.setShowBadge(false)
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            notificationManager.createNotificationChannel(channel)
        }
    }
}