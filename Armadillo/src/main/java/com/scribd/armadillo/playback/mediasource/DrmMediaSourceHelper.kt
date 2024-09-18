package com.scribd.armadillo.playback.mediasource

import android.content.Context
import com.google.android.exoplayer2.MediaItem
import com.scribd.armadillo.encryption.SecureStorage
import com.scribd.armadillo.error.DrmPlaybackException
import com.scribd.armadillo.models.AudioPlayable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This is a helper responsible for generating the correct media source for an audio request.
 *
 * This will apply the correct DRM-related information needed for content decryption (if the content is DRM-protected).
 * In case of a download media (the content is either downloaded or being downloaded), this includes the DRM key ID used for retrieving
 * the local DRM license (instead of fetching DRM license from the server).
 */
internal interface DrmMediaSourceHelper {
    fun createMediaItem(
        context: Context,
        id: String,
        request: AudioPlayable.MediaRequest,
        isDownload: Boolean,
    ): MediaItem
}

@Singleton
internal class DrmMediaSourceHelperImpl @Inject constructor(private val secureStorage: SecureStorage) : DrmMediaSourceHelper {

    override fun createMediaItem(context: Context, id: String, request: AudioPlayable.MediaRequest, isDownload: Boolean): MediaItem =
        MediaItem.Builder()
            .setUri(request.url)
            .apply {
                // Apply DRM config if content is DRM-protected
                val drmConfig = request.drmInfo?.let { drmInfo ->
                    MediaItem.DrmConfiguration.Builder(drmInfo.drmType.toExoplayerConstant())
                        .setLicenseUri(drmInfo.licenseServer)
                        .setLicenseRequestHeaders(drmInfo.drmHeaders)
                        .apply {
                            // If the content is a download content, use the saved offline DRM key id.
                            // This ID is needed to retrieve the local DRM license for content decryption.
                            if (isDownload) {
                                secureStorage.getDrmDownload(context = context, id =  id, drmType = drmInfo.drmType)?.let { drmDownload ->
                                    setKeySetId(drmDownload.drmKeyId)
                                } ?: throw DrmPlaybackException(IllegalStateException("No DRM key id saved for download content"))
                            }
                        }
                        .build()
                }
                setDrmConfiguration(drmConfig)
            }
            .build()
}
