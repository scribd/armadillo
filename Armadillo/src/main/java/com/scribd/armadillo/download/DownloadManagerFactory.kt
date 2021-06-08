package com.scribd.armadillo.download

import android.content.Context
import com.google.android.exoplayer2.database.DatabaseProvider
import com.google.android.exoplayer2.offline.DefaultDownloadIndex
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloaderFactory
import com.scribd.armadillo.Constants
import com.scribd.armadillo.encryption.ExoplayerEncryption
import javax.inject.Inject
import javax.inject.Singleton

internal interface DownloadManagerFactory {
    fun build(context: Context, databaseProvider: DatabaseProvider): DownloadManager
}

/**
 * All instances of Armadillo must receive the same instance of [DownloadManager]. This is probably best accomplished through DI
 */
@Singleton
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