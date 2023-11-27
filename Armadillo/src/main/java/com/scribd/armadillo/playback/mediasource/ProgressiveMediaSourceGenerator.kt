package com.scribd.armadillo.playback.mediasource

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.scribd.armadillo.Constants
import com.scribd.armadillo.download.CacheManager
import com.scribd.armadillo.models.AudioPlayable
import javax.inject.Inject

@OptIn(UnstableApi::class)
internal class ProgressiveMediaSourceGenerator @Inject constructor(
    private val cacheManager: CacheManager) : MediaSourceGenerator {

    override fun generateMediaSource(context: Context, request: AudioPlayable.MediaRequest): MediaSource =
        ProgressiveMediaSource.Factory(buildDataSourceFactory(context)).createMediaSource(MediaItem.fromUri(request.url))

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