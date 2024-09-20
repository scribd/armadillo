package com.scribd.armadillo.download.drm

import android.media.MediaDrm
import androidx.annotation.GuardedBy
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaItem.DrmConfiguration
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.drm.DrmSessionManagerProvider
import com.google.android.exoplayer2.drm.ExoMediaDrm
import com.google.android.exoplayer2.drm.FrameworkMediaDrm
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.Assertions
import com.google.android.exoplayer2.util.Util
import com.google.common.primitives.Ints
import com.scribd.armadillo.StateStore
import com.scribd.armadillo.actions.LicenseDrmErrorAction
import com.scribd.armadillo.actions.LicenseExpirationDetermined
import com.scribd.armadillo.actions.LicenseExpiredAction
import com.scribd.armadillo.actions.LicenseKeyIsUsableAction
import com.scribd.armadillo.playback.error.ArmadilloHttpErrorHandlingPolicy
import com.scribd.armadillo.time.milliseconds
import java.util.UUID
import javax.inject.Inject

/** Forks [DefaultDrmSessionManagerProvider], but changes the [ExoMediaDrm.Provider] instance
 *
 * note: if moving to media3, maybe AppManagedProvider may help*/
internal class ArmadilloDrmSessionManagerProvider @Inject constructor(private val stateStore: StateStore.Modifier) :
    DrmSessionManagerProvider {

    ////////////////////////////////////////////////////////////////
    // These fields identical to DefaultDrmSessionManagerProvider //

    private val lock = Any()

    @GuardedBy("lock")
    private var drmConfiguration: DrmConfiguration? = null

    @GuardedBy("lock")
    private var manager: DrmSessionManager? = null
    private val drmHttpDataSourceFactory: DataSource.Factory? = null
    private val userAgent: String? = null

    // End DefaultDrmSessionManagerProvider fields //
    /////////////////////////////////////////////////

    companion object {
        const val TAG = "ArmadilloDrmSMProvider"
    }

    /**  Identical to DefaultDrmSessionManagerProvider method, */
    override fun get(mediaItem: MediaItem): DrmSessionManager {
        Assertions.checkNotNull(mediaItem.localConfiguration)
        val drmConfiguration = mediaItem.localConfiguration!!.drmConfiguration
        if (drmConfiguration != null && Util.SDK_INT >= 18) {
            synchronized(this.lock) {
                if (!Util.areEqual(drmConfiguration, this.drmConfiguration)) {
                    this.drmConfiguration = drmConfiguration
                    this.manager = this.createManager(drmConfiguration)
                }
                return Assertions.checkNotNull<DrmSessionManager?>(this.manager)
            }
        } else {
            return DrmSessionManager.DRM_UNSUPPORTED
        }
    }

    /** Near identical to DefaultDrmSessionManagerProvider method, except for the indicated lines */
    private fun createManager(drmConfiguration: DrmConfiguration): DrmSessionManager {
        val dataSourceFactory = this.drmHttpDataSourceFactory
            ?: DefaultHttpDataSource.Factory().setUserAgent(this.userAgent)
        val license = if (drmConfiguration.licenseUri == null) null else  drmConfiguration.licenseUri.toString()
        val httpDrmCallback = HttpMediaDrmCallback(license, drmConfiguration.forceDefaultLicenseUri, dataSourceFactory)

        drmConfiguration.licenseRequestHeaders.entries.forEach { entry ->
            httpDrmCallback.setKeyRequestProperty(entry.key, entry.value)
        }

        val drmSessionManager = DefaultDrmSessionManager.Builder()
            //this line is different, changing the DrmProvider
            .setUuidAndExoMediaDrmProvider(drmConfiguration.scheme, ArmadilloDrmProvider(stateStore))
            .setMultiSession(drmConfiguration.multiSession)
            .setPlayClearSamplesWithoutKeys(drmConfiguration.playClearContentWithoutKey)
            .setUseDrmSessionsForClearContent(*Ints.toArray(drmConfiguration.forcedSessionTrackTypes))
            //this line is also different, adding custom error handling
            .setLoadErrorHandlingPolicy(ArmadilloHttpErrorHandlingPolicy())
            .build(httpDrmCallback)
        drmSessionManager.setMode(DefaultDrmSessionManager.MODE_PLAYBACK, drmConfiguration.keySetId)
        return drmSessionManager
    }

    /** New original provider for this class to supply DRM events to the StateStore */
    class ArmadilloDrmProvider(private val stateStore: StateStore.Modifier) : ExoMediaDrm.Provider {
        private var drmExpirationMillis: Long? = null
        override fun acquireExoMediaDrm(uuid: UUID): ExoMediaDrm {
            //uses the main MediaDrm object that this class uses originally, then after we add new listeners to it.
            val instance = FrameworkMediaDrm.newInstance(uuid)

            //ExoMediaDrm.OnEventListener doesn't do anything at all, so its not used
            if (Util.SDK_INT >= 23) {
                instance.setOnKeyStatusChangeListener { exoMediaDrm, sessionId, keyStatuses, hasNewUsableKey ->
                    keyStatuses.firstOrNull()?.let { keyStatus ->
                        //See MediaPlayer.onPlayerError() for playback-affecting errors. This block is more for transparency.
                        when(keyStatus.statusCode) {
                            MediaDrm.KeyStatus.STATUS_USABLE -> {
                                stateStore.dispatch(LicenseKeyIsUsableAction)
                            }
                            MediaDrm.KeyStatus.STATUS_EXPIRED -> {
                                stateStore.dispatch(LicenseExpiredAction)
                            }
                            MediaDrm.KeyStatus.STATUS_OUTPUT_NOT_ALLOWED -> {
                                stateStore.dispatch(LicenseDrmErrorAction)
                            }
                            MediaDrm.KeyStatus.STATUS_PENDING -> {}
                            MediaDrm.KeyStatus.STATUS_INTERNAL_ERROR -> {
                                stateStore.dispatch(LicenseDrmErrorAction)
                            }
                            MediaDrm.KeyStatus.STATUS_USABLE_IN_FUTURE -> {}
                        }
                    }
                }
                //this listener often fires later than the above one
                instance.setOnExpirationUpdateListener { exoMediaDrm, sessionId, expireMillis ->
                    drmExpirationMillis = expireMillis
                    stateStore.dispatch(LicenseExpirationDetermined(expireMillis.milliseconds))
                }
            }
            return instance
        }
    }
}