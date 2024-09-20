package com.scribd.armadillo.playback.mediasource

import android.content.Context
import android.util.Log
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadHelper
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.scribd.armadillo.download.DownloadTracker
import com.scribd.armadillo.models.AudioPlayable
import javax.inject.Inject

/**
 * creates an HLS media source for [ExoPlayer] from a master playlist url
 *
 */
internal class HlsMediaSourceGenerator @Inject constructor(
    private val mediaSourceHelper: HeadersMediaSourceFactoryFactory,
    private val downloadTracker: DownloadTracker) : MediaSourceGenerator {


    override fun generateMediaSource(mediaId: String, context: Context, request: AudioPlayable.MediaRequest): MediaSource {
        val dataSourceFactory = mediaSourceHelper.createDataSourceFactory(context, request)

        downloadTracker.getDownload(id = mediaId, uri = request.url)?.let {
            if (it.state != Download.STATE_FAILED) {
                return DownloadHelper.createMediaSource(it.request, dataSourceFactory)
            }
        }

        if (request.drmInfo != null) {
            Log.e(MediaSourceGenerator.TAG, "HLS does not currently support DRM")
        }

        return HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(request.url))
    }

    override fun updateMediaSourceHeaders(request: AudioPlayable.MediaRequest) = mediaSourceHelper.updateMediaSourceHeaders(request)
}