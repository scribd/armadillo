package com.scribd.armadillo.playback

import android.app.Notification
import android.support.v4.media.session.MediaSessionCompat
import com.scribd.armadillo.models.AudioPlayable

/**
 * This class
 * - should be implemented by the client in order to construct a [Notification] for the [PlaybackService]
 */
interface PlaybackNotificationBuilder {
    val notificationId: Int
    val channelId: String
    fun build(audioPlayable: AudioPlayable,
              currentChapterIndex: Int,
              isPlaying: Boolean,
              token: MediaSessionCompat.Token): Notification
}