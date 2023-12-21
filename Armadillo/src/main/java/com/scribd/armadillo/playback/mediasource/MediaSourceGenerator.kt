package com.scribd.armadillo.playback.mediasource

import android.content.Context
import com.google.android.exoplayer2.source.MediaSource
import com.scribd.armadillo.models.AudioPlayable

/** Creates a MediaSource for starting playback in Exoplayer when this
 * class is initialized. */
internal interface MediaSourceGenerator {
    companion object {
        const val TAG = "MediaSourceGenerator"
    }

    fun generateMediaSource(context: Context, request: AudioPlayable.MediaRequest): MediaSource

    fun updateMediaSourceHeaders(request: AudioPlayable.MediaRequest)
}