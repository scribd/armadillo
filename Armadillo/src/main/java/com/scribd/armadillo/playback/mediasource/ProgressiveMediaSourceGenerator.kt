package com.scribd.armadillo.playback.mediasource

import android.content.Context
import android.util.Log
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.scribd.armadillo.Constants
import com.scribd.armadillo.download.CacheManager
import com.scribd.armadillo.models.AudioPlayable
import javax.inject.Inject

internal class ProgressiveMediaSourceGenerator @Inject constructor(
    private val cacheManager: CacheManager) : MediaSourceGenerator {

    override fun generateMediaSource(context: Context, request: AudioPlayable.MediaRequest): MediaSource =
        ProgressiveMediaSource.Factory(buildDataSourceFactory(context)).createMediaSource(MediaItem.fromUri(request.url)).also {
            if (request.drmInfo != null) {
                Log.e(MediaSourceGenerator.TAG, "Progressive media does not currently support DRM")
            }
        }

    override fun updateMediaSourceHeaders(request: AudioPlayable.MediaRequest) = Unit // Doesn't use headers

    private fun buildDataSourceFactory(context: Context): DataSource.Factory {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(Constants.getUserAgent(context))
            .setConnectTimeoutMs(DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS)
            .setReadTimeoutMs(DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS)
            .setAllowCrossProtocolRedirects(true)
        val upstreamFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        return cacheManager.playbackDataSourceFactory(context, upstreamFactory)
    }
}