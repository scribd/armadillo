package com.scribd.armadillo.download.drm

import android.content.Context
import android.net.Uri
import android.os.Handler
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.drm.DrmSessionEventListener
import com.google.android.exoplayer2.drm.OfflineLicenseHelper
import com.google.android.exoplayer2.source.dash.DashUtil
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.Log
import com.scribd.armadillo.Constants
import com.scribd.armadillo.StateStore
import com.scribd.armadillo.download.drm.events.WidevineSessionEventListener
import com.scribd.armadillo.error.DrmDownloadException
import com.scribd.armadillo.models.DrmDownload
import com.scribd.armadillo.models.DrmInfo
import com.scribd.armadillo.models.DrmType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DashDrmLicenseDownloader @Inject constructor(context: Context, private val stateStore: StateStore.Modifier)
    : DrmLicenseDownloader {

    private val drmDataSourceFactory = DefaultHttpDataSource.Factory().setUserAgent(Constants.getUserAgent(context))
    private val audioDataSourceFactory = DefaultHttpDataSource.Factory().setUserAgent(Constants.getUserAgent(context))
    private val drmEventDispatcher = DrmSessionEventListener.EventDispatcher()
    private val drmHandler = Handler(context.mainLooper)

    private var offlineHelper: OfflineLicenseHelper? = null

    override suspend fun downloadDrmLicense(
        requestUrl: String,
        customRequestHeaders: Map<String, String>,
        drmInfo: DrmInfo,
    ): DrmDownload {
        // Update data source for DRM license to add any DRM-specific request headers
        drmDataSourceFactory.setDefaultRequestProperties(drmInfo.drmHeaders)
        // Update data source for audio to add custom headers
        audioDataSourceFactory.setDefaultRequestProperties(customRequestHeaders)

        // Create helper to download DRM license
        offlineHelper = findOfflineHelper(drmInfo.drmType, drmInfo.licenseServer)
        return try {
            val audioDataSource = audioDataSourceFactory.createDataSource()
            val manifest = DashUtil.loadManifest(audioDataSource, Uri.parse(requestUrl))
            val format = DashUtil.loadFormatWithDrmInitData(audioDataSource, manifest.getPeriod(0))
            format?.let {
                val keyId = offlineHelper!!.downloadLicense(it)
                DrmDownload(
                    drmKeyId = keyId,
                    drmType = drmInfo.drmType,
                    licenseServer = drmInfo.licenseServer,
                    audioType = C.CONTENT_TYPE_DASH,
                )
            } ?: throw IllegalStateException("No media format retrieved for audio request")
        } catch (e: Exception) {
            Log.e(DrmLicenseDownloader.TAG, "Failure to download DRM license for offline usage", e)
            throw DrmDownloadException(e)
        }
    }

    private fun findOfflineHelper(type: DrmType, licenseServerUrl: String): OfflineLicenseHelper =
        when (type) {
            DrmType.WIDEVINE -> {
                OfflineLicenseHelper.newWidevineInstance(
                    licenseServerUrl,
                    drmDataSourceFactory,
                    drmEventDispatcher
                ).also {
                    //streaming equivalent is in DashMediaSourceGenerator
                    drmEventDispatcher.addEventListener(
                        drmHandler,
                        WidevineSessionEventListener()
                    )
                }
            }
        }

    override suspend fun releaseDrmLicense(drmDownload: DrmDownload) {
        val offlineHelper = offlineHelper ?: findOfflineHelper(drmDownload.drmType, drmDownload.licenseServer)
        try {
            offlineHelper.releaseLicense(drmDownload.drmKeyId)
        } catch (e: Exception) {
            Log.e(DrmLicenseDownloader.TAG, "Failure to release downloaded DRM license", e)
            throw DrmDownloadException(e)
        }
    }
}