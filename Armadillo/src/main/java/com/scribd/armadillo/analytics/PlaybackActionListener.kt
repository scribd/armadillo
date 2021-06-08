package com.scribd.armadillo.analytics

import com.scribd.armadillo.Milliseconds
import com.scribd.armadillo.error.ArmadilloException
import com.scribd.armadillo.models.ArmadilloState

/**
 * This listener provides callbacks to when actions are called on the audio player
 * It is useful for scenarios where the developer wants to track specific events ex. analytics
 * For UI and most other applications, the developer should rely on [ArmadilloPlayer]
 */
interface PlaybackActionListener {
    fun onPoll(state: ArmadilloState) = Unit

    fun onNewAudiobook(state: ArmadilloState) = Unit
    fun onLoadingStart(state: ArmadilloState) = Unit
    fun onLoadingEnd(state: ArmadilloState) = Unit
    fun onPlay(state: ArmadilloState) = Unit
    fun onPause(state: ArmadilloState) = Unit
    fun onStop(state: ArmadilloState) = Unit

    /** The player has reached the end of the content and stopped */
    fun onContentEnded(state: ArmadilloState) = Unit

    /** When audio begins seeking to a non-contiguous section of audio (eg, seeking, rewinding, etc).*/
    fun onDiscontinuity(state: ArmadilloState) = Unit

    /** Fast forward completed */
    fun onFastForward(beforeState: ArmadilloState, afterState: ArmadilloState) = Unit

    /** Rewind completed */
    fun onRewind(beforeState: ArmadilloState, afterState: ArmadilloState) = Unit

    /** Chapter change completed */
    fun onSkipToNext(beforeState: ArmadilloState, afterState: ArmadilloState) = Unit

    /** Chapter change completed */
    fun onSkipToPrevious(beforeState: ArmadilloState, afterState: ArmadilloState) = Unit

    /** Arbitrary Seek action completed that is not rewinding, changing chapters, or fast forwarding */
    fun onSeek(seekTarget: Milliseconds, beforeState: ArmadilloState, afterState: ArmadilloState) = Unit

    fun onSpeedChange(state: ArmadilloState, oldSpeed: Float, newSpeed: Float) = Unit
    fun onSkipDistanceChange(state: ArmadilloState, oldDistance: Milliseconds, newDistance: Milliseconds) = Unit
    fun onCustomAction(action: String?, state: ArmadilloState) = Unit
    fun onError(armadilloException: ArmadilloException, state: ArmadilloState) = Unit
}