package com.scribd.armadillotestapp.presentation.analytics

import android.util.Log
import com.scribd.armadillo.analytics.PlaybackActionListener
import com.scribd.armadillo.models.ArmadilloState
import com.scribd.armadillo.time.Interval
import com.scribd.armadillo.time.Millisecond
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

class ArmadilloPlaybackActionListener : PlaybackActionListener {

    private val disposables = CompositeDisposable()
    private val publishSubject = PublishSubject.create<ArmadilloState>()

    private companion object {
        const val TAG = "PlaybackActionListener"
    }

    override fun onPoll(state: ArmadilloState) = publishSubject.onNext(state)

    override fun onNewAudiobook(state: ArmadilloState) {
        initPoll()
        Log.v(TAG, "onNewAudiobook: " + state.toString())
    }

    override fun onLoadingStart(state: ArmadilloState) {
        Log.v(TAG, "onLoadStart: $state")
    }

    override fun onLoadingEnd(state: ArmadilloState) {
        Log.v(TAG, "onLoadEnd: $state")
    }

    override fun onPlay(state: ArmadilloState) {
        Log.v(TAG, "onPlay: $state")
    }

    override fun onPause(state: ArmadilloState) {
        Log.v(TAG, "onPause: $state")
    }

    override fun onStop(state: ArmadilloState) {
        Log.v(TAG, "onStop: $state")
        disposables.clear()
    }

    override fun onDiscontinuity(state: ArmadilloState) {
        Log.v(TAG, "on discontinuity")
    }

    override fun onFastForward(beforeState: ArmadilloState, afterState: ArmadilloState) {
        Log.v(TAG, "onFastForward: $afterState")
    }

    override fun onRewind(beforeState: ArmadilloState, afterState: ArmadilloState) {
        Log.v(TAG, "onRewind: $afterState")
    }

    override fun onSkipToNext(beforeState: ArmadilloState, afterState: ArmadilloState) {
        Log.v(TAG, "onSkipToNext: $afterState")
    }

    override fun onSkipToPrevious(beforeState: ArmadilloState, afterState: ArmadilloState) {
        Log.v(TAG, "onSkipToPrevious: $afterState")
    }

    override fun onSeek(seekTarget: Interval<Millisecond>, beforeState: ArmadilloState, afterState: ArmadilloState) {
        Log.v(TAG, "onSeekTo: $afterState")
    }

    override fun onSkipDistanceChange(state: ArmadilloState, oldDistance: Interval<Millisecond>, newDistance: Interval<Millisecond>) {
        Log.v(TAG, "onSkipDistance: $newDistance")
    }

    override fun onSpeedChange(state: ArmadilloState, oldSpeed: Float, newSpeed: Float) {
        Log.v(TAG, "onSpeedChange: $newSpeed")
    }

    private fun initPoll() {
        disposables.clear()
        disposables.add(publishSubject
                .sample(5, TimeUnit.SECONDS) // looks into the sequence of elements and emits the last item that was produced within the duration
                .subscribe {
                    Log.v(TAG, "onPoll: " + it.toString())
                })
    }
}