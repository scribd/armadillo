package com.scribd.armadillo.playback.mediasource

import android.content.Context
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaItem.DrmConfiguration
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
    private val downloadTracker: DownloadTracker) : MediaSourceGenerator {

    override fun generateMediaSource(context: Context, request: AudioPlayable.MediaRequest): MediaSource {
        val dataSourceFactory = mediaSourceHelper.createDataSourceFactory(context, request)

        downloadTracker.getDownload(request.url.toUri())?.let {
            if (it.state != Download.STATE_FAILED) {
                return DownloadHelper.createMediaSource(it.request, dataSourceFactory)
            }
        }

        val mediaItemBuilder = MediaItem.Builder()
            .setUri(request.url)

        if (request.drmInfo != null) {
            mediaItemBuilder.setDrmConfiguration(
                DrmConfiguration.Builder(request.drmInfo.drmType.toExoplayerConstant())
                    .setLicenseUri(request.drmInfo.licenseServer)
                    .setLicenseRequestHeaders(request.drmInfo.drmHeaders)
                    .build()
            )
        }

        return DashMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItemBuilder.build())
    }

    override fun updateMediaSourceHeaders(request: AudioPlayable.MediaRequest) = mediaSourceHelper.updateMediaSourceHeaders(request)
}