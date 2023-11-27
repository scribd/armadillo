package com.scribd.armadillo.download

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.offline.DefaultDownloaderFactory
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.Downloader
import com.scribd.armadillo.Constants
import com.scribd.armadillo.HeadersStore
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

/**
 * Downloader factory that applies headers to all requests for a download. Due to https://github.com/google/ExoPlayer/issues/6166, this
 * needs to recreate the factory for each download
 *
 * This factory assumes that an audiobook requiring headers will have a URI segment with an integer ID.
 *
 * // TODO [29]: Remove this once exoplayer is fixed
 */
@OptIn(UnstableApi::class)
class HeaderAwareDownloaderFactory @Inject constructor(private val context: Context,
                                                       private val headersStore: HeadersStore,
                                                       private val cacheManager: CacheManager,
                                                       private val httpDataSourceFactory: DefaultHttpDataSource.Factory) :
    DefaultDownloaderFactory(
        // Create a minimal factory. We'll create a new one on the fly later to actually create the downloader
        cacheManager.downloadDataSourceFactory(context, httpDataSourceFactory),
        threadPool
    ) {

    private companion object {
        val threadPool: ExecutorService = Executors.newFixedThreadPool(Constants.MAX_PARALLEL_DOWNLOADS)
    }

    override fun createDownloader(request: DownloadRequest): Downloader {
        headersStore.keyForUrl(request.uri.toString())?.let { key ->
            headersStore.headersForKey(key)?.forEach {
                httpDataSourceFactory.setDefaultRequestProperties(mapOf(it.key to it.value))
            }
        }

        val defaultDataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        val defaultDownloaderFactory = DefaultDownloaderFactory(
            cacheManager.downloadDataSourceFactory(context, defaultDataSourceFactory),
            threadPool
        )
        return defaultDownloaderFactory.createDownloader(request)
    }
}