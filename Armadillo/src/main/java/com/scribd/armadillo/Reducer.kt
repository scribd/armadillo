package com.scribd.armadillo

import android.util.Log
import androidx.annotation.VisibleForTesting
import com.scribd.armadillo.actions.Action
import com.scribd.armadillo.actions.ClearErrorAction
import com.scribd.armadillo.actions.ContentEndedAction
import com.scribd.armadillo.actions.CustomMediaSessionAction
import com.scribd.armadillo.actions.ErrorAction
import com.scribd.armadillo.actions.FastForwardAction
import com.scribd.armadillo.actions.LoadingAction
import com.scribd.armadillo.actions.MetadataUpdateAction
import com.scribd.armadillo.actions.NewAudioPlayableAction
import com.scribd.armadillo.actions.PlaybackEngineReady
import com.scribd.armadillo.actions.PlaybackProgressAction
import com.scribd.armadillo.actions.PlaybackSpeedAction
import com.scribd.armadillo.actions.PlayerStateAction
import com.scribd.armadillo.actions.RewindAction
import com.scribd.armadillo.actions.SeekAction
import com.scribd.armadillo.actions.SkipDistanceAction
import com.scribd.armadillo.actions.SkipNextAction
import com.scribd.armadillo.actions.SkipPrevAction
import com.scribd.armadillo.actions.StopTrackingDownloadAction
import com.scribd.armadillo.actions.UpdateDownloadAction
import com.scribd.armadillo.actions.UpdateProgressAction
import com.scribd.armadillo.error.ActionBeforeSetup
import com.scribd.armadillo.error.ArmadilloException
import com.scribd.armadillo.error.IncorrectChapterMetadataException
import com.scribd.armadillo.error.UnrecognizedAction
import com.scribd.armadillo.extensions.filterOutCompletedItems
import com.scribd.armadillo.extensions.removeItemsByUrl
import com.scribd.armadillo.extensions.replaceDownloadProgressItemsByUrl
import com.scribd.armadillo.models.ArmadilloState
import com.scribd.armadillo.models.MediaControlState
import com.scribd.armadillo.models.PlaybackInfo
import com.scribd.armadillo.models.PlaybackProgress
import com.scribd.armadillo.models.PlaybackState
import com.scribd.armadillo.models.addNewAction
import com.scribd.armadillo.time.Interval
import com.scribd.armadillo.time.Millisecond

/**
 * Creates a new immutable [ArmadilloState]. The [Reducer] is the only way state should ever be changed.
 */
internal object Reducer {
    const val TAG = "Reducer"
    /**
     * Due to floating point math, adding up the duration of the each chapter to determine the audioPlayable duration will result in some
     * level of inaccuracy. Generally, we seek to have the duration be slightly shorter then the actual playlist length. This value is
     * the allowable discrepancy between calculated duration and actual duration of the playlist.
     *
     * Can be set to a negative value to disable errors when a discrepancy is found.
     */
    private var maxDurationDiscrepancy = ArmadilloConfiguration.MAX_DISCREPANCY_DEFAULT

    fun reduce(oldState: ArmadilloState, action: Action): ArmadilloState {
        val newDebug = oldState.addNewAction(action)
        return when (action) {
            is PlaybackSpeedAction -> {
                val playbackInfo = oldState.playbackInfo ?: throw ActionBeforeSetup(action)
                oldState.copy(
                        playbackInfo = playbackInfo.copy(
                                playbackSpeed = action.playbackSpeed
                        ))
                        .apply { debugState = newDebug }
            }
            is PlayerStateAction -> {
                val playbackInfo = oldState.playbackInfo ?: throw ActionBeforeSetup(action)
                val controlState = if (action.playerState == PlaybackState.NONE) {
                    MediaControlState(isStopping = true)
                } else playbackInfo.controlState.copy(isStartingNewAudioPlayable = false)
                oldState.copy(
                        playbackInfo = playbackInfo.copy(
                                controlState = controlState,
                                playbackState = action.playerState
                        ))
                        .apply { debugState = newDebug }
            }
            is PlaybackProgressAction -> {
                val playbackInfo = oldState.playbackInfo ?: throw ActionBeforeSetup(action)
                oldState.copy(playbackInfo = playbackInfo.copy(progress = playbackInfo.progress.copy(
                    currentChapterIndex = playbackInfo.audioPlayable.getChapterIndexAtOffset(action.currentPosition),
                    positionInDuration = action.currentPosition,
                    totalPlayerDuration = action.playerDuration
                )))
                .apply { debugState = newDebug }
            }

            is ErrorAction -> {
                oldState.copy(error = action.error)
                        .apply { debugState = newDebug }
            }

            is LoadingAction -> {
                val playbackInfo = oldState.playbackInfo ?: throw ActionBeforeSetup(action)
                oldState.copy(playbackInfo = playbackInfo.copy(
                        isLoading = action.isLoading))
                        .apply { debugState = newDebug }
            }

            is UpdateDownloadAction -> {
                val newDownloadInfo = action.downloadProgressInfo
                val oldDownloadInfo = oldState.downloadInfo.filterOutCompletedItems()
                val updatedList = oldDownloadInfo.replaceDownloadProgressItemsByUrl(listOf(newDownloadInfo))
                oldState.copy(downloadInfo = updatedList)
                        .apply { debugState = newDebug }
            }

            is StopTrackingDownloadAction -> {
                val newDownloadInfo = action.downloadProgressInfo
                val updatedList = oldState.downloadInfo.removeItemsByUrl(listOf(newDownloadInfo))
                oldState.copy(downloadInfo = updatedList)
                        .apply { debugState = newDebug }
            }
            is SkipDistanceAction -> {
                val playbackInfo = oldState.playbackInfo ?: throw ActionBeforeSetup(action)
                oldState.copy(playbackInfo = playbackInfo.copy(
                        skipDistance = action.skipDistance
                ))
                        .apply { debugState = newDebug }
            }
            is PlaybackEngineReady -> {
                val internalStateInfo = oldState.internalState
                oldState.copy(
                        internalState = internalStateInfo.copy(
                                isPlaybackEngineReady = action.isReady
                        ))
                        .apply { debugState = newDebug }
            }
            is NewAudioPlayableAction -> {
                maxDurationDiscrepancy = action.maxDurationDiscrepancy
                oldState.copy(playbackInfo = PlaybackInfo(
                        audioPlayable = action.audioPlayable,
                        playbackState = PlaybackState.NONE,
                        progress = PlaybackProgress(
                                positionInDuration = action.initialOffset,
                                totalChaptersDuration = action.audioPlayable.duration),
                        playbackSpeed = Constants.DEFAULT_PLAYBACK_SPEED,
                        controlState = MediaControlState(isStartingNewAudioPlayable = true),
                        skipDistance = oldState.playbackInfo?.skipDistance ?: Constants.AUDIO_SKIP_DURATION,
                        isLoading = true))
                        .apply { debugState = newDebug }
            }
            is MetadataUpdateAction -> {
                oldState.copy(
                    playbackInfo = oldState.playbackInfo?.let {
                        it.copy(
                            progress = it.progress.copy(totalChaptersDuration = action.chapters.last().endTime),
                            audioPlayable = it.audioPlayable.copy(title = action.title, chapters = action.chapters)
                        )
                    }
                )
                .apply { debugState = newDebug }
            }
            is SeekAction -> {
                val playbackInfo = oldState.playbackInfo ?: throw ActionBeforeSetup(action)
                //clear control inputs once loading ends, otherwise keep it
                val controlState = if (action.isSeeking) {
                    MediaControlState(
                            isSeeking = action.isSeeking,
                            seekTarget = action.seekPositionTarget)
                } else MediaControlState()

                oldState.copy(playbackInfo = playbackInfo.copy(
                        controlState = controlState))
                        .apply { debugState = newDebug }
            }
            is FastForwardAction -> {
                val playbackInfo = oldState.playbackInfo ?: throw ActionBeforeSetup(action)
                oldState.copy(playbackInfo = playbackInfo.copy(
                        controlState = MediaControlState(
                                isSeeking = true,
                                isFastForwarding = true,
                                seekTarget = action.seekPositionTarget)
                )).apply { debugState = newDebug }
            }
            is RewindAction -> {
                val playbackInfo = oldState.playbackInfo ?: throw ActionBeforeSetup(action)
                oldState.copy(playbackInfo = playbackInfo.copy(
                        controlState = MediaControlState(
                                isSeeking = true,
                                isRewinding = true,
                                seekTarget = action.seekPositionTarget)
                )).apply { debugState = newDebug }
            }
            is SkipNextAction -> {
                val playbackInfo = oldState.playbackInfo ?: throw ActionBeforeSetup(action)
                oldState.copy(playbackInfo = playbackInfo.copy(
                        controlState = MediaControlState(
                                isSeeking = true,
                                isNextChapter = true,
                                seekTarget = action.seekPositionTarget)
                )).apply { debugState = newDebug }
            }
            is SkipPrevAction -> {
                val playbackInfo = oldState.playbackInfo ?: throw ActionBeforeSetup(action)
                oldState.copy(playbackInfo = playbackInfo.copy(
                        controlState = MediaControlState(
                                isSeeking = true,
                                isPrevChapter = true,
                                seekTarget = action.seekPositionTarget)
                )).apply { debugState = newDebug }
            }
            is CustomMediaSessionAction -> {
                val playbackInfo = oldState.playbackInfo ?: throw ActionBeforeSetup(action)
                val newControlState = playbackInfo.controlState.copy(
                        customMediaActionName = action.actionName)
                oldState.copy(playbackInfo = playbackInfo.copy(
                        controlState = newControlState))
                        .apply { debugState = newDebug }
            }
            is UpdateProgressAction -> {
                val playbackInfo = oldState.playbackInfo ?: throw ActionBeforeSetup(action)
                val chapterIndex = if (action.currentChapterIndex >= 0) {
                    action.currentChapterIndex
                } else {
                    playbackInfo.controlState.updatedChapterIndex
                }
                val newControlState = playbackInfo.controlState.copy(
                        isPlaybackStateUpdating = action.isUpdated,
                        updatedChapterIndex = chapterIndex)
                /**
                 * We dispatch an error when a progress update is received that is beyond the bounds of the audioPlayable. When this happens,
                 * chapter metadata indicates that the content is shorter then it is.
                 */
                val error = verifyProgressIsNotBeyondDuration(playbackInfo.progress)
                oldState.copy(
                        playbackInfo = playbackInfo.copy(controlState = newControlState),
                        error = error).apply { debugState = newDebug }
            }
            is ClearErrorAction -> {
                oldState.copy(error = null)
                        .apply { debugState = newDebug }
            }
            /**
             * The playback engine will dispatch this action when it reaches the end of the content. If the metadata indicates that there
             * is more content to be played, we should alert the client of an error.
             */
            is ContentEndedAction -> {
                val playbackProgress = oldState.playbackInfo?.progress ?: throw ActionBeforeSetup(action)
                val error = verifyAudiobookIsComplete(playbackProgress)
                return oldState
                        .copy(error = error,
                            playbackInfo = oldState.playbackInfo.copy(
                                controlState = MediaControlState(hasContentEnded = true)
                        ))
                        .apply { debugState = newDebug }
            }
            else -> throw UnrecognizedAction(action)
        }
    }

    @VisibleForTesting
    fun verifyProgressIsNotBeyondDuration(playbackProgress: PlaybackProgress): ArmadilloException? {
        val currentPosition = playbackProgress.positionInDuration
        val totalDuration = playbackProgress.totalChaptersDuration
        return checkBounds { currentPosition - totalDuration }
    }

    @VisibleForTesting
    fun verifyAudiobookIsComplete(playbackProgress: PlaybackProgress): ArmadilloException? {
        val currentPosition = playbackProgress.positionInDuration
        val totalDuration = playbackProgress.totalChaptersDuration
        return checkBounds { totalDuration - currentPosition }
    }

    private fun checkBounds(timeToCheck: () -> Interval<Millisecond>): ArmadilloException? {
        return if (maxDurationDiscrepancy >= 0 && timeToCheck.invoke().inSeconds.longValue > maxDurationDiscrepancy) {
            Log.e(TAG, "Content metadata is incorrect")
            IncorrectChapterMetadataException
        } else {
            null
        }
    }
}
