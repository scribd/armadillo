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
import com.scribd.armadillo.error.ArmadilloException
import com.scribd.armadillo.error.ArmadilloIOException
import com.scribd.armadillo.error.DownloadServiceLaunchedInBackground
import com.scribd.armadillo.error.DrmDownloadException
import com.scribd.armadillo.error.UnexpectedDownloadException
import com.scribd.armadillo.extensions.encodeInByteArray
import com.scribd.armadillo.extensions.toUri
import com.scribd.armadillo.hasSnowCone
import com.scribd.armadillo.models.AudioPlayable
import com.scribd.armadillo.playback.createRenderersFactory
import kotlinx.coroutines.CoroutineExceptionHandler
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
    fun redownloadDrmLicense(id: String, request: AudioPlayable.MediaRequest)
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
    @Named(Constants.DI.GLOBAL_SCOPE) private val scope: CoroutineScope,
) : DownloadEngine {
    private val errorHandler = CoroutineExceptionHandler { _, e ->
        stateModifier.dispatch(ErrorAction(
            error = e as? ArmadilloException ?: UnexpectedDownloadException(e)
        ))
    }

    override fun init() = downloadTracker.init()
    override fun download(audioPlayable: AudioPlayable) {
        scope.launch(errorHandler) {
            launch {
                // Download DRM license for offline use
                offlineDrmManager.downloadDrmLicenseForOffline(id = audioPlayable.id.toString(), request = audioPlayable.request)
            }

            launch {
                val downloadHelper = downloadHelper(
                    id = audioPlayable.id.toString(),
                    context = context,
                    mediaRequest = audioPlayable.request
                )

                downloadHelper.prepare(object : DownloadHelper.Callback {
                    override fun onPrepared(helper: DownloadHelper) {
                        var request = helper.getDownloadRequest(audioPlayable.id.encodeInByteArray())
                        request = request.copyWithId(audioPlayable.id.toString())
                        try {
                            startDownload(context, request)
                        } catch (e: Exception) {
                            if (hasSnowCone() && e is ForegroundServiceStartNotAllowedException) {
                                stateModifier.dispatch(ErrorAction(DownloadServiceLaunchedInBackground(audioPlayable.id, e)))
                            } else {
                                stateModifier.dispatch(ErrorAction(ArmadilloIOException(cause = e, actionThatFailedMessage = "Can't prepare download.")))
                            }
                        }
                    }

                    override fun onPrepareError(helper: DownloadHelper, e: IOException) =
                        stateModifier.dispatch(ErrorAction(ArmadilloIOException(cause = e, actionThatFailedMessage = "Can't report download error.")))
                })
            }
        }
    }

    override fun removeDownload(audioPlayable: AudioPlayable) {
        scope.launch(errorHandler) {
            launch { downloadManager.removeDownload(audioPlayable.id.toString()) }
            launch { offlineDrmManager.removeDownloadedDrmLicense(id = audioPlayable.id.toString(), request = audioPlayable.request) }
        }
    }

    override fun removeAllDownloads() {
        scope.launch(errorHandler) {
            launch { downloadManager.removeAllDownloads() }
            launch { offlineDrmManager.removeAllDownloadedDrmLicenses() }
        }
    }

    override fun updateProgress() = downloadTracker.updateProgress()

    override fun redownloadDrmLicense(id: String, request: AudioPlayable.MediaRequest) {
        scope.launch(errorHandler) {
            try {
                offlineDrmManager.downloadDrmLicenseForOffline(id = id, request = request)
            } catch (ex: DrmDownloadException){
                //continue to try and use old license - a playback error appears elsewhere
            }
        }
    }

    private fun startDownload(context: Context, downloadRequest: DownloadRequest) =
        DownloadService.sendAddDownload(context, downloadService, downloadRequest, true)

    private fun downloadHelper(id: String, context: Context, mediaRequest: AudioPlayable.MediaRequest): DownloadHelper {
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
            .setMediaId(id)
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