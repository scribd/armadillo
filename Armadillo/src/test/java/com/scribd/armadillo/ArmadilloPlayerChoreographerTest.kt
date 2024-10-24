package com.scribd.armadillo

import android.support.v4.media.session.MediaControllerCompat
import com.scribd.armadillo.actions.MediaRequestUpdateAction
import com.scribd.armadillo.actions.MetadataUpdateAction
import com.scribd.armadillo.models.ArmadilloState
import com.scribd.armadillo.models.AudioPlayable
import com.scribd.armadillo.models.Chapter
import com.scribd.armadillo.models.InternalState
import com.scribd.armadillo.models.PlaybackInfo
import com.scribd.armadillo.models.PlaybackState
import com.scribd.armadillo.playback.MediaSessionConnection
import com.scribd.armadillo.time.milliseconds
import io.reactivex.subjects.BehaviorSubject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Fixes issue in mockito. typealias is insufficient https://github.com/nhaarman/mockito-kotlin/issues/272#issuecomment-513971465
private interface Callback : (MediaControllerCompat.TransportControls) -> Unit

@Config(manifest = Config.NONE, sdk = [25])
@RunWith(RobolectricTestRunner::class)
class ArmadilloPlayerChoreographerTest {
    @Rule
    @JvmField
    val daggerComponentRule = DaggerComponentRule()

    private lateinit var choreographer: ArmadilloPlayerChoreographer

    @Before
    fun setUp() {
        choreographer = ArmadilloPlayerChoreographer()
        choreographer.stateModifier = mock()
    }

    @Test
    fun currentState_returnsCurrentState() {
        val state: ArmadilloState = mock()
        val stateProvider: StateStore.Provider = mock()

        val stateSubject = BehaviorSubject.create<ArmadilloState>()
        stateSubject.onNext(state)

        whenever(stateProvider.stateSubject).thenReturn(stateSubject)

        choreographer.stateProvider = stateProvider
        assertThat(choreographer.armadilloStateSubject.value).isEqualTo(state)
    }

    @Test
    @Ignore("Flaky - fails CI on randomly with threading timing, unrelated to actual changes on branch.")
    fun updateMediaRequest_transmitsUpdateAction() {
        // Set up playback state
        val transportControls = mock<MediaControllerCompat.TransportControls>()
        val playbackConnection = mock<MediaSessionConnection>()
        whenever(playbackConnection.transportControls).thenReturn(transportControls)
        choreographer.playbackConnection = playbackConnection

        val playbackState = PlaybackState.PAUSED
        val playbackInfo: PlaybackInfo = mock()
        whenever(playbackInfo.playbackState).thenReturn(playbackState)

        val stateProvider: StateStore.Provider = mock()
        val state = ArmadilloState(
            playbackInfo = playbackInfo,
            internalState = InternalState(true),
            downloadInfo = emptyList())
        whenever(stateProvider.currentState).thenReturn(state)

        val stateSubject = BehaviorSubject.create<ArmadilloState>()
        whenever(stateProvider.stateSubject).thenReturn(stateSubject)
        stateSubject.onNext(state)

        choreographer.stateProvider = stateProvider

        val newUrl = "https://www.github.com/scribd/armadillo"
        val newHeaders = mapOf(
            "header1" to "value1",
            "header2" to "value2"
        )
        val newRequest = AudioPlayable.MediaRequest.createHttpUri(newUrl, newHeaders)
        choreographer.updateMediaRequest(newRequest)
        ArmadilloPlayerChoreographer.handler.hasMessages(1) //magic looper processor

        verify(choreographer.stateModifier).dispatch(MediaRequestUpdateAction(newRequest))
        verify(transportControls).sendCustomAction(eq("update_media_request"), argWhere {
            it.getSerializable("media_request") == newRequest
        })
    }

    @Test
    fun updateMetadata_transmitsMetadataAction() {
        // Set up playback state
        val transportControls = mock<MediaControllerCompat.TransportControls>()
        val playbackConnection = mock<MediaSessionConnection>()
        whenever(playbackConnection.transportControls).thenReturn(transportControls)
        choreographer.playbackConnection = playbackConnection

        val playbackState = PlaybackState.PAUSED
        val playbackInfo: PlaybackInfo = mock()
        whenever(playbackInfo.playbackState).thenReturn(playbackState)

        val stateProvider: StateStore.Provider = mock()
        val state = ArmadilloState(
            playbackInfo = playbackInfo,
            internalState = InternalState(true),
            downloadInfo = emptyList())
        whenever(stateProvider.currentState).thenReturn(state)

        val stateSubject = BehaviorSubject.create<ArmadilloState>()
        whenever(stateProvider.stateSubject).thenReturn(stateSubject)
        stateSubject.onNext(state)

        choreographer.stateProvider = stateProvider

        val title = "New Title"
        val chapters = listOf(
            Chapter("New Chapter 0",
                1,
                2,
                123.milliseconds,
                200.milliseconds)
        )
        choreographer.updatePlaybackMetadata(title, chapters)

        verify(choreographer.stateModifier).dispatch(MetadataUpdateAction(title, chapters))
    }

    @Test
    fun doWhenPlaybackReady_playbackReady_invokes() {
        val transportControls = mock<MediaControllerCompat.TransportControls>()
        val playbackConnection = mock<MediaSessionConnection>()
        whenever(playbackConnection.transportControls).thenReturn(transportControls)
        choreographer.playbackConnection = playbackConnection

        val stateProvider: StateStore.Provider = mock()
        val state = ArmadilloState(
            internalState = InternalState(true),
            downloadInfo = emptyList())
        whenever(stateProvider.currentState).thenReturn(state)

        val stateSubject = BehaviorSubject.create<ArmadilloState>()
        whenever(stateProvider.stateSubject).thenReturn(stateSubject)
        stateSubject.onNext(state)

        choreographer.stateProvider = stateProvider

        val callback = mock<Callback>()

        choreographer.doWhenPlaybackReady(callback)
        verify(callback).invoke(eq(transportControls))
    }

    @Test
    fun doWhenPlaybackReady_playbackNotReady_invokesWhenReady() {
        val playbackConnection = mock<MediaSessionConnection>()
        val transportControls = mock<MediaControllerCompat.TransportControls>()
        whenever(playbackConnection.transportControls).thenReturn(null)
        choreographer.playbackConnection = playbackConnection

        val stateProvider: StateStore.Provider = mock()
        // Not Ready
        whenever(stateProvider.currentState).thenReturn(ArmadilloState(
            internalState = InternalState(false),
            downloadInfo = emptyList()))

        val stateSubject = BehaviorSubject.create<ArmadilloState>()
        whenever(stateProvider.stateSubject).thenReturn(stateSubject)

        choreographer.stateProvider = stateProvider

        val callback = mock<Callback>()

        choreographer.doWhenPlaybackReady(callback)
        verify(callback, times(0)).invoke(eq(transportControls))

        // Transport controls ready, but engine still not ready
        whenever(playbackConnection.transportControls).thenReturn(transportControls)
        stateSubject.onNext((ArmadilloState(
            internalState = InternalState(false),
            downloadInfo = emptyList())))
        verify(callback, times(0)).invoke(eq(transportControls))

        // Ready
        stateSubject.onNext((ArmadilloState(
            internalState = InternalState(true),
            downloadInfo = emptyList())))
        verify(callback, times(1)).invoke(eq(transportControls))
    }
}