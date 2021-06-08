package com.scribd.armadillo.playback.mediasource

import android.content.Context
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.upstream.cache.Cache
import com.scribd.armadillo.models.AudioPlayable

/** Creates a MediaSource for starting playback in Exoplayer when this
 * class is initialized. */
internal interface MediaSourceGenerator {
    fun generateMediaSource(context: Context, request: AudioPlayable.MediaRequest): MediaSource
}