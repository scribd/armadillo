package com.scribd.armadillo.analytics

import androidx.annotation.VisibleForTesting
import com.scribd.armadillo.Constants
import com.scribd.armadillo.Milliseconds
import com.scribd.armadillo.StateStore
import com.scribd.armadillo.di.Injector
import com.scribd.armadillo.error.ActionListenerException
import com.scribd.armadillo.models.ArmadilloState
import com.scribd.armadillo.models.AudioPlayable
import com.scribd.armadillo.models.InternalState
import com.scribd.armadillo.models.PlaybackState
import com.scribd.armadillo.time.milliseconds
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import java.util.concurrent.TimeUnit

interface PlaybackActionTransmitter {
    fun begin(pollingInterval: Milliseconds)
    fun destroy()
}

internal class PlaybackActionTransmitterImpl(private val stateProvider: StateStore.Provider) : PlaybackActionTransmitter {

    private val actionListeners
        get() = PlaybackActionListenerHolder.actionlisteners

    private val playbackStateListener
        get() = PlaybackActionListenerHolder.stateListener

    private var disposables = CompositeDisposable()

    private var seekStartState: ArmadilloState? = null
    private var currentState: ArmadilloState = ArmadilloState(null, emptyList(), InternalState(), null)
    private var lastState: ArmadilloState = currentState
    private var pollingIntervalMillis: Milliseconds = 500.milliseconds

    @VisibleForTesting
    internal val audiobook: AudioPlayable?
        get() = currentState.playbackInfo?.audioPlayable

    init {
        Injector.mainComponent.inject(this)
    }

    override fun begin(pollingInterval: Milliseconds) {
        pollingIntervalMillis = pollingInterval
        disposables.clear()
        disposables = CompositeDisposable()
        initPollForListeners()

        disposables.add(stateProvider.stateSubject
            .retry()
            .subscribe({ armadilloState ->
                currentState = armadilloState
                lastState = checkForStateChanges(armadilloState)
            }, { throwable ->
                (throwable as? Exception)?.let {
                    actionListeners.dispatch { listener, state -> listener.onError(ActionListenerException(it), state) }
                }
            }))
    }

    override fun destroy() {
        disposables.clear()
        currentState = ArmadilloState(null, emptyList(), InternalState(), null)
        lastState = currentState
        seekStartState = null
    }

    private fun checkForStateChanges(current: ArmadilloState): ArmadilloState {
        var stateToRetainForLast = current

        //New AudioPlayable
        if (current.playbackInfo?.controlState?.isStartingNewAudioPlayable == true && lastState.playbackInfo?.controlState?.isStartingNewAudioPlayable != true) {
            actionListeners.dispatch { listener, state -> listener.onNewAudiobook(state) }
            playbackStateListener?.onNewAudiobook(current.playbackInfo.audioPlayable)
        }

        //Buffer Start
        if (current.playbackInfo?.isLoading == true && lastState.playbackInfo?.isLoading == false) {
            actionListeners.dispatch { listener, state -> listener.onLoadingStart(state) }
        }

        //Buffer End
        if (current.playbackInfo?.isLoading == false && lastState.playbackInfo?.isLoading == true) {
            actionListeners.dispatch { listener, state -> listener.onLoadingEnd(state) }
        }

        //Play
        if (current.playbackInfo?.playbackState == PlaybackState.PLAYING && lastState.playbackInfo?.playbackState != PlaybackState.PLAYING) {
            actionListeners.dispatch { listener, state -> listener.onPlay(state) }
        }

        //Pause
        if (current.playbackInfo?.playbackState == PlaybackState.PAUSED && lastState.playbackInfo?.playbackState != PlaybackState.PAUSED) {
            actionListeners.dispatch { listener, state -> listener.onPause(state) }
        }

        //Stop
        if (current.playbackInfo?.controlState?.isStopping == true && lastState.playbackInfo?.controlState?.isStopping == false) {
            actionListeners.dispatch { listener, state -> listener.onStop(state) }
            playbackStateListener?.onPlaybackEnd()
            stateToRetainForLast = ArmadilloState(null, emptyList(), InternalState(), null)
            seekStartState = null
        }

        // Playback Ended
        if (current.playbackInfo?.controlState?.hasContentEnded == true && lastState.playbackInfo?.controlState?.hasContentEnded == false) {
            actionListeners.dispatch { listener, state ->
                listener.onContentEnded(state)
            }
            playbackStateListener?.onPlaybackEnd()
        }

        //Seek
        if (current.playbackInfo?.controlState?.isSeeking == true && lastState.playbackInfo?.controlState?.isSeeking == false) {
            seekStartState = current
            actionListeners.dispatch { listener, state ->
                listener.onDiscontinuity(state)
            }
        } else if (current.playbackInfo?.controlState?.isSeeking == false && lastState.playbackInfo?.controlState?.isSeeking == true) {
            seekStartState?.let { seekState ->
                seekState.playbackInfo?.controlState?.let {
                    when {
                        it.isFastForwarding -> actionListeners.dispatch { listener, state ->
                            listener.onFastForward(seekState, state)
                        }
                        it.isRewinding -> actionListeners.dispatch { listener, state ->
                            listener.onRewind(seekState, state)
                        }
                        it.isNextChapter -> actionListeners.dispatch { listener, state ->
                            listener.onSkipToNext(seekState, state)
                        }
                        it.isPrevChapter -> actionListeners.dispatch { listener, state ->
                            listener.onSkipToPrevious(seekState, state)
                        }
                        else -> actionListeners.dispatch { listener, state ->
                            listener.onSeek(it.seekTarget ?: 0.milliseconds, seekState, state)
                        }
                    }
                }
            }
        }

        //Progress Update
        if (current.playbackInfo?.controlState?.isPlaybackStateUpdating == true && lastState.playbackInfo?.controlState?.isPlaybackStateUpdating == false) {
            current.playbackInfo.audioPlayable.let { audiobook ->
                val currentChapterIndex = current.playbackInfo.controlState.updatedChapterIndex
                current.playbackInfo.let { localPlaybackInfo ->
                    playbackStateListener?.onPlaybackStateChange(localPlaybackInfo, audiobook, currentChapterIndex)
                }
            }
        }

        //Playback Speed
        if (lastState.playbackInfo != null && (current.playbackInfo?.playbackSpeed != lastState.playbackInfo?.playbackSpeed)) {
            actionListeners.dispatch { listener, state ->
                listener.onSpeedChange(current,
                    seekStartState?.playbackInfo?.playbackSpeed ?: 1.0f,
                    state.playbackInfo?.playbackSpeed ?: 1.0f)
            }
        }

        //Skip Distance
        if (lastState.playbackInfo != null && (current.playbackInfo?.skipDistance != lastState.playbackInfo?.skipDistance)) {
            actionListeners.dispatch { listener, state ->
                listener.onSkipDistanceChange(current,
                    seekStartState?.playbackInfo?.skipDistance ?: Constants.AUDIO_SKIP_DURATION,
                    state.playbackInfo?.skipDistance ?: Constants.AUDIO_SKIP_DURATION)
            }
        }

        //Custom Action
        if (current.playbackInfo?.controlState?.isCustomAction == true && lastState.playbackInfo?.controlState?.isCustomAction == false) {
            actionListeners.dispatch { listener, state ->
                listener.onCustomAction(state.playbackInfo?.controlState?.customMediaActionName ?: "", state)
            }
        }

        //Error
        current.error?.let { error ->
            actionListeners.dispatch { listener, state ->
                listener.onError(error, state)
            }
        }

        return stateToRetainForLast
    }

    private fun initPollForListeners() {
        disposables.add(Observable.interval(pollingIntervalMillis.longValue, TimeUnit.MILLISECONDS)
            .subscribe {
                actionListeners.dispatch { listener, state ->
                    listener.onPoll(state)
                }
            })
    }

    private fun List<PlaybackActionListener>.dispatch(lambda: (listener: PlaybackActionListener, state: ArmadilloState) -> Unit) {
        this.forEach { listener ->
            currentState.let {
                lambda.invoke(listener, it)
            }
        }
    }

}