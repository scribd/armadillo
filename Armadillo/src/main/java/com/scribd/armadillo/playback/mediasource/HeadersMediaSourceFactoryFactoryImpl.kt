package com.scribd.armadillo.playback.mediasource

import android.content.Context
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.scribd.armadillo.Constants
import com.scribd.armadillo.HeadersStore
import com.scribd.armadillo.download.CacheManager
import com.scribd.armadillo.models.AudioPlayable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class HeadersMediaSourceFactoryFactoryImpl @Inject constructor(
    private val cacheManager: CacheManager,
    private val headersStore: HeadersStore
): HeadersMediaSourceFactoryFactory {
    private val previousRequests = mutableMapOf<String, DefaultHttpDataSource.Factory>()

    override fun createDataSourceFactory(context: Context, request: AudioPlayable.MediaRequest): DataSource.Factory {
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
}