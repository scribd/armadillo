package com.scribd.armadillo.playback.mediasource

import android.content.Context
import androidx.media3.exoplayer.source.MediaSource
import com.scribd.armadillo.models.AudioPlayable

/** Creates a MediaSource for starting playback in Exoplayer when this
 * class is initialized. */
internal interface MediaSourceGenerator {
    fun generateMediaSource(context: Context, request: AudioPlayable.MediaRequest): MediaSource

    fun updateMediaSourceHeaders(request: AudioPlayable.MediaRequest)
}