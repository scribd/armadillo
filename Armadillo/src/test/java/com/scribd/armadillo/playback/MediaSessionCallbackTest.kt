package com.scribd.armadillo.playback

import android.content.Intent
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Build.VERSION_CODES.O_MR1
import android.os.Bundle
import android.view.KeyEvent
import com.scribd.armadillo.ArmadilloConfiguration
import com.scribd.armadillo.Constants
import com.scribd.armadillo.DaggerComponentRule
import com.scribd.armadillo.StateStore
import com.scribd.armadillo.analytics.PlaybackActionListenerHolder
import com.scribd.armadillo.extensions.CustomAction
import com.scribd.armadillo.extensions.toUri
import com.scribd.armadillo.models.ArmadilloState
import com.scribd.armadillo.models.AudioPlayable
import com.scribd.armadillo.models.Chapter
import com.scribd.armadillo.models.PlaybackInfo
import com.scribd.armadillo.models.PlaybackState
import com.scribd.armadillo.time.milliseconds
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class MediaSessionCallbackTest {
    private companion object {
        const val URL = "www.audiobooks.com"
        const val DOC_ID_ONE = 1234
        const val DOC_ID_TWO = 9999
        const val CURRENT_CHAPTER_INDEX = 0
    }

    @Rule
    @JvmField
    val daggerComponentRule = DaggerComponentRule()

    private lateinit var mediaSessionCallback: MediaSessionCallback
    private lateinit var playbackEngine: AudioPlaybackEngine
    private lateinit var playbackEngineFactory: PlaybackEngineFactory
    private lateinit var playbackStateListener: PlaybackService.PlaybackStateListener

    private lateinit var stateProvider: StateStore.Provider
    private lateinit var stateModifier: StateStore.Modifier
    private lateinit var armadilloState: ArmadilloState
    private lateinit var playbackInfo: PlaybackInfo
    private lateinit var playbackState: PlaybackState
    private lateinit var request: AudioPlayable.MediaRequest

    private lateinit var audiobookOne: AudioPlayable
    private lateinit var bundleOne: Bundle
    private lateinit var audiobookTwo: AudioPlayable
    private lateinit var bundleTwo: Bundle

    @Before
    fun setUp() {
        playbackEngineFactory = mock()
        playbackStateListener = mock()
        PlaybackActionListenerHolder.stateListener = playbackStateListener
        playbackEngine = mock()
        whenever(playbackEngine.pause()).then {
            whenever(playbackInfo.playbackState).thenReturn(PlaybackState.PAUSED)
            it
        }
        whenever(playbackEngine.play()).then {
            whenever(playbackInfo.playbackState).thenReturn(PlaybackState.PLAYING)
            it
        }

        stateProvider = mock()
        stateModifier = mock()
        armadilloState = mock()
        playbackState = PlaybackState.PLAYING
        playbackInfo = mock()
        request = mock()

        audiobookOne = mock()
        audiobookTwo = mock()
        bundleOne = Bundle()
        bundleTwo = Bundle()
        bundleOne.addAudiobook(audiobookOne)
        bundleTwo.addAudiobook(audiobookTwo)

        // Set up state
        whenever(stateProvider.currentState).thenReturn(armadilloState)
        whenever(armadilloState.playbackInfo).thenReturn(playbackInfo)
        whenever(playbackInfo.playbackState).thenReturn(playbackState)
        whenever(request.url).thenReturn(URL)

        // Set up audiobooks
        whenever(playbackEngine.currentChapterIndex).thenReturn(CURRENT_CHAPTER_INDEX)
        whenever(audiobookOne.request).thenReturn(request)
        whenever(audiobookOne.id).thenReturn(DOC_ID_ONE)
        whenever(audiobookTwo.id).thenReturn(DOC_ID_TWO)

        // Set up engine
        whenever(playbackEngineFactory.createPlaybackEngine(audiobookOne)).thenReturn(playbackEngine)
        whenever(playbackEngineFactory.createPlaybackEngine(audiobookTwo)).thenReturn(playbackEngine)

        // Set up MediaSessionCallback
        mediaSessionCallback = MediaSessionCallback(null)
        mediaSessionCallback.playbackEngineFactory = playbackEngineFactory
        mediaSessionCallback.playbackEngine = playbackEngine
        mediaSessionCallback.stateProvider = stateProvider
        mediaSessionCallback.stateModifier = stateModifier
    }

    @Test
    fun onPlay_playbackEngineShouldPlay() {
        mediaSessionCallback.onPlay()

        verify(mediaSessionCallback.playbackEngine!!).play()
    }

    @Test
    fun onPlayFromUri_isCurrentlyPlayingSameAudiobook_shouldNotStop() {
        mediaSessionCallback.isPlaying = true
        whenever(playbackInfo.audioPlayable).thenReturn(audiobookOne)
        mediaSessionCallback.onPlayFromUri(URL.toUri(), bundleOne)
        verify(mediaSessionCallback, Mockito.times(0)).playbackEngine!!
    }

    @Test
    fun onPlayFromUri_isCurrentlyDifferentPlayingAudiobook_shouldStop() {
        mediaSessionCallback.isPlaying = true
        whenever(playbackInfo.audioPlayable).thenReturn(audiobookOne)
        mediaSessionCallback.onPlayFromUri(URL.toUri(), bundleTwo)
        verify(mediaSessionCallback.playbackEngine!!, times(1)).deinit()
        verify(mediaSessionCallback.playbackEngine!!, times(1))
            .beginPlayback(ArmadilloConfiguration())
    }

    @Test
    fun onPlayFromUri_notCurrentlyPlaying_shouldStart() {
        mediaSessionCallback.isPlaying = false
        whenever(playbackInfo.audioPlayable).thenReturn(audiobookTwo)
        mediaSessionCallback.onPlayFromUri(URL.toUri(), bundleOne)
        verify(mediaSessionCallback.playbackEngine!!, times(1))
            .beginPlayback(ArmadilloConfiguration())
    }

    @Test
    fun onPause_playbackEngineShouldPause() {
        mediaSessionCallback.onPause()

        verify(mediaSessionCallback.playbackEngine!!).pause()
    }

    @Test
    fun onFastForward_playbackEngineShouldSkipChapter() {
        mediaSessionCallback.onFastForward()

        verify(mediaSessionCallback.playbackEngine!!).nextChapter()
    }

    @Test
    fun onRewind_playbackEngineShouldSkipChapter() {
        mediaSessionCallback.onRewind()

        verify(mediaSessionCallback.playbackEngine!!).previousChapter(Constants.MAX_POSITION_FOR_SEEK_TO_PREVIOUS)
    }

    @Test
    fun onSkipNext_playbackEngineShouldSkipForward() {
        mediaSessionCallback.onSkipToNext()

        verify(mediaSessionCallback.playbackEngine!!).skipForward(Constants.AUDIO_SKIP_DURATION)
    }

    @Test
    fun onSkipBack_playbackEngineShouldSkipBackward() {
        mediaSessionCallback.onSkipToPrevious()

        verify(mediaSessionCallback.playbackEngine!!).skipBackward(Constants.AUDIO_SKIP_DURATION)
    }

    @Test
    fun onStop_playbackEngineDeinit_engineIsNull() {
        mediaSessionCallback.onStop()

        assertThat(mediaSessionCallback.playbackEngine).isNull()
    }

    @Test
    fun onUpdateProgressAction_playbackEngineUpdates() {
        whenever(playbackInfo.audioPlayable).thenReturn(audiobookOne)
        mediaSessionCallback.onCustomAction(Constants.Actions.UPDATE_PROGRESS, Bundle())

        verify(playbackEngine, times(1)).updateProgress()
    }

    @Test
    fun onPlaybackSpeedAction_updatesEnginePlaybackSpeed() {
        val playbackSpeed = 1.5f
        mediaSessionCallback.onCustomAction(Constants.Actions.SET_PLAYBACK_SPEED, Bundle().apply {
            putFloat(Constants.Actions.Extras.PLAYBACK_SPEED, playbackSpeed)
        })

        verify(playbackEngine).setPlaybackSpeed(playbackSpeed)
    }

    @Test
    fun onIsInForegroundAction_updatesEngineOffloading() {
        val isInForeground = true
        mediaSessionCallback.onCustomAction(Constants.Actions.SET_IS_IN_FOREGROUND, Bundle().apply {
            putBoolean(Constants.Actions.Extras.IS_IN_FOREGROUND, isInForeground)
        })

        verify(playbackEngine).offloadAudio = !isInForeground
    }

    @Test
    fun onMetadataUpdateAction_updatesMetadata() {
        val newTitle = "New Title"
        val newChapters = listOf(
            Chapter("Chapter 1", 0, 0, 0.milliseconds, 100.milliseconds),
            Chapter("Chapter 2", 0, 1, 100.milliseconds, 500.milliseconds)
        )
        mediaSessionCallback.onCustomAction(Constants.Actions.UPDATE_METADATA,
            CustomAction.UpdatePlaybackMetadata(newTitle, newChapters).toBundle())

        verify(playbackEngine).updateMetadata(newTitle, newChapters)
    }

    @Test
    fun onSeekTo_playbackEngineSeeks() {
        mediaSessionCallback.onSeekTo(5000)
        verify(mediaSessionCallback.playbackEngine!!).seekTo(5000.milliseconds)
    }

    @Config(sdk = [O_MR1])
    @Test
    fun onDoubleTapPlayPause_shouldSkip() {
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
        val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)

        mediaSessionCallback.onMediaButtonEvent(intent)
        mediaSessionCallback.onMediaButtonEvent(intent)

        verify(mediaSessionCallback.playbackEngine!!).skipForward(Constants.AUDIO_SKIP_DURATION)
    }

    @Config(sdk = [O_MR1])
    @Test
    fun onTap_timerShouldStartWaiting() {
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
        val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)

        mediaSessionCallback.onMediaButtonEvent(intent)
        val doubleTapTimerShadow = Shadows.shadowOf(mediaSessionCallback.doubleTapTimer)

        assertThat(doubleTapTimerShadow.hasStarted()).isTrue()
        assertThat(doubleTapTimerShadow.millisInFuture).isEqualTo(mediaSessionCallback.doubleTapTimeOut)
        assertThat(mediaSessionCallback.isWaitingForDoubleTap).isTrue()
    }

    @Config(sdk = [O_MR1])
    @Test
    fun onDoubleTapTimeout_shouldResetBeforeContinue() {
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
        val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)

        assertThat(mediaSessionCallback.onMediaButtonEvent(intent)).isTrue()
        mediaSessionCallback.doubleTapTimer!!.onFinish()

        assertThat(mediaSessionCallback.isWaitingForDoubleTap).isFalse()
    }

    @Config(sdk = [O_MR1])
    @Test
    fun onOtherKeyPress_shouldNotWaitForDoubleTap() {
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
        val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT)
        intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)

        mediaSessionCallback.onMediaButtonEvent(intent)
        assertThat(mediaSessionCallback.isWaitingForDoubleTap).isFalse()
    }

    @Config(sdk = [LOLLIPOP])
    @Test
    fun apiUnderO_shouldGoToDefaultMediaButtonEvent() {
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
        val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)

        assertThat(mediaSessionCallback.onMediaButtonEvent(intent)).isFalse()
    }

    @Config(sdk = [O_MR1])
    @Test
    fun tapPlay_waitForTimeout_tapPlay_shouldPlayPauseTwice() {
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
        val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)

        mediaSessionCallback.onMediaButtonEvent(intent)
        mediaSessionCallback.doubleTapTimer!!.onFinish()

        verify(mediaSessionCallback.playbackEngine!!).pause()

        mediaSessionCallback.onMediaButtonEvent(intent)
        mediaSessionCallback.doubleTapTimer!!.onFinish()

        verify(mediaSessionCallback.playbackEngine!!).play()
    }

    private fun Bundle.addAudiobook(audiobook: AudioPlayable) {
        putSerializable(Constants.Keys.KEY_AUDIO_PLAYABLE, audiobook)
        putSerializable(Constants.Keys.KEY_ARMADILLO_CONFIG, ArmadilloConfiguration())
    }
}
