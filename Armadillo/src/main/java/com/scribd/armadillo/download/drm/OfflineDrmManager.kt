package com.scribd.armadillo.download.drm

import android.content.Context
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.util.Log
import com.google.android.exoplayer2.util.Util
import com.scribd.armadillo.encryption.SecureStorage
import com.scribd.armadillo.error.DrmContentTypeUnsupportedException
import com.scribd.armadillo.extensions.toUri
import com.scribd.armadillo.models.AudioPlayable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class responsible for handling DRM downloading/persistence
 */
@Singleton
internal class OfflineDrmManager @Inject constructor(
    private val context: Context,
    private val secureStorage: SecureStorage,
    private val dashDrmLicenseDownloader: DashDrmLicenseDownloader,
) {
    companion object {
        private const val TAG = "OfflineDrmManager"
    }

    suspend fun downloadDrmLicenseForOffline(audiobook: AudioPlayable) {
        withContext(Dispatchers.IO) {
            audiobook.request.drmInfo?.let { drmInfo ->
                val drmResult = when (@C.ContentType val type = Util.inferContentType(audiobook.request.url.toUri(), null)) {
                    C.TYPE_DASH -> dashDrmLicenseDownloader
                    else -> throw DrmContentTypeUnsupportedException(type)
                }.downloadDrmLicense(
                    requestUrl = audiobook.request.url,
                    customRequestHeaders = audiobook.request.headers,
                    drmInfo = drmInfo,
                )

                // Persist DRM result, which includes the key ID that can be used to retrieve the offline license
                secureStorage.saveDrmDownload(context, audiobook.request.url, drmResult)
                Log.i(TAG, "DRM license ready for offline usage")
            }
        }
    }

    suspend fun removeDownloadedDrmLicense(audiobook: AudioPlayable) {
        withContext(Dispatchers.IO) {
            audiobook.request.drmInfo?.let { drmInfo ->
                secureStorage.getDrmDownload(context, audiobook.request.url, drmInfo.drmType)?.let { drmDownload ->
                    // Remove the persisted download info immediately so audio playback would stop using the offline license
                    secureStorage.removeDrmDownload(context, audiobook.request.url, drmInfo.drmType)

                    // Release the DRM license
                    when (val type = drmDownload.audioType) {
                        C.TYPE_DASH -> dashDrmLicenseDownloader
                        else -> throw DrmContentTypeUnsupportedException(type)
                    }.releaseDrmLicense(drmDownload)
                }
            }
        }
    }

    suspend fun removeAllDownloadedDrmLicenses() {
        withContext(Dispatchers.IO) {
            // Make sure that a removal fails, it won't affect the removal of other licenses
            supervisorScope {
                secureStorage.getAllDrmDownloads(context).forEach { drmDownloadPair ->
                    launch {
                        // Remove the persisted download info immediately so audio playback would stop using the offline license
                        secureStorage.removeDrmDownload(context, drmDownloadPair.key)

                        // Release the DRM license
                        when (val type = drmDownloadPair.value.audioType) {
                            C.TYPE_DASH -> dashDrmLicenseDownloader
                            else -> throw DrmContentTypeUnsupportedException(type)
                        }.releaseDrmLicense(drmDownloadPair.value)
                    }
                }
            }
        }
    }
}