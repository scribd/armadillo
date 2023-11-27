package com.scribd.armadillo.playback

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.view.KeyEvent
import androidx.annotation.VisibleForTesting
import androidx.core.content.IntentCompat
import com.scribd.armadillo.ArmadilloConfiguration
import com.scribd.armadillo.Constants
import com.scribd.armadillo.Constants.AUDIO_POSITION_SHIFT_IN_MS
import com.scribd.armadillo.Milliseconds
import com.scribd.armadillo.StateStore
import com.scribd.armadillo.actions.CustomMediaSessionAction
import com.scribd.armadillo.actions.UpdateProgressAction
import com.scribd.armadillo.di.Injector
import com.scribd.armadillo.extensions.CustomAction
import com.scribd.armadillo.mediaitems.ArmadilloMediaBrowse
import com.scribd.armadillo.models.AudioPlayable
import com.scribd.armadillo.models.PlaybackInfo
import com.scribd.armadillo.models.PlaybackState
import com.scribd.armadillo.time.milliseconds
import javax.inject.Inject

/**
 * This class:
 *  - holds a [AudioPlaybackEngine] and executes its methods based upon commands received by [MediaControllerCompat.TransportControls]
 *  - communicates updates back to the [PlaybackService] with [PlaybackService.PlaybackStateListener] (to update foreground notification)
 */
internal class MediaSessionCallback(private val onMediaSessionEventListener: OnMediaSessionEventListener?) : MediaSessionCompat.Callback() {
    init {
        Injector.mainComponent.inject(this)
    }

    private companion object {
        const val TAG = "MediaSessionCallback"
    }

    @Inject
    internal lateinit var playbackEngineFactory: PlaybackEngineFactory

    @Inject
    internal lateinit var stateProvider: StateStore.Provider

    @Inject
    internal lateinit var stateModifier: StateStore.Modifier

    @Inject
    internal lateinit var mediaBrowser: ArmadilloMediaBrowse.Browser

    @VisibleForTesting
    internal var playbackEngine: AudioPlaybackEngine? = null

    @VisibleForTesting
    internal var isPlaying = false

    @VisibleForTesting
    internal var isWaitingForDoubleTap = false

    @VisibleForTesting
    internal var doubleTapTimer: CountDownTimer? = null

    // This is 2/3 of ViewConfiguration's double-tap timeout. Using the full time gave a sense of lag on the player notification, so we
    // decided to shorten it instead.
    @VisibleForTesting
    internal val doubleTapTimeOut = 200L

    private val playbackInfo: PlaybackInfo?
        get() = stateProvider.currentState.playbackInfo

    private inner class DoubleTapTimer : CountDownTimer(doubleTapTimeOut, doubleTapTimeOut) {
        override fun onTick(p0: Long) {}

        override fun onFinish() {
            playPauseAfterTimeout()
        }
    }

    override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
        // double-tapping play/pause buttons to skip is default behaviour for API < Oreo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val keyEvent = IntentCompat.getParcelableExtra(mediaButtonEvent, Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)!!

            when (keyEvent.keyCode) {
                KeyEvent.KEYCODE_HEADSETHOOK, // used to hang up calls
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                KeyEvent.KEYCODE_MEDIA_PLAY,
                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    // Only act on the DOWN event (which includes taps on Notification buttons) of button taps
                    if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                        if (!isWaitingForDoubleTap) {
                            isWaitingForDoubleTap = true

                            doubleTapTimer = DoubleTapTimer().start()
                        } else {
                            isWaitingForDoubleTap = false
                            doubleTapTimer!!.cancel()
                            onSkipToNext()
                        }
                    }
                    // should be true whether the action is up or down
                    return true
                }
            }
        }

        // if we received a MediaButtonEvent that is not a play/pause/skip:
        // then we did not handle the event - should send to default MediaSessionCompat instead
        return false
    }

    private fun playPauseAfterTimeout() {
        isWaitingForDoubleTap = false
        when (playbackInfo?.playbackState) {
            PlaybackState.PLAYING -> onPause()
            PlaybackState.PAUSED -> onPlay()
            PlaybackState.NONE -> Unit
            null -> Unit
        }
    }

    override fun onPlayFromUri(uri: Uri, extras: Bundle) {
        @Suppress("DEPRECATION") val newAudioPlayable = extras.getSerializable(Constants.Keys.KEY_AUDIO_PLAYABLE) as AudioPlayable
        val isAutoPlay = extras.getBoolean(Constants.Keys.KEY_IS_AUTO_PLAY, false)
        val maxDurationDiscrepancy = extras.getInt(Constants.Keys.KEY_MAX_DURATION_DISCREPANCY,
            ArmadilloConfiguration.MAX_DISCREPANCY_DEFAULT)

        if (newAudioPlayable == stateProvider.currentState.playbackInfo?.audioPlayable && isPlaying) {
            Log.v(TAG, "onPlayFromUri: already playing audioPlayable: ${newAudioPlayable.id} - Skipping setup")
            return
        }

        if (isPlaying) {
            onStop()
        }

        @Suppress("UNCHECKED_CAST", "DEPRECATION") val initialOffset = extras.getSerializable(Constants.Keys.KEY_INITIAL_OFFSET) as Milliseconds
        playbackEngine = playbackEngineFactory.createPlaybackEngine(newAudioPlayable)
        playbackEngine?.beginPlayback(isAutoPlay, maxDurationDiscrepancy, initialOffset)
        playbackEngine?.seekTo(initialOffset)

        isPlaying = true

        Log.v(TAG, "onPlayFromUri: ${newAudioPlayable.id}")
    }

    override fun onPlay() {
        playbackEngine?.play()
        Log.v(TAG, "onPlay")
    }

    override fun onPlayFromSearch(query: String?, extras: Bundle?) {
        mediaBrowser.searchForMedia(query, true)
        Log.v(TAG, "onPlayFromSearch")
    }

    override fun onPause() {
        playbackEngine?.pause()
        Log.v(TAG, "onPause")
    }

    // Note: the behaviour of skip-to-next and fast-forward buttons is swapped
    override fun onSkipToNext() {
        playbackEngine?.skipForward(playbackInfo?.skipDistance ?: Constants.AUDIO_SKIP_DURATION)
        Log.v(TAG, "onSkipToNext")
    }

    override fun onSkipToPrevious() {
        playbackEngine?.skipBackward(playbackInfo?.skipDistance ?: Constants.AUDIO_SKIP_DURATION)
        Log.v(TAG, "onSkipToPrevious")
    }

    override fun onFastForward() {
        playbackEngine?.nextChapter()
        Log.v(TAG, "onFastForward")
    }

    override fun onRewind() {
        playbackEngine?.previousChapter(Constants.MAX_POSITION_FOR_SEEK_TO_PREVIOUS)
        Log.v(TAG, "onRewind")
    }

    override fun onSeekTo(posInMilis: Long) {
        // if the shift has been added, then it must have originated from app UI, because we added it
        val absolutePosition = if (posInMilis >= AUDIO_POSITION_SHIFT_IN_MS) {
            posInMilis - AUDIO_POSITION_SHIFT_IN_MS // undo
        } else {
            // possibly from notification which sends position relative to chapter start
            // so, add chapter start time to make it absolute position
            val chapterStartTime = playbackInfo?.progress?.currentChapterIndex?.let { chapterIndex ->
                playbackInfo?.audioPlayable?.chapters?.getOrNull(chapterIndex)?.startTime?.longValue
            } ?: 0
            posInMilis + chapterStartTime
        }

        playbackEngine?.seekTo(absolutePosition.milliseconds)
        Log.v(TAG, "onSeekTo: received $posInMilis, absolute $absolutePosition")
    }

    override fun onCustomAction(action: String?, extras: Bundle?) {
        val customAction = CustomAction.build(action, extras)
        stateModifier.dispatch(CustomMediaSessionAction(action ?: ""))
        when (customAction) {
            CustomAction.UpdateProgress -> dispatchProgress()
            is CustomAction.SetPlaybackSpeed -> {
                playbackEngine?.setPlaybackSpeed(customAction.playbackSpeed)
                Log.v(TAG, "SetPlaybackSpeed")
            }
            is CustomAction.UpdatePlaybackMetadata -> {
                playbackEngine?.updateMetadata(customAction.title, customAction.chapters)
            }
            is CustomAction.UpdateMediaRequest -> {
                playbackEngine?.updateMediaRequest(customAction.mediaRequest)
            }
            null -> Log.e(TAG, "Custom action is null from $action with $extras")
        }
    }

    override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {
        onPlayFromMediaId(mediaId, extras)
    }

    override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
        if (mediaId != null) {
            val result = mediaBrowser.prepareMedia(mediaId, false)

            // The media ID has changed, something has gone wrong, this is probably an error item
            if (mediaId != result?.mediaId) {
                onStop()
                onMediaSessionEventListener?.onPlayMediaFailed(result)
            }
        }
    }

    override fun onStop() {
        isPlaying = false
        playbackEngine?.deinit()
        playbackEngine = null
        Log.v(TAG, "onStop")
    }

    private fun dispatchProgress() {
        stateProvider.currentState.playbackInfo?.audioPlayable?.let {
            playbackEngine?.let { engine ->
                engine.updateProgress()
                stateModifier.dispatch(UpdateProgressAction(true, engine.currentChapterIndex))
            }
        }
    }

    internal interface OnMediaSessionEventListener {
        fun onPlayMediaFailed(media: MediaBrowserCompat.MediaItem?)
    }
}

