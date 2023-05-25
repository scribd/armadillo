package com.scribd.armadillo.playback

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.upstream.cache.Cache
import com.scribd.armadillo.ArmadilloPlayerChoreographer
import com.scribd.armadillo.Constants
import com.scribd.armadillo.Milliseconds
import com.scribd.armadillo.StateStore
import com.scribd.armadillo.actions.FastForwardAction
import com.scribd.armadillo.actions.NewAudioPlayableAction
import com.scribd.armadillo.actions.PlaybackEngineReady
import com.scribd.armadillo.actions.PlaybackProgressAction
import com.scribd.armadillo.actions.PlaybackSpeedAction
import com.scribd.armadillo.actions.PlayerStateAction
import com.scribd.armadillo.actions.RewindAction
import com.scribd.armadillo.actions.SeekAction
import com.scribd.armadillo.actions.SkipNextAction
import com.scribd.armadillo.actions.SkipPrevAction
import com.scribd.armadillo.di.Injector
import com.scribd.armadillo.encryption.ExoplayerEncryption
import com.scribd.armadillo.error.MissingDataException
import com.scribd.armadillo.models.AudioPlayable
import com.scribd.armadillo.models.Chapter
import com.scribd.armadillo.models.PlaybackState
import com.scribd.armadillo.playback.mediasource.MediaSourceRetriever
import com.scribd.armadillo.time.milliseconds
import javax.inject.Inject
import javax.inject.Named

/**
 * The interface [ArmadilloPlayerChoreographer] uses to communicate with a playback engine.
 */
internal interface AudioPlaybackEngine {
    /**
     * In general, we should not be querying the state of the [AudioPlaybackEngine]
     * but in one case, [PlaybackNotificationBuilder.build] requires [Chapter] info
     */
    val currentChapterIndex: Int

    /**
     * Can the engine offload audio processing. Note that this may have no effect if the engine is not able to offload.
     */
    var offloadAudio: Boolean

    fun beginPlayback(isAutoPlay: Boolean, maxDurationDiscrepancy: Int, initialOffset: Milliseconds = 0.milliseconds)

    /**
     * Updates the request for the currently playing media. This could be to change the request headers.
     * If the URL is not for the currently playing content, this is ignored
     */
    fun updateMediaRequest(mediaRequest: AudioPlayable.MediaRequest)
    fun deinit()
    fun play()
    fun pause()
    fun nextChapter()
    fun previousChapter(maxPositionForSeekToPrevious: Milliseconds)
    fun skipForward(time: Milliseconds)
    fun skipBackward(time: Milliseconds)
    fun seekTo(offset: Milliseconds)
    fun setPlaybackSpeed(playbackSpeed: Float)
    fun updateProgress()

    fun updateMetadata(title: String, chapters: List<Chapter>)
}

/**
 * A wrapper for the [ExoPlayer] audio engine
 *
 * @property audioPlayable
 * @property downloadCache
 */
internal class ExoplayerPlaybackEngine(private var audioPlayable: AudioPlayable) : AudioPlaybackEngine {

    init {
        Injector.mainComponent.inject(this)
    }

    @field:[Inject Named(Constants.DI.DOWNLOAD_CACHE)]
    internal lateinit var downloadCache: Cache

    @Inject
    internal lateinit var context: Context

    @Inject
    internal lateinit var stateModifier: StateStore.Modifier

    @Inject
    internal lateinit var audioAttributes: ArmadilloAudioAttributes

    @Inject
    internal lateinit var exoplayerEncryption: ExoplayerEncryption

    @Inject
    internal lateinit var mediaSourceRetriever: MediaSourceRetriever

    @VisibleForTesting
    internal lateinit var exoPlayer: ExoPlayer

    private val playerEventListener = PlayerEventListener()

    override val currentChapterIndex: Int
        get() = audioPlayable.chapters.indexOf(currentChapter)

    private val currentChapter: Chapter
        get() = audioPlayable.getChapterAtOffset(exoPlayer.currentPositionInDuration())
            ?: throw MissingDataException("currentChapter null")

    override var offloadAudio: Boolean = false
        set(value) {
            // It is always valid to disable offloading. It is invalid to enable offloading if the playback speed is modified
            if (exoPlayer.playbackParameters.speed == 1f || !value) {
                field = value
                exoPlayer.experimentalSetOffloadSchedulingEnabled(value)
            }
        }

    override fun beginPlayback(isAutoPlay: Boolean, maxDurationDiscrepancy: Int, initialOffset: Milliseconds) {
        stateModifier.dispatch(NewAudioPlayableAction(audioPlayable, maxDurationDiscrepancy, initialOffset))

        exoPlayer = createExoplayerInstance(context, audioAttributes.exoPlayerAttrs)

        val mediaSource = mediaSourceRetriever.generateMediaSource(audioPlayable.request, context)
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()

        exoPlayer.addListener(playerEventListener)

        exoPlayer.playWhenReady = isAutoPlay

        stateModifier.dispatch(PlaybackEngineReady(true))
        stateModifier.dispatch(PlayerStateAction(PlaybackState.PAUSED))
    }

    override fun updateMediaRequest(mediaRequest: AudioPlayable.MediaRequest) {
        if (mediaRequest.url == audioPlayable.request.url) {
            audioPlayable = audioPlayable.copy(
                request = mediaRequest
            )
            mediaSourceRetriever.updateMediaSourceHeaders(mediaRequest)
        }
    }

    override fun deinit() {
        exoPlayer.release()
        exoPlayer.removeListener(playerEventListener)

        stateModifier.dispatch(PlayerStateAction(PlaybackState.NONE))
        stateModifier.dispatch(PlaybackEngineReady(false))
    }

    override fun play() {
        exoPlayer.playWhenReady = true
    }

    override fun pause() {
        exoPlayer.playWhenReady = false
    }

    override fun nextChapter() {
        val nextChapter: Chapter = audioPlayable.getNextChapter(currentChapter) ?: return

        val trueOffset = adjustSeekOffset(nextChapter.startTime)
        stateModifier.dispatch(SkipNextAction(trueOffset))
        seekToExo(trueOffset)
    }

    override fun previousChapter(maxPositionForSeekToPrevious: Milliseconds) {
        val currentPosition = exoPlayer.currentPositionInDuration()

        val timeListenedToInCurrentChapter = currentPosition - currentChapter.startTime

        val shouldSkipToPreviousTrack = timeListenedToInCurrentChapter <= maxPositionForSeekToPrevious

        val previousChapter = audioPlayable.getPreviousChapter(currentChapter)
        val trueOffset = if (shouldSkipToPreviousTrack && previousChapter != null) {
            adjustSeekOffset(previousChapter.startTime)
        } else {
            adjustSeekOffset(currentChapter.startTime)
        }
        stateModifier.dispatch(SkipPrevAction(trueOffset))
        seekToExo(trueOffset)
    }

    override fun skipForward(time: Milliseconds) {
        val desiredListeningPosition = exoPlayer.currentPositionInDuration() + time
        val nextChapter = audioPlayable.getNextChapter(currentChapter)
        val trueOffset = when {
            // Beyond chapter -> limit to next next chapter
            desiredListeningPosition > currentChapter.endTime && nextChapter != null -> adjustSeekOffset(nextChapter.startTime)
            // No limits
            else -> adjustSeekOffset(desiredListeningPosition)
        }
        stateModifier.dispatch(FastForwardAction(trueOffset))
        seekToExo(trueOffset)
    }

    override fun skipBackward(time: Milliseconds) {
        val desiredListeningPosition = exoPlayer.currentPositionInDuration() - time
        val trueOffset = adjustSeekOffset(desiredListeningPosition)
        stateModifier.dispatch(RewindAction(trueOffset))
        seekToExo(trueOffset)
    }

    override fun seekTo(offset: Milliseconds) {
        val trueOffset = adjustSeekOffset(offset)
        stateModifier.dispatch(SeekAction(true, trueOffset))
        seekToExo(trueOffset)
    }

    override fun setPlaybackSpeed(playbackSpeed: Float) {
        if (playbackSpeed != 1f) {
            // Audio processing effects are disabled in offloading mode, including playback speed. Cannot offload if speed != 1
            exoPlayer.experimentalSetOffloadSchedulingEnabled(false)
        }
        exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
        stateModifier.dispatch(PlaybackSpeedAction(playbackSpeed))
    }

    override fun updateProgress() {
        val playbackState = exoPlayer.playbackState
        val shouldRecordPosition = playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED
        if (exoPlayer.hasProgressAvailable() && shouldRecordPosition) {
            stateModifier.dispatch(PlaybackProgressAction(exoPlayer.currentPositionInDuration(), exoPlayer.playerDuration()))
        }
    }

    override fun updateMetadata(title: String, chapters: List<Chapter>) {
        audioPlayable = audioPlayable.copy(
            title = title,
            chapters = chapters
        )
    }

    private fun adjustSeekOffset(offset: Milliseconds): Milliseconds = when {
        // Beyond end -> limit to end
        (offset > audioPlayable.duration) -> audioPlayable.duration
        (offset < 0.milliseconds) -> 0.milliseconds
        else -> offset
    }

    private fun seekToExo(position: Milliseconds) {
        exoPlayer.seekTo(exoPlayer.currentMediaItemIndex, position.longValue)
        stateModifier.dispatch(PlaybackProgressAction(position, exoPlayer.playerDuration()))
    }
}