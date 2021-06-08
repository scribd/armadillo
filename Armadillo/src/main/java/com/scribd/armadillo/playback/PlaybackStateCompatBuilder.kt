package com.scribd.armadillo.playback

import android.os.SystemClock
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.scribd.armadillo.models.PlaybackInfo
import com.scribd.armadillo.models.PlaybackState

/**
 * Builds a [PlaybackStateCompat] for our [MediaSessionCompat] in order to receive callbacks for media buttons
 */
internal interface PlaybackStateCompatBuilder {
    fun build(playbackInfo: PlaybackInfo): PlaybackStateCompat
}

internal class PlaybackStateBuilderImpl : PlaybackStateCompatBuilder {
    // Keeping Builder instance as per recommendation in the documentation
    // https://developer.android.com/guide/topics/media-apps/working-with-a-media-session#init-session
    private val stateBuilder = PlaybackStateCompat.Builder()

    override fun build(playbackInfo: PlaybackInfo): PlaybackStateCompat {
        val compatState = mapPlaybackState(playbackInfo.playbackState)
        val positionInDuration = playbackInfo.progress.positionInDuration
        val currentChapterStartTime = playbackInfo.audioPlayable.chapters[playbackInfo.progress.currentChapterIndex].startTime

        // The position in the current track.
        val currentPosition = positionInDuration - currentChapterStartTime

        stateBuilder.setState(compatState, currentPosition.longValue, playbackInfo.playbackSpeed);
        stateBuilder.setActions(getAvailableActions(compatState))
        return stateBuilder.build()
    }

    private fun getAvailableActions(playbackState: Int): Long {
        var actions = (PlaybackStateCompat.ACTION_FAST_FORWARD
            or PlaybackStateCompat.ACTION_REWIND
            or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            or PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH)
        actions = when (playbackState) {
            PlaybackStateCompat.STATE_PLAYING -> actions or (PlaybackStateCompat.ACTION_PAUSE
                or PlaybackStateCompat.ACTION_STOP
                or PlaybackStateCompat.ACTION_SEEK_TO
                or PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH)
            PlaybackStateCompat.STATE_PAUSED -> actions or (PlaybackStateCompat.ACTION_PLAY
                or PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
                or PlaybackStateCompat.ACTION_SEEK_TO
                or PlaybackStateCompat.ACTION_STOP)
            else -> actions or (PlaybackStateCompat.ACTION_PLAY
                or PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
                or PlaybackStateCompat.ACTION_PLAY
                or PlaybackStateCompat.ACTION_STOP
                or PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH)
        }
        return actions
    }

    /**
     * Gets corresponding [PlaybackStateCompat] state from Armadillo [PlaybackState]
     */
    private fun mapPlaybackState(playState: PlaybackState?): Int {
        return when (playState) {
            PlaybackState.PLAYING -> PlaybackStateCompat.STATE_PLAYING
            PlaybackState.PAUSED -> PlaybackStateCompat.STATE_PAUSED
            else -> PlaybackStateCompat.STATE_NONE
        }
    }
}

