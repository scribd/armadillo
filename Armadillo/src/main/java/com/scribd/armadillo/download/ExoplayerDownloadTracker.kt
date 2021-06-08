package com.scribd.armadillo.download

import android.net.Uri
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadManager
import com.scribd.armadillo.Constants
import com.scribd.armadillo.ExoplayerDownload
import com.scribd.armadillo.StateStore
import com.scribd.armadillo.actions.Action
import com.scribd.armadillo.actions.ErrorAction
import com.scribd.armadillo.actions.StopTrackingDownloadAction
import com.scribd.armadillo.actions.UpdateDownloadAction
import com.scribd.armadillo.error.DownloadFailed
import com.scribd.armadillo.error.UnableToLoadDownloadInfo
import com.scribd.armadillo.extensions.toUri
import com.scribd.armadillo.models.DownloadProgressInfo
import com.scribd.armadillo.models.DownloadState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.lang.Exception
import java.util.HashMap
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Listener on [DownloadManager] for when a task is started or finished.
 *
 * Intermediate progress must be queried separately. See [DownloadEngine.updateProgress]
 */
internal interface DownloadTracker {
    fun init()
    fun trackDownload(download: ExoplayerDownload)
    fun getDownload(uri: Uri): ExoplayerDownload?
    fun updateProgress()
    suspend fun loadDownloads()
}

/**
 * Wraps [DownloadManager].
 * Manages the [ExoplayerDownload] actions in memory
 * Updates based on changes to [DownloadManager] with [DownloadManager.Listener].
 *
 * The client will be notified that a download is complete once.
 */
@Singleton
internal class ExoplayerDownloadTracker @Inject constructor(
    @Named(Constants.DI.GLOBAL_SCOPE) private val globalScope: CoroutineScope,
    private val downloadManager: DownloadManager,
    private val stateModifier: StateStore.Modifier) : DownloadTracker {

    companion object {
        private const val TAG = "DownloadTracker"
    }

    private val downloads = HashMap<Uri, ExoplayerDownload>()
    private val downloadIndex = downloadManager.downloadIndex

    private var isInitialized = false

    override fun init() {
        if (!isInitialized) {
            isInitialized = true
            downloadManager.addListener(DownloadManagerListener())

            globalScope.launch {
                loadDownloads()
            }
        }
    }

    /**
     * This method gets all content that is currently downloading, and then makes a call to the download service to begin
     * the download (which will resume the download for the user). Currently the client has it's own queueing system, but if it ever
     * enables parallel downloads we should call this method on app start.
     */
    override suspend fun loadDownloads() {
        withContext(Dispatchers.IO) {
            try {
                downloadIndex.getDownloads(
                    ExoplayerDownload.STATE_DOWNLOADING,
                    ExoplayerDownload.STATE_COMPLETED,
                    ExoplayerDownload.STATE_QUEUED)
                    .use { loadedDownloads ->
                        while (loadedDownloads.moveToNext()) {
                            val download = loadedDownloads.download
                            downloads[download.request.uri] = download
                            // If we want to resume downloads we should make a call here to the download service to begin download for this uri
                        }
                    }
            } catch (e: IOException) {
                Log.e(TAG, "Failed to load downloads", e)
                withContext(Dispatchers.Main) {
                    stateModifier.dispatch(ErrorAction(UnableToLoadDownloadInfo))
                }
            }
        }
    }

    override fun trackDownload(download: ExoplayerDownload) {
        if (downloads.containsKey(download.request.uri)) {
            return
        }
        downloads[download.request.uri] = download
    }

    override fun getDownload(uri: Uri): ExoplayerDownload? = downloads[uri]

    override fun updateProgress() {
        downloadManager.currentDownloads.forEach { download ->
            downloads[download.request.uri] = download
            TestableDownloadState(download).toDownloadInfo()?.let {
                dispatchActionsForProgress(it)
            }
        }
    }

    /**
     * This listener updates from [DownloadManager]. It dispatches changes to the [StateStore].
     */
    private inner class DownloadManagerListener : DownloadManager.Listener {

        override fun onDownloadChanged(downloadManager: DownloadManager, download: Download, finalException: Exception?) {
            Log.v(TAG, "onDownloadChanged")
            downloads[download.request.uri] = download
            TestableDownloadState(download).toDownloadInfo()?.let {
                dispatchActionsForProgress(it)
            }
        }

        /**
         * [onDownloadRemoved] will receive some overlap with [onDownloadChanged]. This does not seem to be an issue.
         */
        override fun onDownloadRemoved(downloadManager: DownloadManager, download: ExoplayerDownload) {
            Log.v(TAG, "onDownloadRemoved")
            downloads.remove(download.request.uri)
            TestableDownloadState(download).toDownloadInfo()?.let {
                dispatchActionsForProgress(it)
            }
        }
    }

    @VisibleForTesting
    fun dispatchActionsForProgress(downloadInfo: DownloadProgressInfo) {
        val taskFailed = downloadInfo.isFailed()
        val isRemoveDownloadComplete = downloadInfo.downloadState is DownloadState.REMOVED
        val isDownloadComplete = downloadInfo.isDownloaded()

        Log.v(TAG, "dispatchActionsForProgress: ${downloadInfo.downloadState}")

        val actions = mutableListOf<Action>()
        when {
            isDownloadComplete || isRemoveDownloadComplete || taskFailed -> {
                actions.add(UpdateDownloadAction(downloadInfo)) // notify subscribers that download is complete
                actions.add(StopTrackingDownloadAction(downloadInfo)) // remove this download from ArmadilloState
                if (!isDownloadComplete) {
                    stopTracking(downloadInfo)
                }
            }
            else -> actions.add(UpdateDownloadAction(downloadInfo))
        }

        if (taskFailed) {
            actions.add(ErrorAction(DownloadFailed))
        }

        actions.forEach {
            stateModifier.dispatch(it)
        }
    }

    private fun stopTracking(downloadInfo: DownloadProgressInfo) {
        downloads.remove(downloadInfo.url.toUri())
    }
}
