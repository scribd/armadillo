package com.scribd.armadillo.playback.mediasource

import android.content.Context
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadHelper
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
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
internal class HlsMediaSourceGenerator @Inject constructor(
    private val cacheManager: CacheManager,
    private val headersStore: HeadersStore,
    private val downloadTracker: DownloadTracker) : MediaSourceGenerator {

    override fun generateMediaSource(context: Context, request: AudioPlayable.MediaRequest): MediaSource {
        downloadTracker.getDownload(request.url.toUri())?.let {
            if (it.state != Download.STATE_FAILED) {
                return DownloadHelper.createMediaSource(it.request, buildDataSourceFactory(context, request))
            }
        }
        return HlsMediaSource.Factory(buildDataSourceFactory(context, request))
            .createMediaSource(MediaItem.fromUri(request.url))
    }

    private fun buildDataSourceFactory(context: Context, request: AudioPlayable.MediaRequest): DataSource.Factory {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(Constants.getUserAgent(context))
            .setConnectTimeoutMs(DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS)
            .setReadTimeoutMs(DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS)
            .setAllowCrossProtocolRedirects(true)

        if (request.headers.isNotEmpty()) {
            headersStore.keyForUrl(request.url)?.let {
                headersStore.setHeaders(it, request.headers)
            }
            httpDataSourceFactory.setDefaultRequestProperties(request.headers)
        }

        val upstreamFactory = DefaultDataSourceFactory(context, httpDataSourceFactory)
        return cacheManager.playbackDataSourceFactory(context, upstreamFactory)
    }
}