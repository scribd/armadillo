package com.scribd.armadillo.playback.mediasource

import android.content.Context
import com.google.android.exoplayer2.upstream.DataSource
import com.scribd.armadillo.models.AudioPlayable

internal interface HeadersMediaSourceHelper {
    fun createDataSourceFactory(context: Context, request: AudioPlayable.MediaRequest): DataSource.Factory
    fun updateMediaSourceHeaders(request: AudioPlayable.MediaRequest)
}