package com.scribd.armadillo.playback

import android.media.MediaMetadata
import android.support.v4.media.MediaMetadataCompat
import com.scribd.armadillo.models.AudioPlayable
import com.scribd.armadillo.models.PlaybackProgress
import com.scribd.armadillo.time.milliseconds

/**
 * Used for building the [MediaMetadataCompat] for a given [AudioPlayable].
 *
 * Necessary for enabling Bluetooth media controls. Without metadata, playback state updates (e.g. Playing/Paused) may not be fully
 * propagated through the OS, particularly on API 28+, leading to weird behavior like Bluetooth devices sending 'Play' commands
 * instead of 'Pause' when the audioPlayable is already playing.
 */
internal interface MediaMetadataCompatBuilder {
    fun build(audioPlayable: AudioPlayable, playbackProgress: PlaybackProgress? = null): MediaMetadataCompat
}

internal class MediaMetadataCompatBuilderImpl : MediaMetadataCompatBuilder {
    override fun build(audioPlayable: AudioPlayable, playbackProgress: PlaybackProgress?): MediaMetadataCompat {

        val chapterTitle = playbackProgress?.let { audioPlayable.chapters[it.currentChapterIndex].title } ?: ""
        val duration = playbackProgress?.let { audioPlayable.chapters[it.currentChapterIndex].duration } ?: (-1).milliseconds

        return MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, chapterTitle)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, audioPlayable.title)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration.longValue)
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, audioPlayable.id.toString())
            .build()

    }
}