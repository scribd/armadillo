package com.scribd.armadillo.download

import com.google.android.exoplayer2.offline.DownloadManager
import com.scribd.armadillo.ExoplayerDownload
import com.scribd.armadillo.extensions.decodeToInt
import com.scribd.armadillo.models.DownloadProgressInfo
import com.scribd.armadillo.models.DownloadState

/**
 * [ExoplayerDownload] is not easy to test. It's a final class with private constructor. This class is a valuable intermediate
 * for being able to use this class in testing.
 */
internal data class TestableDownloadState(val id: Int,
                                          val url: String,
                                          val state: Int,
                                          val downloadPercentage: Int,
                                          val downloadedBytes: Long) {
    companion object {
        const val QUEUED = ExoplayerDownload.STATE_QUEUED
        const val COMPLETED = ExoplayerDownload.STATE_COMPLETED
        const val IN_PROGRESS = ExoplayerDownload.STATE_DOWNLOADING
        const val REMOVING = ExoplayerDownload.STATE_REMOVING
        const val FAILED = ExoplayerDownload.STATE_FAILED
    }

    constructor(download: ExoplayerDownload) : this(
            download.request.data.decodeToInt(),
            download.request.uri.toString(),
            download.state,
            download.percentDownloaded.toInt(),
            download.bytesDownloaded)

    /**
     * This method converts [TestableDownloadState] (a testable wrapper fo exoplayer's [DownloadManager.TaskState])
     * to armadillo's [DownloadProgressInfo].
     * This method returns null when there is no need to report an intermediate progress state.
     */
    fun toDownloadInfo(): DownloadProgressInfo? {
        val downloadState = when (state) {
            REMOVING -> DownloadState.REMOVED
            COMPLETED -> DownloadState.COMPLETED
            IN_PROGRESS -> {
                val percent = if (DownloadProgressInfo.PROGRESS_UNSET == downloadPercentage) {
                    0
                } else {
                    downloadPercentage
                }
                DownloadState.STARTED(percent, downloadedBytes)
            }
            QUEUED -> return null
            else -> DownloadState.FAILED
        }

        return DownloadProgressInfo(
                id = id,
                url = url,
                downloadState = downloadState)
    }
}