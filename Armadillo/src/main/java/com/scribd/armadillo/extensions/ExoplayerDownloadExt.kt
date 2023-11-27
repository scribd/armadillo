package com.scribd.armadillo.extensions

import androidx.media3.common.C
import com.scribd.armadillo.ExoplayerDownload

/**
 * Exoplayer can be mysterious at times. This method is designed for making a more printable version of [ExoplayerDownload] for debugging
 * purposes
 */
fun ExoplayerDownload.toPrint(): String {

    val stateStr = when (state) {
        ExoplayerDownload.STATE_QUEUED -> "Queued"
        ExoplayerDownload.STATE_STOPPED -> "Stopped"
        ExoplayerDownload.STATE_DOWNLOADING -> "Downloading"
        ExoplayerDownload.STATE_COMPLETED -> "Completed"
        ExoplayerDownload.STATE_FAILED -> "Failed"
        ExoplayerDownload.STATE_REMOVING -> "Removing"
        ExoplayerDownload.STATE_RESTARTING -> "Restarting"
        else -> "Unknown"
    }

    val failureReasonStr = when (failureReason) {
        ExoplayerDownload.FAILURE_REASON_NONE -> null // The download isn't failed
        else -> "unknown"
    }

    val percentDownloadedStr = when {
        percentDownloaded.toInt() == C.PERCENTAGE_UNSET -> "Unset"
        else -> percentDownloaded.toString()
    }

    val bytesStr = when {
        bytesDownloaded > 0 -> bytesDownloaded.toString()
        else -> null
    }

    var str = "State: $stateStr"
    if (failureReasonStr != null) {
        str += "  - FailureReason: $failureReasonStr"
    }

    str += "  - percentDownloaded: $percentDownloadedStr"
    str += "  - bytes: $bytesStr"
    return  str
}