package com.scribd.armadillo.download

import android.content.Context
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.offline.DownloadHelper
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloadRequest
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.Util
import com.scribd.armadillo.Constants
import com.scribd.armadillo.HeadersStore
import com.scribd.armadillo.StateStore
import com.scribd.armadillo.actions.ErrorAction
import com.scribd.armadillo.extensions.encodeInByteArray
import com.scribd.armadillo.extensions.toUri
import com.scribd.armadillo.models.AudioPlayable
import com.scribd.armadillo.playback.createRenderersFactory
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

internal interface DownloadEngine {
    fun init()
    fun download(audiobook: AudioPlayable)
    fun removeDownload(audiobook: AudioPlayable)
    fun removeAllDownloads()
    fun updateProgress()
}

/**
 * Entry point for downloading.
 * Manages [DownloadTracker] for keeping track of downloads.
 * Starts the [DownloadService] when necessary
 */
@Singleton
internal class ExoplayerDownloadEngine @Inject constructor(private val context: Context,
                                                           private val downloadHeadersStore: HeadersStore,
                                                           private val downloadService: Class<out DownloadService>,
                                                           private val downloadManager: DownloadManager,
                                                           private val downloadTracker: DownloadTracker,
                                                           private val stateModifier: StateStore.Modifier) : DownloadEngine {
    override fun init() = downloadTracker.init()

    override fun download(audiobook: AudioPlayable) {
        val downloadHelper = downloadHelper(context, audiobook.request)

        downloadHelper.prepare(object : DownloadHelper.Callback {
            override fun onPrepared(helper: DownloadHelper) {
                val request = helper.getDownloadRequest(audiobook.id.encodeInByteArray())
                startDownload(context, request)
            }

            override fun onPrepareError(helper: DownloadHelper, e: IOException) =
                stateModifier.dispatch(ErrorAction(com.scribd.armadillo.error.ArmadilloIOException(e)))
        })
    }

    override fun removeDownload(audiobook: AudioPlayable) =
        DownloadService.sendRemoveDownload(context, downloadService, audiobook.request.url, true)

    override fun removeAllDownloads() = downloadManager.removeAllDownloads()

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
            C.TYPE_HLS ->
                DownloadHelper.forMediaItem(context, mediaItem, renderersFactory, DefaultDataSource.Factory(context, dataSourceFactory))
            C.TYPE_OTHER -> DownloadHelper.forMediaItem(context, mediaItem)
            else -> throw IllegalStateException("Unsupported type: $type")
        }
    }
}