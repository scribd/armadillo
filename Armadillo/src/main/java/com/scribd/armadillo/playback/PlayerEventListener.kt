package com.scribd.armadillo.playback

import android.content.Context
import android.util.Log
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.scribd.armadillo.StateStore
import com.scribd.armadillo.actions.Action
import com.scribd.armadillo.actions.ContentEndedAction
import com.scribd.armadillo.actions.ErrorAction
import com.scribd.armadillo.actions.LoadingAction
import com.scribd.armadillo.actions.PlayerStateAction
import com.scribd.armadillo.actions.SeekAction
import com.scribd.armadillo.actions.UpdateProgressAction
import com.scribd.armadillo.di.Injector
import com.scribd.armadillo.models.PlaybackState
import com.scribd.armadillo.time.milliseconds
import javax.inject.Inject

/**
 * Class to manage the events emitted by [ExoPlayer]
 *
 * It communicates changes by sending [Action]s with [StateStore.Modifier].
 */
internal class PlayerEventListener @Inject constructor(private val context: Context) : Player.Listener {
    init {
        Injector.mainComponent.inject(this)
    }

    private companion object {
        const val TAG = "PlayerEventListener"
    }

    @Inject
    internal lateinit var stateModifier: StateStore.Modifier

    override fun onPlayerError(error: PlaybackException) {
        val exception = (error as ExoPlaybackException).toArmadilloException(context)
        stateModifier.dispatch(ErrorAction(exception))
        Log.e(TAG, "onPlayerError: $error")
    }

    override fun onIsLoadingChanged(isLoading: Boolean) {
        Log.v(TAG, "onLoadingChanged --- isLoading: $isLoading")
        stateModifier.dispatch(LoadingAction(isLoading))
    }

    override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
        Log.v(TAG, "onPositionDiscontinuity --- reason: $reason")
        stateModifier.dispatch(SeekAction(false, newPosition.contentPositionMs.milliseconds))
    }

    override fun onRepeatModeChanged(repeatMode: Int) = Unit

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = Unit

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        val playState = when {
            isPlaying -> PlaybackState.PLAYING
            else -> PlaybackState.PAUSED
        }
        stateModifier.dispatch(PlayerStateAction(playState))
        Log.v(TAG, "onIsPlayingChanged --- --- isPlaying: $isPlaying")
    }

    override fun onPlaybackStateChanged(state: Int) {
        if (Player.STATE_ENDED == state) {
            stateModifier.dispatch(UpdateProgressAction(true))
            stateModifier.dispatch(ContentEndedAction)
        }
        Log.v(TAG, "onPlayerStateChanged --- --- playbackState: ${playbackState(state)}")
    }

    private fun playbackState(playbackState: Int): String {
        return when (playbackState) {
            Player.STATE_IDLE -> "idle"
            Player.STATE_BUFFERING -> "buffering"
            Player.STATE_READY -> "ready"
            Player.STATE_ENDED -> "ended"
            else -> "unknown"
        }
    }
}