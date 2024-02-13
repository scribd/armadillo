package com.scribd.armadillo.download

import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.offline.DownloadHelper
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloadRequest
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.Util
import com.scribd.armadillo.Constants
import com.scribd.armadillo.HeadersStore
import com.scribd.armadillo.StateStore
import com.scribd.armadillo.actions.ErrorAction
import com.scribd.armadillo.download.drm.OfflineDrmManager
import com.scribd.armadillo.error.DownloadServiceLaunchedInBackground
import com.scribd.armadillo.extensions.encodeInByteArray
import com.scribd.armadillo.extensions.toUri
import com.scribd.armadillo.hasSnowCone
import com.scribd.armadillo.models.AudioPlayable
import com.scribd.armadillo.playback.createRenderersFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

internal interface DownloadEngine {
    fun init()
    fun download(audioPlayable: AudioPlayable)
    fun removeDownload(audioPlayable: AudioPlayable)
    fun removeAllDownloads()
    fun updateProgress()
}

/**
 * Entry point for downloading.
 * Manages [DownloadTracker] for keeping track of downloads.
 * Starts the [DownloadService] when necessary
 */
@Singleton
internal class ExoplayerDownloadEngine @Inject constructor(
    private val context: Context,
    private val downloadHeadersStore: HeadersStore,
    private val downloadService: Class<out DownloadService>,
    private val downloadManager: DownloadManager,
    private val downloadTracker: DownloadTracker,
    private val stateModifier: StateStore.Modifier,
    private val offlineDrmManager: OfflineDrmManager,
    @Named(Constants.DI.GLOBAL_SCOPE) private val globalScope: CoroutineScope,
) : DownloadEngine {
    override fun init() = downloadTracker.init()

    override fun download(audioPlayable: AudioPlayable) {
        globalScope.launch {
            launch {
                // Download DRM license for offline use
                offlineDrmManager.downloadDrmLicenseForOffline(audioPlayable)
            }

            launch {
                val downloadHelper = downloadHelper(context, audioPlayable.request)

                downloadHelper.prepare(object : DownloadHelper.Callback {
                    override fun onPrepared(helper: DownloadHelper) {
                        val request = helper.getDownloadRequest(audioPlayable.id.encodeInByteArray())
                        try {
                            startDownload(context, request)
                        } catch (e: Exception) {
                            if (hasSnowCone() && e is ForegroundServiceStartNotAllowedException) {
                                stateModifier.dispatch(ErrorAction(DownloadServiceLaunchedInBackground(audioPlayable.id)))
                            } else {
                                stateModifier.dispatch(ErrorAction(com.scribd.armadillo.error.ArmadilloIOException(e)))
                            }
                        }
                    }

                    override fun onPrepareError(helper: DownloadHelper, e: IOException) =
                        stateModifier.dispatch(ErrorAction(com.scribd.armadillo.error.ArmadilloIOException(e)))
                })
            }
        }
    }

    override fun removeDownload(audioPlayable: AudioPlayable) {
        globalScope.launch {
            launch { downloadManager.removeDownload(audioPlayable.request.url) }
            launch { offlineDrmManager.removeDownloadedDrmLicense(audioPlayable) }
        }
    }

    override fun removeAllDownloads() {
        globalScope.launch {
            launch { downloadManager.removeAllDownloads() }
            launch { offlineDrmManager.removeAllDownloadedDrmLicenses() }
        }
    }

    override fun updateProgress() = downloadTracker.updateProgress()

    private fun startDownload(context: Context, downloadRequest: DownloadRequest) =
        DownloadService.sendAddDownload(context, downloadService, downloadRequest, true)

    private fun downloadHelper(context: Context, mediaRequest: AudioPlayable.MediaRequest): DownloadHelper {
        val uri = mediaRequest.url.toUri()
        val renderersFactory = createRenderersFactory(context)
        val dataSourceFactory = DefaultHttpDataSource.Factory().setUserAgent(Constants.getUserAgent(context))

        if (mediaRequest.headers.isNotEmpty()) {
            downloadHeadersStore.keyForUrl(mediaRequest.url)?.let {
                downloadHeadersStore.setHeaders(it, mediaRequest.headers)
            }
            dataSourceFactory.setDefaultRequestProperties(mediaRequest.headers)
        }
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .build()
        return when (@C.ContentType val type = Util.inferContentType(uri)) {
            C.TYPE_HLS,
            C.TYPE_DASH ->
                DownloadHelper.forMediaItem(context, mediaItem, renderersFactory, DefaultDataSource.Factory(context, dataSourceFactory))

            C.TYPE_OTHER -> DownloadHelper.forMediaItem(context, mediaItem)
            else -> throw IllegalStateException("Unsupported type: $type")
        }
    }
}