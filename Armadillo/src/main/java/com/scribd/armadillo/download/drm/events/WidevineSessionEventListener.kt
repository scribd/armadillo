package com.scribd.armadillo.download.drm.events

import android.content.Context
import com.google.android.exoplayer2.drm.DrmSessionEventListener
import com.google.android.exoplayer2.source.MediaSource
import com.scribd.armadillo.StateStore
import com.scribd.armadillo.actions.LicenseAcquiredAction
import com.scribd.armadillo.actions.LicenseReleasedAction
import com.scribd.armadillo.di.Injector
import com.scribd.armadillo.encryption.SecureStorage
import com.scribd.armadillo.models.DrmType
import javax.inject.Inject

internal class WidevineSessionEventListener
    : DrmSessionEventListener {

    @Inject
    internal lateinit var stateStore: StateStore.Modifier

    @Inject
    internal lateinit var secureStorage: SecureStorage

    @Inject
    internal lateinit var context: Context

    init {
        Injector.mainComponent.inject(this)
    }

    override fun onDrmSessionAcquired(windowIndex: Int, mediaPeriodId: MediaSource.MediaPeriodId?, state: Int) {
        stateStore.dispatch(LicenseAcquiredAction(type = DrmType.WIDEVINE))
    }

    override fun onDrmSessionReleased(windowIndex: Int, mediaPeriodId: MediaSource.MediaPeriodId?) {
        stateStore.dispatch(LicenseReleasedAction)
    }
}