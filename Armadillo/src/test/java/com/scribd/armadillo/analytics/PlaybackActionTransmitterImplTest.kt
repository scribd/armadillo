package com.scribd.armadillo.analytics

import com.scribd.armadillo.DaggerComponentRule
import com.scribd.armadillo.StateStore
import com.scribd.armadillo.models.ArmadilloState
import com.scribd.armadillo.models.AudioPlayable
import com.scribd.armadillo.models.DrmState
import com.scribd.armadillo.models.InternalState
import com.scribd.armadillo.models.MediaControlState
import com.scribd.armadillo.models.PlaybackInfo
import com.scribd.armadillo.models.PlaybackProgress
import com.scribd.armadillo.models.PlaybackState
import com.scribd.armadillo.playback.PlaybackService
import com.scribd.armadillo.time.milliseconds
import com.scribd.armadillo.time.seconds
import io.reactivex.subjects.BehaviorSubject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class PlaybackActionTransmitterImplTest {

    @get:Rule
    val daggerComponentRule = DaggerComponentRule()

    private lateinit var transmitter: PlaybackActionTransmitterImpl

    private lateinit var listener: PlaybackActionListener
    private lateinit var playbackStateListener: PlaybackService.PlaybackStateListener
    private lateinit var provider: StateStore.Provider
    private lateinit var states: BehaviorSubject<ArmadilloState>

    private lateinit var state0: ArmadilloState
    private lateinit var audiobook0: AudioPlayable
    private lateinit var progress0: PlaybackProgress
    private lateinit var playback0: PlaybackInfo
    private lateinit var controls0: MediaControlState

    private lateinit var state1: ArmadilloState
    private lateinit var audiobook1: AudioPlayable
    private lateinit var progress1: PlaybackProgress
    private lateinit var playback1: PlaybackInfo
    private lateinit var controls1: MediaControlState

    private lateinit var state2: ArmadilloState
    private lateinit var audiobook2: AudioPlayable
    private lateinit var progress2: PlaybackProgress
    private lateinit var playback2: PlaybackInfo
    private lateinit var controls2: MediaControlState

    private val pollInterval = 100.milliseconds

    @Before
    fun setUp() {
        listener = mock()
        provider = mock()
        playbackStateListener = mock()
        states = BehaviorSubject.create<ArmadilloState>()

        audiobook0 = mock()
        progress0 = mock()
        controls0 = mock()
        playback0 = mock()
        state0 = ArmadilloState(playback0, emptyList(), DrmState.NoDRM, InternalState(), null)
        whenever(playback0.audioPlayable).thenReturn(audiobook0)
        whenever(playback0.controlState).thenReturn(controls0)
        whenever(playback0.progress).thenReturn(progress0)

        audiobook1 = mock()
        progress1 = mock()
        controls1 = mock()
        playback1 = mock()
        state1 = ArmadilloState(playback1, emptyList(), DrmState.NoDRM, InternalState(), null)
        whenever(playback1.audioPlayable).thenReturn(audiobook1)
        whenever(playback1.controlState).thenReturn(controls1)
        whenever(playback1.progress).thenReturn(progress1)

        audiobook2 = mock()
        progress2 = mock()
        controls2 = mock()
        playback2 = mock()
        state2 = ArmadilloState(playback2, emptyList(), DrmState.NoDRM, InternalState(), null)
        whenever(playback2.audioPlayable).thenReturn(audiobook2)
        whenever(playback2.controlState).thenReturn(controls2)
        whenever(playback2.progress).thenReturn(progress2)

        whenever(provider.stateSubject).thenReturn(states)
        whenever(provider.currentState).thenReturn(state2)
        PlaybackActionListenerHolder.stateListener = playbackStateListener
        PlaybackActionListenerHolder.actionlisteners.clear()
        PlaybackActionListenerHolder.actionlisteners.add(listener)
        transmitter = PlaybackActionTransmitterImpl(provider)
        transmitter.begin(pollInterval)
    }

    @Test
    fun loadNewAudiobook_noPriorLoad() {
        whenever(controls1.isStartingNewAudioPlayable).thenReturn(true)
        states.onNext(state1)
        verify(listener, never()).onError(any(), any())
        verify(listener).onNewAudiobook(any())
        verify(playbackStateListener).onNewAudiobook(any())
    }

    @Test
    fun loadNewAudiobook_changeAudiobooks() {
        whenever(controls1.isStartingNewAudioPlayable).thenReturn(false)
        whenever(controls2.isStartingNewAudioPlayable).thenReturn(true)
        states.onNext(state1)
        states.onNext(state2)
        verify(listener, never()).onError(any(), any())
        verify(listener).onNewAudiobook(any())
        verify(playbackStateListener).onNewAudiobook(any())
    }

    @Test
    fun loadBuffer_beginLoad() {
        whenever(playback1.isLoading).thenReturn(false)
        whenever(playback2.isLoading).thenReturn(true)
        states.onNext(state1)
        states.onNext(state2)
        verify(listener, never()).onError(any(), any())
        verify(listener).onLoadingStart(any())
    }

    @Test
    fun loadBuffer_finishLoad() {
        whenever(playback1.isLoading).thenReturn(true)
        whenever(playback2.isLoading).thenReturn(false)
        states.onNext(state1)
        states.onNext(state2)
        verify(listener, never()).onError(any(), any())
        verify(listener).onLoadingEnd(any())
    }

    @Test
    fun play_noneToPlaying() {
        whenever(playback1.playbackState).thenReturn(PlaybackState.NONE)
        whenever(playback2.playbackState).thenReturn(PlaybackState.PLAYING)
        states.onNext(state1)
        states.onNext(state2)
        verify(listener, never()).onError(any(), any())
        verify(listener).onPlay(any())
    }

    @Test
    fun play_pausedToPlaying() {
        whenever(playback1.playbackState).thenReturn(PlaybackState.PAUSED)
        whenever(playback2.playbackState).thenReturn(PlaybackState.PLAYING)
        states.onNext(state1)
        states.onNext(state2)
        verify(listener, never()).onError(any(), any())
        verify(listener).onPlay(any())
    }

    @Test
    fun pause_noneToPaused() {
        whenever(playback1.playbackState).thenReturn(PlaybackState.NONE)
        whenever(playback2.playbackState).thenReturn(PlaybackState.PAUSED)
        states.onNext(state1)
        states.onNext(state2)
        verify(listener, never()).onError(any(), any())
        verify(listener).onPause(any())
    }

    @Test
    fun pause_playToPaused() {
        whenever(playback1.playbackState).thenReturn(PlaybackState.PLAYING)
        whenever(playback2.playbackState).thenReturn(PlaybackState.PAUSED)
        states.onNext(state1)
        states.onNext(state2)
        verify(listener, never()).onError(any(), any())
        verify(listener).onPause(any())
    }

    @Test
    fun stop() {
        whenever(controls1.isStopping).thenReturn(false)
        whenever(controls2.isStopping).thenReturn(true)
        states.onNext(state1)
        states.onNext(state2)
        verify(listener, never()).onError(any(), any())
        verify(listener).onStop(any())
        verify(playbackStateListener).onPlaybackEnd()
    }

    @Test
    fun contentEnded() {
        whenever(controls1.hasContentEnded).thenReturn(false)
        whenever(controls2.hasContentEnded).thenReturn(true)

        states.onNext(state1)
        states.onNext(state2)
        verify(listener).onContentEnded(state2)
    }

    @Test
    fun restart_callsNewAudiobook() {
        whenever(controls0.isStartingNewAudioPlayable).thenReturn(false)
        whenever(controls1.isStartingNewAudioPlayable).thenReturn(false)
        whenever(controls2.isStartingNewAudioPlayable).thenReturn(true)
        whenever(controls0.isStopping).thenReturn(false)
        whenever(controls1.isStopping).thenReturn(true)
        whenever(controls2.isStopping).thenReturn(false)
        states.onNext(state0)
        states.onNext(state1)
        states.onNext(state2)
        verify(listener, never()).onError(any(), any())
        verify(listener).onStop(any())
        verify(playbackStateListener).onPlaybackEnd()
        verify(listener).onNewAudiobook(any())
    }

    @Test
    fun seek_beginSeek() {
        whenever(controls1.isSeeking).thenReturn(false)
        whenever(controls2.isSeeking).thenReturn(true)
        states.onNext(state1)
        states.onNext(state2)
        verify(listener, never()).onError(any(), any())
        verify(listener).onDiscontinuity(any())
    }

    @Test
    fun seek_beginAndEndArbitrarySeek() {
        whenever(controls0.isSeeking).thenReturn(false)
        whenever(controls1.isSeeking).thenReturn(true)
        whenever(controls2.isSeeking).thenReturn(false)
        states.onNext(state0)
        states.onNext(state1)
        states.onNext(state2)
        verify(listener, never()).onError(any(), any())
        verify(listener).onSeek(any(), eq(state1), any())
    }

    @Test
    fun seek_fastForward() {
        whenever(controls0.isSeeking).thenReturn(false)
        whenever(controls1.isSeeking).thenReturn(true)
        whenever(controls2.isSeeking).thenReturn(false)
        whenever(controls1.isFastForwarding).thenReturn(true)
        states.onNext(state0)
        states.onNext(state1)
        states.onNext(state2)
        verify(listener, never()).onError(any(), any())
        verify(listener).onFastForward(any(), any())
    }

    @Test
    fun seek_rewind() {
        whenever(controls0.isSeeking).thenReturn(false)
        whenever(controls1.isSeeking).thenReturn(true)
        whenever(controls2.isSeeking).thenReturn(false)
        whenever(controls1.isRewinding).thenReturn(true)
        states.onNext(state0)
        states.onNext(state1)
        states.onNext(state2)
        verify(listener, never()).onError(any(), any())
        verify(listener).onRewind(any(), any())
    }

    @Test
    fun seek_nextChapter() {
        whenever(controls0.isSeeking).thenReturn(false)
        whenever(controls1.isSeeking).thenReturn(true)
        whenever(controls2.isSeeking).thenReturn(false)
        whenever(controls1.isNextChapter).thenReturn(true)
        states.onNext(state0)
        states.onNext(state1)
        states.onNext(state2)
        verify(listener, never()).onError(any(), any())
        verify(listener).onSkipToNext(any(), any())
    }

    @Test
    fun seek_prevChapter() {
        whenever(controls0.isSeeking).thenReturn(false)
        whenever(controls1.isSeeking).thenReturn(true)
        whenever(controls2.isSeeking).thenReturn(false)
        whenever(controls1.isPrevChapter).thenReturn(true)
        states.onNext(state0)
        states.onNext(state1)
        states.onNext(state2)
        verify(listener, never()).onError(any(), any())
        verify(listener).onSkipToPrevious(any(), any())
    }

    @Test
    fun speedChange() {
        whenever(playback1.playbackSpeed).thenReturn(2f)
        whenever(playback2.playbackSpeed).thenReturn(30f)
        states.onNext(state1)
        states.onNext(state2)
        verify(listener, never()).onError(any(), any())
        verify(listener).onSpeedChange(any(), any(), any())
    }

    @Test
    fun skipDistanceChange() {
        whenever(playback1.skipDistance).thenReturn(10.seconds.inMilliseconds)
        whenever(playback2.skipDistance).thenReturn(60.seconds.inMilliseconds)
        states.onNext(state1)
        states.onNext(state2)
        verify(listener, never()).onError(any(), any())
        verify(listener).onSkipDistanceChange(any(), any(), any())
    }

    @Test
    fun progressUpdate() {
        whenever(controls1.isPlaybackStateUpdating).thenReturn(false)
        whenever(controls2.isPlaybackStateUpdating).thenReturn(true)
        whenever(controls1.updatedChapterIndex).thenReturn(3)
        whenever(controls2.updatedChapterIndex).thenReturn(4)
        states.onNext(state1)
        states.onNext(state2)
        verify(playbackStateListener).onPlaybackStateChange(any(), eq(audiobook2), eq(4))
    }
}
