package com.scribd.armadillo

import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import com.scribd.armadillo.actions.PlaybackProgressAction
import com.scribd.armadillo.models.AudioPlayable
import com.scribd.armadillo.models.PlaybackProgress
import com.scribd.armadillo.playback.ExoplayerPlaybackEngine
import com.scribd.armadillo.time.milliseconds
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ExoplayerPlaybackEngineTest {
    private lateinit var playbackEngine: ExoplayerPlaybackEngine
    private lateinit var exoplayer: ExoPlayer
    private lateinit var audiobook: AudioPlayable
    private lateinit var stateModifier: StateStore.Modifier
    private lateinit var timeline: Timeline

    private companion object {
        const val WINDOW_INDEX = 0
        val SKIP_DISTANCE = 30.milliseconds
    }

    @Rule
    @JvmField
    val daggerComponentRule = DaggerComponentRule()

    @Before
    fun setUp() {
        audiobook = MockModels.audiobook()
        stateModifier = mock()

        timeline = mock()

        playbackEngine = ExoplayerPlaybackEngine(audiobook)
        playbackEngine.stateModifier = stateModifier
        exoplayer = mock()
        playbackEngine.exoPlayer = exoplayer
    }

    @Test
    fun next_hasMoreTracks_shouldSkip() {
        val chap1 = MockModels.chapters()[0]
        whenever(exoplayer.currentPosition).thenReturn(chap1.startTime.longValue)

        playbackEngine.nextChapter()

        val chap2 = MockModels.chapters()[1]

        verify(exoplayer).seekTo(WINDOW_INDEX, chap2.startTime.longValue)
    }

    @Test
    fun previous_greaterThenSeekToPreviousThreshold_shouldSkipToBeginningOfCurrentTrack() {
        val chap2 = MockModels.chapters()[1]
        whenever(exoplayer.currentPosition).thenReturn((chap2.startTime + Constants.MAX_POSITION_FOR_SEEK_TO_PREVIOUS + 1
            .milliseconds)
            .longValue)

        playbackEngine.previousChapter(Constants.MAX_POSITION_FOR_SEEK_TO_PREVIOUS)

        verify(exoplayer).seekTo(WINDOW_INDEX, chap2.startTime.longValue)
    }

    @Test
    fun previous_lessThenSeekToPreviousThreshold_shouldSkipToPreviousTrack() {
        val chap2 = MockModels.chapters()[1]
        whenever(exoplayer.currentPosition).thenReturn((chap2.startTime + 1.milliseconds).longValue)

        playbackEngine.previousChapter(Constants.MAX_POSITION_FOR_SEEK_TO_PREVIOUS)

        val chap1 = MockModels.chapters()[0]
        verify(exoplayer).seekTo(WINDOW_INDEX, chap1.startTime.longValue)
    }

    @Test
    fun skipBackward_positionLessThanSkipDistance_skipsToBeginning() {
        whenever(exoplayer.currentPosition).thenReturn(SKIP_DISTANCE.longValue - 1)
        playbackEngine.skipBackward(SKIP_DISTANCE)

        verify(exoplayer).seekTo(WINDOW_INDEX, 0)
    }

    @Test
    fun skipBackward_positionBeyondSkipDistance_skipsBackDistance() {
        val offset = 1L
        whenever(exoplayer.currentPosition).thenReturn(SKIP_DISTANCE.longValue + offset)
        playbackEngine.skipBackward(SKIP_DISTANCE)

        verify(exoplayer).seekTo(WINDOW_INDEX, offset)
    }

    @Test
    fun skipForward_endOfChapter_skipsToEndOfChapter() {
        val firstChapter = MockModels.chapters()[0]
        val secondChapter = MockModels.chapters()[1]
        whenever(exoplayer.currentPosition).thenReturn(firstChapter.endTime.longValue - 1)
        playbackEngine.skipForward(SKIP_DISTANCE)

        verify(exoplayer).seekTo(WINDOW_INDEX, secondChapter.startTime.longValue)
    }

    @Test
    fun skipForward_endOfDocument_skipsToEnd() {
        val lastChapter = MockModels.chapters().last()
        whenever(exoplayer.currentPosition).thenReturn(lastChapter.endTime.longValue - 1)
        playbackEngine.skipForward(SKIP_DISTANCE)

        verify(exoplayer).seekTo(WINDOW_INDEX, lastChapter.endTime.longValue)
    }

    @Test
    fun skipForward_inChapter_skipsForward() {
        whenever(exoplayer.currentPosition).thenReturn(0)
        playbackEngine.skipForward(SKIP_DISTANCE)

        verify(exoplayer).seekTo(WINDOW_INDEX, SKIP_DISTANCE.longValue)
    }

    @Test
    fun seekTo_validPosition_seeksToPosition() {
        playbackEngine.seekTo(5000.milliseconds)
        verify(exoplayer).seekTo(WINDOW_INDEX, 5000)
    }

    @Test
    fun seekTo_beyondAudiobookDuration_seeksToEnd() {
        playbackEngine.seekTo(audiobook.duration + 1.milliseconds)
        verify(exoplayer).seekTo(WINDOW_INDEX, audiobook.duration.longValue)
    }

    @Test
    fun seekTo_positionLessThenZero_seeksToStart() {
        playbackEngine.seekTo((-1).milliseconds)
        verify(exoplayer).seekTo(WINDOW_INDEX, 0)
    }

    @Test
    fun setPlaybackSpeed_one_setsSpeed() {
        playbackEngine.setPlaybackSpeed(1f)

        verify(exoplayer).setPlaybackParameters(PlaybackParameters(1f))
        verify(exoplayer, never()).experimentalSetOffloadSchedulingEnabled(any())
    }

    @Test
    fun setPlaybackSpeed_notOne_setsSpeedAndDisablesOffloading() {
        playbackEngine.setPlaybackSpeed(2f)

        verify(exoplayer).setPlaybackParameters(PlaybackParameters(2f))
        verify(exoplayer).experimentalSetOffloadSchedulingEnabled(false)
    }

    @Test
    fun setOffloading_playbackSpeedOne_setsOffloading() {
        whenever(exoplayer.playbackParameters).thenReturn(PlaybackParameters(1f))
        playbackEngine.offloadAudio = true

        verify(exoplayer).experimentalSetOffloadSchedulingEnabled(true)
        assertThat(playbackEngine.offloadAudio).isTrue()
    }

    @Test
    fun setOffloading_playbackSpeedNotOne_doesNotSetOffloading() {
        whenever(exoplayer.playbackParameters).thenReturn(PlaybackParameters(2f))
        playbackEngine.offloadAudio = true

        verify(exoplayer, never()).experimentalSetOffloadSchedulingEnabled(any())
        assertThat(playbackEngine.offloadAudio).isFalse()
    }

    @Test
    fun setOffloading_playbackSpeedNotOneDisabling_setsOffloading() {
        whenever(exoplayer.playbackParameters).thenReturn(PlaybackParameters(2f))
        playbackEngine.offloadAudio = false

        verify(exoplayer).experimentalSetOffloadSchedulingEnabled(false)
        assertThat(playbackEngine.offloadAudio).isFalse()
    }

    @Test
    fun updateProgress_playerIdle_doesNothing() {
        whenever(exoplayer.playbackState).thenReturn(Player.STATE_IDLE)
        whenever(exoplayer.currentTimeline).thenReturn(mock())
        whenever(timeline.isEmpty).thenReturn(false)

        playbackEngine.updateProgress()

        verifyZeroInteractions(stateModifier)
    }

    @Test
    fun updateProgress_playerEnded_doesNothing() {
        whenever(exoplayer.playbackState).thenReturn(Player.STATE_ENDED)
        whenever(exoplayer.currentTimeline).thenReturn(timeline)
        whenever(timeline.isEmpty).thenReturn(false)

        playbackEngine.updateProgress()

        verifyZeroInteractions(stateModifier)
    }

    @Test
    fun updateProgress_noProgress_doesNothing() {
        whenever(exoplayer.playbackState).thenReturn(Player.STATE_READY)
        whenever(exoplayer.currentTimeline).thenReturn(timeline)
        whenever(timeline.isEmpty).thenReturn(true)

        playbackEngine.updateProgress()

        verifyZeroInteractions(stateModifier)
    }

    @Test
    fun updateProgress_hasProgressWithoutDuration_sendsProgressWithChapters() {
        val position = 405592L

        whenever(exoplayer.playbackState).thenReturn(Player.STATE_READY)
        whenever(exoplayer.currentTimeline).thenReturn(timeline)
        whenever(timeline.isEmpty).thenReturn(false)

        whenever(exoplayer.duration).thenReturn(C.TIME_UNSET)
        whenever(exoplayer.currentPosition).thenReturn(position)

        playbackEngine.updateProgress()

        verify(stateModifier).dispatch(PlaybackProgressAction(
            playerDuration = null,
            currentPosition = position.milliseconds
        ))
    }

    @Test
    fun updateProgress_hasProgressWithDuration_sendsProgressWithChaptersAndDuration() {
        val position = 405592L
        val duration = 4810349L

        whenever(exoplayer.playbackState).thenReturn(Player.STATE_READY)
        whenever(exoplayer.currentTimeline).thenReturn(timeline)
        whenever(timeline.isEmpty).thenReturn(false)

        whenever(exoplayer.duration).thenReturn(duration)
        whenever(exoplayer.currentPosition).thenReturn(position)

        playbackEngine.updateProgress()

        verify(stateModifier).dispatch(PlaybackProgressAction(
            currentPosition = position.milliseconds,
            playerDuration = duration.milliseconds
        ))
    }
}