package com.scribd.armadillo.actions

import com.scribd.armadillo.Milliseconds
import com.scribd.armadillo.error.ArmadilloException
import com.scribd.armadillo.models.ArmadilloState
import com.scribd.armadillo.models.AudioPlayable
import com.scribd.armadillo.models.Chapter
import com.scribd.armadillo.models.DownloadProgressInfo
import com.scribd.armadillo.models.DrmType
import com.scribd.armadillo.models.PlaybackState

// Exoplayer State Updates

internal data class PlayerStateAction(val playerState: PlaybackState) : Action {
    override val name = "PlayerStateAction($this)"
}

internal data class PlaybackSpeedAction(val playbackSpeed: Float) : Action {
    override val name: String
        get() = "PlaybackSpeedAction($this)"
}

internal data class SkipDistanceAction(val skipDistance: Milliseconds) : Action {
    override val name: String = "SkipDistanceAction($this)"
}

/**
 * Updates Playback Progress
 */
internal data class PlaybackProgressAction(val currentPosition: Milliseconds, val playerDuration: Milliseconds?) : Action {
    override val name = "PlaybackProgressAction"
}

/**
 * Adds or updates a download for tracking
 */
internal data class UpdateDownloadAction(val downloadProgressInfo: DownloadProgressInfo) : Action {
    override val name = "UpdateDownloadAction"
}

/**
 * This action should be sent when it is no longer relevant for [ArmadilloState] to track this download
 * (i.e. because it has been removed or completed)”
 */
internal data class StopTrackingDownloadAction(val downloadProgressInfo: DownloadProgressInfo) : Action {
    override val name = "StopTrackingDownloadAction"
}

internal data class LoadingAction(val isLoading: Boolean) : Action {
    override val name = "Loading: $isLoading"
}

/** used to regularly update listening progress of the audio - set isUpdated true to flag an update and false to mark it consumed */
internal data class UpdateProgressAction(val isUpdated: Boolean, val currentChapterIndex: Int = -1) : Action {
    override val name = "UpdateProgressAction: updating = $isUpdated"
}

// Media Control State updates - updates control info in ArmadilloState / exposes player intent to users

/** For a beginning or end of a seek (discontinuity). when isSeeking is becoming false, the seek action is resolved. */
internal data class SeekAction(val isSeeking: Boolean, val seekPositionTarget: Milliseconds?) : Action {
    override val name = "Seeking: $isSeeking"
}

internal data class FastForwardAction(val seekPositionTarget: Milliseconds?) : Action {
    override val name = "FastForwardAction: $seekPositionTarget"
}

internal data class RewindAction(val seekPositionTarget: Milliseconds?) : Action {
    override val name = "RewindAction: $seekPositionTarget"
}

internal data class SkipNextAction(val seekPositionTarget: Milliseconds?) : Action {
    override val name = "SkipNextAction: $seekPositionTarget"
}

internal data class SkipPrevAction(val seekPositionTarget: Milliseconds?) : Action {
    override val name = "SkipNextAction: $seekPositionTarget"
}

internal data class CustomMediaSessionAction(val actionName: String) : Action {
    override val name = "Custom Media Session Action: $actionName"
}

internal data class PlaybackEngineReady(val isReady: Boolean) : Action {
    override val name = "PlaybackEngineReady: $isReady"
}

internal data class NewAudioPlayableAction(val audioPlayable: AudioPlayable,
                                           val maxDurationDiscrepancy: Int,
                                           val initialOffset: Milliseconds) : Action {
    override val name = "Audiobook: $audioPlayable"
}

internal data class MetadataUpdateAction(val title: String, val chapters: List<Chapter>) : Action {
    override val name: String = "MetadataUpdateAction: $title"
}

internal data class MediaRequestUpdateAction(val mediaRequest: AudioPlayable.MediaRequest): Action {
    override val name: String = "MediaRequestUpdateAction: $mediaRequest"
}

internal object ContentEndedAction : Action {
    override val name = "ContentEndedAction"
}

internal data class OpeningLicenseAction(val type: DrmType?) : Action {
    override val name: String = "OpeningLicenseAction"
}

internal data class LicenseAcquiredAction(val type: DrmType) : Action {
    override val name = "LicenseAcquiredAction"
}

internal data class LicenseExpirationDetermined(val expirationMilliseconds: Milliseconds) : Action {
    override val name: String
        get() = "LicenseExpirationDetermined"
}

/** session can be recovering or resuming */
internal object LicenseKeyIsUsableAction : Action {
    override val name: String = "LicenseKeyUsableAction"
}

internal object LicenseExpiredAction : Action {
    override val name: String = "LicenseExpiredAction"
}

internal object LicenseReleasedAction : Action {
    override val name = "LicenseReleasedAction"
}

internal object LicenseDrmErrorAction : Action {
    override val name: String  = "LicenseDrmErrorAction"
}

// Errors

internal data class ErrorAction(val error: ArmadilloException) : Action {
    override val name: String = "Error"
}

internal object ClearErrorAction : Action {
    override val name: String = "ClearErrorActions"
}

interface Action {
    val name: String
}
