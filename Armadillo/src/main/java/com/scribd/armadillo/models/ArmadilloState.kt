package com.scribd.armadillo.models

import com.google.android.exoplayer2.C
import com.scribd.armadillo.ArmadilloDebugView
import com.scribd.armadillo.ArmadilloPlayer
import com.scribd.armadillo.Constants
import com.scribd.armadillo.Milliseconds
import com.scribd.armadillo.actions.Action
import com.scribd.armadillo.error.ArmadilloException
import com.scribd.armadillo.error.UnexpectedException
import com.scribd.armadillo.time.milliseconds
import java.util.Arrays

data class ArmadilloState(
    val playbackInfo: PlaybackInfo? = null,
    val downloadInfo: List<DownloadProgressInfo>,
    val drmPlaybackState: DrmState = DrmState.NoDRM,
    val internalState: InternalState = InternalState(),
    val error: ArmadilloException? = null) {

    // only properties in the constructor will be included inside the toString(), equals(), hashCode(), and copy() implementations
    // debug must be handled separately
    var debugState: ArmadilloDebugState = ArmadilloDebugState()

    internal fun downloadInfoForUrl(url: String?): DownloadProgressInfo? = downloadInfo.find { it.url == url }

    /**
     * Provides the position within a chapter based on [percent]
     */
    internal fun positionFromChapterPercent(percent: Int): Milliseconds? {
        if (playbackInfo == null) {
            return null
        }
        if (percent > 100 || percent < 0) {
            throw UnexpectedException(cause = IllegalArgumentException("Invalid argument: $percent"),
                actionThatFailedMessage = "Calculating seekbar progress from percentage.")
        }
        val currentChapter = playbackInfo.audioPlayable.chapters[playbackInfo.progress.currentChapterIndex]
        val decimal = percent / 100.toDouble()
        val durationWithinChapter = (currentChapter.duration.value * decimal).milliseconds
        return currentChapter.startTime + durationWithinChapter
    }
}

data class PlaybackInfo(
    val audioPlayable: AudioPlayable,
    val playbackState: PlaybackState,
    val progress: PlaybackProgress,
    val controlState: MediaControlState,
    val playbackSpeed: Float,
    val skipDistance: Milliseconds,
    val isLoading: Boolean)

enum class PlaybackState {
    PLAYING, PAUSED, NONE
}

/**
 * Playback Progress
 *
 * @param currentChapterIndex The index of the chapter currently being played
 * @param positionInDuration Global position relative to the start of the content
 * @param totalPlayerDuration The total duration of content as calculated by the player engine. Will be null if this is not yet known
 * @param totalChaptersDuration The total duration of content as calculated based on the provided chapter metadata
 */
data class PlaybackProgress(
    val currentChapterIndex: Int = 0,
    val positionInDuration: Milliseconds = 0.milliseconds,
    val totalPlayerDuration: Milliseconds? = null,
    val totalChaptersDuration: Milliseconds)

/**
 * Long lived Media Control commands being processed.
 */
data class MediaControlState(
    val isStartingNewAudioPlayable: Boolean = false,
    val isStopping: Boolean = false,
    val isFastForwarding: Boolean = false,
    val isRewinding: Boolean = false,
    val isNextChapter: Boolean = false,
    val isPrevChapter: Boolean = false,
    val isCustomAction: Boolean = false,
    val isSeeking: Boolean = false,
    /** Has the player reached the end of content */
    val hasContentEnded: Boolean = false,
    val seekTarget: Milliseconds? = 0.milliseconds,
    /**  Progress updates for the PlaybackService */
    val isPlaybackStateUpdating: Boolean = false,
    val updatedChapterIndex: Int = 0,
    val customMediaActionName: String? = null)

/**
 * Download progress for an audioPlayable
 * [id] The [AudioPlayable.id] of this task
 * [url] The url identifier for the audioPlayable
 * [downloadState] Current status of the download
 */
data class DownloadProgressInfo(
    val id: Int,
    val url: String,
    val downloadState: DownloadState,
    val exoPlayerDownloadState: Int? = null) {

    fun isDownloaded(): Boolean = DownloadState.COMPLETED == downloadState

    fun isFailed(): Boolean = downloadState is DownloadState.FAILED

    companion object {
        const val PROGRESS_UNSET = C.PERCENTAGE_UNSET
    }
}

sealed class DownloadState {
    /**
     * Progress for a download
     */
    data class STARTED(val percentComplete: Int, val downloadedBytes: Long) : DownloadState()

    /**
     * When a download is complete
     */
    object COMPLETED : DownloadState() {
        override fun toString() = "COMPLETED"
    }

    /**
     * When a download removal is complete
     */
    object REMOVED : DownloadState() {
        override fun toString() = "REMOVED"
    }

    data class FAILED(val failureReason: Int? = null) : DownloadState() {
        override fun toString() = "FAILED"
    }
}

data class InternalState(val isPlaybackEngineReady: Boolean = false)

sealed class DrmState(val drmType: DrmType?, val expireMillis: Milliseconds, val isSessionValid: Boolean) {
    /** This Content is not utilizing DRM protections, or is now first initializing **/
    object NoDRM : DrmState(null, 0.milliseconds, true)

    /** Attempt to open the license and decrypt */
    class LicenseOpening(drmType: DrmType?, expireMillis: Milliseconds = 0.milliseconds) : DrmState(drmType, expireMillis, true)

    /** A DRM License has been obtained. */
    class LicenseAcquired(drmType: DrmType, expireMillis: Milliseconds) : DrmState(drmType, expireMillis, true)

    /** The player encountered an expiration event */
    class LicenseExpired(drmType: DrmType?, expireMillis: Milliseconds) : DrmState(drmType, expireMillis, false)

    /** A DRM license exists and content is able to be decrypted. */
    class LicenseUsable(drmType: DrmType?, expireMillis: Milliseconds) : DrmState(drmType, expireMillis, true)

    /** The content with the previously retrieved license has been released. */
    class LicenseReleased(drmType: DrmType?, expireMillis: Milliseconds = 0.milliseconds, isSessionValid: Boolean)
        : DrmState(drmType, expireMillis, isSessionValid)

    /** An error occurred for the DRM license. This might not affect playback; see [ArmadilloState.error] for playback issues. */
    class LicenseError(drmType: DrmType?, expireMillis: Milliseconds) : DrmState(drmType, expireMillis, false)
}

/**
 * Debugging information for [ArmadilloState]
 * [appStateUpdateCount] is the number of times the [ArmadilloPlayer] has provided an updated state to observers
 * [actionHistory] reverse chronological list of actions dispatched to the [ArmadilloPlayer]
 */
data class ArmadilloDebugState(var appStateUpdateCount: Int = 0,
                               val actionHistory: Array<Action> = emptyArray()) {
    fun getActionHistoryDisplayString(): String {
        return actionHistory.joinToString(separator = ", ") { it.name }
    }

    // Auto-generated equals and hashCode. IDE recommends manually implementing equals for data classes with array values
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ArmadilloDebugState

        if (appStateUpdateCount != other.appStateUpdateCount) return false
        if (!Arrays.equals(actionHistory, other.actionHistory)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = appStateUpdateCount
        result = 31 * result + Arrays.hashCode(actionHistory)
        return result
    }
}

/**
 * Debug Tool for [ArmadilloDebugView]:
 * [addNewAction] all actions dispatched to the [ArmadilloPlayer]
 */
internal fun ArmadilloState.addNewAction(action: Action): ArmadilloDebugState {
    val newActionsArr = debugState.actionHistory.copyOf().toMutableList()
    newActionsArr.add(0, action)
    return debugState.copy(actionHistory = newActionsArr.takeLast(Constants.DEBUG_MAX_SIZE).toTypedArray())
}
