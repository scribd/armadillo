package com.scribd.armadillo.playback.mediasource

import android.content.Context
import com.google.android.exoplayer2.drm.DefaultDrmSessionManagerProvider
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadHelper
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.scribd.armadillo.download.DownloadTracker
import com.scribd.armadillo.extensions.toUri
import com.scribd.armadillo.models.AudioPlayable
import javax.inject.Inject

internal class DashMediaSourceGenerator @Inject constructor(
    private val mediaSourceHelper: HeadersMediaSourceHelper,
    private val downloadTracker: DownloadTracker,
    private val drmMediaSourceHelper: DrmMediaSourceHelper,
) : MediaSourceGenerator {

    override fun generateMediaSource(context: Context, request: AudioPlayable.MediaRequest): MediaSource {
        val dataSourceFactory = mediaSourceHelper.createDataSourceFactory(context, request)

        downloadTracker.getDownload(request.url.toUri())?.let {
            if (it.state != Download.STATE_FAILED) {
                val mediaItem = drmMediaSourceHelper.createMediaItem(context = context, request = request, isDownload = true)
                return DownloadHelper.createMediaSource(it.request, dataSourceFactory, DefaultDrmSessionManagerProvider().get(mediaItem))
            }
        }

        val mediaItem = drmMediaSourceHelper.createMediaItem(context = context, request = request, isDownload = false)
        return DashMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItem)
    }

    override fun updateMediaSourceHeaders(request: AudioPlayable.MediaRequest) = mediaSourceHelper.updateMediaSourceHeaders(request)
}