package com.scribd.armadillo.playback.mediasource

import android.content.Context
import android.os.Handler
import com.google.android.exoplayer2.drm.DrmSessionManagerProvider
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadHelper
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.scribd.armadillo.StateStore
import com.scribd.armadillo.actions.OpeningLicenseAction
import com.scribd.armadillo.download.DownloadEngine
import com.scribd.armadillo.download.DownloadTracker
import com.scribd.armadillo.download.drm.events.WidevineSessionEventListener
import com.scribd.armadillo.models.AudioPlayable
import com.scribd.armadillo.models.DrmType
import javax.inject.Inject

/** For playback, both streaming and downloaded */
internal class DashMediaSourceGenerator @Inject constructor(
    context: Context,
    private val mediaSourceHelper: HeadersMediaSourceHelper,
    private val downloadTracker: DownloadTracker,
    private val drmMediaSourceHelper: DrmMediaSourceHelper,
    private val drmSessionManagerProvider: DrmSessionManagerProvider,
    private val downloadEngine: DownloadEngine,
    private val stateStore: StateStore.Modifier,
) : MediaSourceGenerator {

    private val drmHandler = Handler(context.mainLooper)

    override fun generateMediaSource(mediaId: String, context: Context, request: AudioPlayable.MediaRequest): MediaSource {
        if (request.drmInfo != null) {
            stateStore.dispatch(OpeningLicenseAction(request.drmInfo.drmType))
        }
        val dataSourceFactory = mediaSourceHelper.createDataSourceFactory(context, request)

        val download = downloadTracker.getDownload(id = mediaId, uri = request.url)
        val isDownloaded = download != null && download.state == Download.STATE_COMPLETED
        val mediaItem = drmMediaSourceHelper.createMediaItem(
            context = context,
            id = mediaId,
            request = request,
            isDownload = isDownloaded
        )

        return if (isDownloaded) {
            val drmManager = if (request.drmInfo == null) {
                drmSessionManagerProvider.get(mediaItem)
            } else null

            if (request.drmInfo?.drmType == DrmType.WIDEVINE) {
                downloadEngine.redownloadDrmLicense(id = mediaId, request = request)
            }
            DownloadHelper.createMediaSource(download!!.request, dataSourceFactory, drmManager)
        } else {
            var factory = DashMediaSource.Factory(dataSourceFactory)
            if (request.drmInfo != null) {
                factory = factory.setDrmSessionManagerProvider(drmSessionManagerProvider)
            }
            factory.createMediaSource(mediaItem).also { source ->
                //download equivalent is in DashDrmLicenseDownloader
                when (request.drmInfo?.drmType) {
                    DrmType.WIDEVINE -> {
                        source.addDrmEventListener(
                            drmHandler,
                            WidevineSessionEventListener()
                        )
                    }

                    else -> Unit //no DRM
                }
            }
        }
    }

    override fun updateMediaSourceHeaders(request: AudioPlayable.MediaRequest) = mediaSourceHelper.updateMediaSourceHeaders(request)
}