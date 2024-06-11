package com.scribd.armadillo.playback

import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.analytics.AnalyticsListener.EventTime
import com.google.android.exoplayer2.source.LoadEventInfo
import com.google.android.exoplayer2.source.MediaLoadData
import com.google.android.exoplayer2.util.Log
import java.io.IOException
import java.lang.Exception

class ArmadilloAnalyticsListener : AnalyticsListener {
    override fun onLoadStarted(
        eventTime: EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData
    ) {
        // Called when a media load starts
        Log.e(TAG, "Load started: ${loadEventInfo.describe()}")
    }

    override fun onLoadCompleted(
        eventTime: EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData
    ) {
        // Called when a media load completes
        Log.e(TAG, "Load completed: ${loadEventInfo.describe()}")
    }

    override fun onLoadCanceled(
        eventTime: EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData
    ) {
        // Called when a media load is canceled
        Log.e(TAG, "Load canceled: ${loadEventInfo.describe()}")
    }

    override fun onLoadError(
        eventTime: EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData,
        error: IOException,
        wasCanceled: Boolean
    ) {
        // Called when a media load encounters an error
        Log.e(TAG, "Load error: ${loadEventInfo.describeWithError(error, wasCanceled)}")
    }

    companion object {
        private const val TAG = "ArmListener"
    }
}

fun LoadEventInfo.describe(): String {
    return "${dataSpec.uri} pos: ${dataSpec.position}, len: ${dataSpec.length}"
}

fun LoadEventInfo.describeWithError(exception: Exception, wasCanceled: Boolean): String {
    return "ERR: ${exception.message}: wasCanceled: $wasCanceled, ${dataSpec.uri} pos: ${dataSpec.position}, len: ${dataSpec.length}"
}
