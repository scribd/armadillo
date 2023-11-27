package com.scribd.armadillo.download

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import com.scribd.armadillo.Constants
import com.scribd.armadillo.encryption.ExoplayerEncryption
import javax.inject.Inject
import javax.inject.Named

interface CacheManager {
    /**
     * Get a data source suitable for playback purposes. Use this for both online and offline playback
     */
    fun playbackDataSourceFactory(context: Context, upstream: DataSource.Factory): CacheDataSource.Factory

    fun clearPlaybackCache()

    /**
     * Returns the number of bytes in the playback cache
     */
    val playbackCacheSize: Long

    /**
     * Get a data source suitable for download purposes. Use this for requesting a download
     */
    fun downloadDataSourceFactory(context: Context, upstream: DataSource.Factory): CacheDataSource.Factory

    /**
     * Returns the number of bytes in the download cache
     */
    val downloadCacheSize: Long
}

@OptIn(UnstableApi::class)
class CacheManagerImpl @Inject constructor(
    @Named(Constants.DI.PLAYBACK_CACHE) private val playbackCache: Cache,
    @Named(Constants.DI.DOWNLOAD_CACHE) private val downloadCache: Cache,
    private val exoplayerEncryption: ExoplayerEncryption
) : CacheManager {

    /**
     * Creates a data source factory for *playback* purposes. This will be initially pointed at the download cache, but will not have
     * write privileges, so it will only write to the playback cache, but will use content from the download cache if it is available
     */
    override fun playbackDataSourceFactory(context: Context, upstream: DataSource.Factory): CacheDataSource.Factory {
        val playbackCacheFactory = createPlaybackCacheFactory(upstream)
        return CacheDataSource.Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(playbackCacheFactory) // Fall back to playback cache if nothing present
            // No writing to download cache when doing playback
            .setCacheWriteDataSinkFactory(null)
            // But if we're reading, still attempt to read encrypted content from the download cache
            .setCacheReadDataSourceFactory(exoplayerEncryption.dataSourceFactory(FileDataSource.Factory()))
    }

    override fun clearPlaybackCache() {
        playbackCache.keys.forEach {
            playbackCache.removeResource(it)
        }
    }

    override val playbackCacheSize: Long
        get() = playbackCache.cacheSpace

    /**
     * Creates a data source factory for *download* purposes. This will have the *playback* cache as its upstream so that it can copy
     * already cached content from there. This *does* have write enabled so that it can write to the download cache.
     */
    override fun downloadDataSourceFactory(context: Context, upstream: DataSource.Factory): CacheDataSource.Factory {
        val playbackCacheFactory = createPlaybackCacheFactory(upstream)
        return CacheDataSource.Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(playbackCacheFactory) // Fall back to playback cache if nothing available
            // Allow writing when creating for downloading - write encrypted to disk
            .setCacheWriteDataSinkFactory(exoplayerEncryption.dataSinkFactory(downloadCache))
            // Read encrypted from download cache
            .setCacheReadDataSourceFactory(exoplayerEncryption.dataSourceFactory(FileDataSource.Factory()))
    }

    override val downloadCacheSize: Long
        get() = downloadCache.cacheSpace

    /**
     * Creates a [CacheDataSource.Factory] pointing at the playback cache.
     *
     * This will be wrapped by a factory pointing at the download cache so that it can use already downloaded content
     */
    private fun createPlaybackCacheFactory(upstream: DataSource.Factory): CacheDataSource.Factory {
        return CacheDataSource.Factory()
            .setCache(playbackCache)
            .setUpstreamDataSourceFactory(upstream) // Fall back to the upstream cache if nothing present
            // Write encrypted to the playback cache
            .setCacheWriteDataSinkFactory(exoplayerEncryption.dataSinkFactory(playbackCache))
            // Read encrypted from playback cache
            .setCacheReadDataSourceFactory(exoplayerEncryption.dataSourceFactory(FileDataSource.Factory()))
    }
}