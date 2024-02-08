package com.scribd.armadillo.download.drm

import android.content.Context
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.util.Log
import com.google.android.exoplayer2.util.Util
import com.scribd.armadillo.Constants
import com.scribd.armadillo.encryption.SecureStorage
import com.scribd.armadillo.error.DrmContentTypeUnsupportedException
import com.scribd.armadillo.extensions.toUri
import com.scribd.armadillo.models.AudioPlayable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Manager class responsible for handling DRM downloading/persistence
 */
@Singleton
internal class OfflineDrmManager @Inject constructor(
    private val context: Context,
    @Named(Constants.DI.GLOBAL_SCOPE) private val globalScope: CoroutineScope,
    private val secureStorage: SecureStorage,
    private val dashDrmLicenseDownloader: DashDrmLicenseDownloader,
) {
    companion object {
        private const val TAG = "OfflineDrmManager"
    }

    fun downloadDrmLicenseForOffline(audiobook: AudioPlayable) {
        globalScope.launch(Dispatchers.IO) {
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
}