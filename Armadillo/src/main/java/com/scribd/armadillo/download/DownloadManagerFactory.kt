package com.scribd.armadillo.download

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.exoplayer.offline.DefaultDownloadIndex
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloaderFactory
import com.scribd.armadillo.Constants
import javax.inject.Inject
import javax.inject.Singleton

internal interface DownloadManagerFactory {
    fun build(context: Context, databaseProvider: DatabaseProvider): DownloadManager
}

/**
 * All instances of Armadillo must receive the same instance of [DownloadManager]. This is probably best accomplished through DI
 */
@Singleton
@OptIn(UnstableApi::class)
internal class ArmadilloDownloadManagerFactory @Inject constructor(
    private val downloaderFactory: DownloaderFactory) : DownloadManagerFactory {

    override fun build(context: Context, databaseProvider: DatabaseProvider): DownloadManager =
        DownloadManager(
            context,
            DefaultDownloadIndex(databaseProvider),
            downloaderFactory).apply {
            maxParallelDownloads = Constants.MAX_PARALLEL_DOWNLOADS
        }
}