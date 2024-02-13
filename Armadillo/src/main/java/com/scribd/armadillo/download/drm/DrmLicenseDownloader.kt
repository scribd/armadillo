package com.scribd.armadillo.download.drm

import com.scribd.armadillo.models.DrmDownload
import com.scribd.armadillo.models.DrmInfo

/**
 * This is a helper class responsible for downloading the DRM license to local storage for a DRM-protected content.
 * This downloaded license can then be retrieved for offline usage using its key ID.
 */
internal interface DrmLicenseDownloader {
    companion object {
        const val TAG = "DrmLicenseDownloader"
    }

    /**
     * Download and persist the DRM license
     * @return object containing information about the downloaded DRM license
     */
    suspend fun downloadDrmLicense(
        requestUrl: String,
        customRequestHeaders: Map<String, String>,
        drmInfo: DrmInfo,
    ): DrmDownload

    /**
     * Release a downloaded DRM license so it's no longer valid for usage.
     */
    suspend fun releaseDrmLicense(drmDownload: DrmDownload)
}