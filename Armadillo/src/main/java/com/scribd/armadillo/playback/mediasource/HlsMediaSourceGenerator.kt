package com.scribd.armadillo.playback.mediasource

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadHelper
import androidx.media3.exoplayer.source.MediaSource
import com.scribd.armadillo.Constants
import com.scribd.armadillo.HeadersStore
import com.scribd.armadillo.download.CacheManager
import com.scribd.armadillo.download.DownloadTracker
import com.scribd.armadillo.extensions.toUri
import com.scribd.armadillo.models.AudioPlayable
import javax.inject.Inject

/**
 * creates an HLS media source for [ExoPlayer] from a master playlist url
 *
 */
@OptIn(UnstableApi::class)
internal class HlsMediaSourceGenerator @Inject constructor(
    private val cacheManager: CacheManager,
    private val headersStore: HeadersStore,
    private val downloadTracker: DownloadTracker) : MediaSourceGenerator {

    private val previousRequests = mutableMapOf<String, DefaultHttpDataSource.Factory>()

    override fun generateMediaSource(context: Context, request: AudioPlayable.MediaRequest): MediaSource {
        downloadTracker.getDownload(request.url.toUri())?.let {
            if (it.state != Download.STATE_FAILED) {
                return DownloadHelper.createMediaSource(it.request, buildDataSourceFactory(context, request))
            }
        }
        return HlsMediaSource.Factory(buildDataSourceFactory(context, request))
            .createMediaSource(MediaItem.fromUri(request.url))
    }

    override fun updateMediaSourceHeaders(request: AudioPlayable.MediaRequest) {
        previousRequests[request.url]?.let { factory ->
            if (request.headers.isNotEmpty()) {
                headersStore.keyForUrl(request.url)?.let {
                    headersStore.setHeaders(it, request.headers)
                }
                // Updating the factory instance updates future requests generated from this factory by ExoPlayer
                factory.setDefaultRequestProperties(request.headers)
            }
        }
    }

    private fun buildDataSourceFactory(context: Context, request: AudioPlayable.MediaRequest): DataSource.Factory {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(Constants.getUserAgent(context))
            .setConnectTimeoutMs(DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS)
            .setReadTimeoutMs(DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS)
            .setAllowCrossProtocolRedirects(true)

        previousRequests[request.url] = httpDataSourceFactory
        if (request.headers.isNotEmpty()) {
            headersStore.keyForUrl(request.url)?.let {
                headersStore.setHeaders(it, request.headers)
            }
            httpDataSourceFactory.setDefaultRequestProperties(request.headers)
        }

        val upstreamFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        return cacheManager.playbackDataSourceFactory(context, upstreamFactory)
    }
}