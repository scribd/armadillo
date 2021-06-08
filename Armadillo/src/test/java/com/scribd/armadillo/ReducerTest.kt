package com.scribd.armadillo

import com.scribd.armadillo.actions.Action
import com.scribd.armadillo.actions.ContentEndedAction
import com.scribd.armadillo.actions.FastForwardAction
import com.scribd.armadillo.actions.MetadataUpdateAction
import com.scribd.armadillo.actions.NewAudioPlayableAction
import com.scribd.armadillo.actions.PlaybackSpeedAction
import com.scribd.armadillo.actions.PlayerStateAction
import com.scribd.armadillo.actions.RewindAction
import com.scribd.armadillo.actions.SeekAction
import com.scribd.armadillo.actions.SkipDistanceAction
import com.scribd.armadillo.actions.SkipNextAction
import com.scribd.armadillo.actions.SkipPrevAction
import com.scribd.armadillo.actions.UpdateDownloadAction
import com.scribd.armadillo.error.IncorrectChapterMetadataException
import com.scribd.armadillo.models.Chapter
import com.scribd.armadillo.models.DownloadState
import com.scribd.armadillo.models.PlaybackProgress
import com.scribd.armadillo.models.PlaybackState
import com.scribd.armadillo.time.milliseconds
import com.scribd.armadillo.time.minutes
import com.scribd.armadillo.time.seconds
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class ReducerTest {
    private companion object {
        const val PLAYBACK_SPEED = 1.2f
    }

    @Test
    fun reduce_isPlaying() {
        val newState = Reducer.reduce(MockModels.appState(), PlayerStateAction(PlaybackState.PLAYING))
        assertThat(newState.playbackInfo!!.playbackState).isEqualTo(PlaybackState.PLAYING)
    }

    @Test
    fun reduce_isPaused() {
        val newState = Reducer.reduce(MockModels.appState(), PlayerStateAction(PlaybackState.PAUSED))
        assertThat(newState.playbackInfo!!.playbackState).isEqualTo(PlaybackState.PAUSED)
    }

    @Test
    fun reduce_playbackSpeed() {
        val appState = MockModels.appState()
        assertThat(appState.playbackInfo!!.playbackSpeed).isEqualTo(1.0f)
        val newState = Reducer.reduce(appState, PlaybackSpeedAction(PLAYBACK_SPEED))
        assertThat(newState.playbackInfo!!.playbackSpeed).isEqualTo(PLAYBACK_SPEED)
    }

    @Test
    fun reduce_NewAudiobookAction() {
        val newState = Reducer.reduce(MockModels.appState(),
            NewAudioPlayableAction(MockModels.audiobook(),
            ArmadilloConfiguration.MAX_DISCREPANCY_DEFAULT, 0.milliseconds))
        val newPlaybackInfo = newState.playbackInfo!!
        assertThat(newPlaybackInfo.audioPlayable).isEqualTo(MockModels.audiobook())
        assertThat(newPlaybackInfo.playbackState).isEqualTo(PlaybackState.NONE)
        assertThat(newPlaybackInfo.progress).isEqualTo(PlaybackProgress(
            currentChapterIndex = 0,
            positionInDuration = 0.milliseconds,
            totalChaptersDuration = MockModels.audiobook().duration))
        assertThat(newPlaybackInfo.playbackSpeed).isEqualTo(Constants.DEFAULT_PLAYBACK_SPEED)
        assertThat(newPlaybackInfo.isLoading).isTrue()
        assertThat(newPlaybackInfo.controlState.isStartingNewAudioPlayable).isTrue()
        assertThat(newPlaybackInfo.controlState.isStopping).isFalse()
    }

    @Test
    fun reduce_metadataUpdateAction() {
        val initialState = MockModels.appState()

        val newTitle = "New Title"
        val newChapters = listOf(
            Chapter("Updated Chapter 0", 0, 0, 0.milliseconds, 100.milliseconds),
            Chapter("Updated Chapter 1", 0, 1, 100.milliseconds, 500.milliseconds)
        )
        val action = MetadataUpdateAction(newTitle, newChapters)
        val newState = Reducer.reduce(initialState, action)

        val newPlayable = newState.playbackInfo!!.audioPlayable
        assertThat(newPlayable.chapters).isEqualTo(newChapters)
        assertThat(newPlayable.title).isEqualTo(newTitle)
        assertThat(newPlayable.id).isEqualTo(initialState.playbackInfo!!.audioPlayable.id)
    }

    @Test
    fun reduce_changeSkipDistance() {
        val targetDistance = 59.seconds.inMilliseconds
        val newState = Reducer.reduce(MockModels.appState(), SkipDistanceAction(targetDistance))
        val newPlaybackInfo = newState.playbackInfo

        assertThat(newPlaybackInfo?.skipDistance).isEqualTo(targetDistance)
    }

    @Test
    fun reduce_identicalSequentialActions_appStatesAreEqualAndDebugStateNotEqual() {
        val appState1 = Reducer.reduce(MockModels.appState(), MockModels.progressAction())

        val identicalActions = listOf(MockModels.progressAction(), MockModels.progressAction())
        var appState2 = MockModels.appState()
        identicalActions.forEach {
            appState2 = Reducer.reduce(appState2, it)
        }

        assertThat(appState1).isEqualTo(appState2)

        assertThat(appState1.debugState).isNotEqualTo(appState2.debugState)
    }

    @Test
    fun reduce_actionsApplied_actionHistoryUpdates() {
        var appState = MockModels.appState()
        val actions: List<Action> = listOf(
            PlayerStateAction(PlaybackState.PLAYING),
            MockModels.progressAction(),
            MockModels.progressAction(),
            MockModels.progressAction())
        actions.forEach {
            appState = Reducer.reduce(appState, it)
        }
        val actionHistory = appState.debugState.actionHistory

        assertThat(actionHistory.size).isEqualTo(4)
        assertThat(actionHistory[0]).isEqualTo(MockModels.progressAction())
        assertThat(actionHistory[1]).isEqualTo(MockModels.progressAction())
        assertThat(actionHistory[2]).isEqualTo(MockModels.progressAction())
        assertThat(actionHistory[3]).isEqualTo(PlayerStateAction(PlaybackState.PLAYING))
    }

    @Test
    fun reduce_identicalProgressActions_appStatesAreEqualAndDebugStateNotEqual() {
        val appState1 = Reducer.reduce(MockModels.appState(), MockModels.progressAction())

        val identicalActions = listOf(MockModels.progressAction(), MockModels.progressAction())
        var appState2 = MockModels.appState()
        identicalActions.forEach {
            appState2 = Reducer.reduce(appState2, it)
        }

        assertThat(appState1).isEqualTo(appState2)
        assertThat(appState1.debugState).isNotEqualTo(appState2.debugState)
    }

    @Test
    fun reduce_updateDownloadAction_addsAction() {
        val appState = Reducer.reduce(MockModels.appState(), MockModels.updateDownloadAction())
        assertThat(appState.downloadInfo.size).isEqualTo(1)
    }

    @Test
    fun reduce_updateDownloadAction_updatesExistingAction() {
        val appState = Reducer.reduce(MockModels.appState(), MockModels.updateDownloadAction())
        val newDownloadPercent = 70
        val newDownloadedBytes = 300L
        val newDownloadInfo = MockModels.downloadInfo().copy(downloadState = DownloadState.STARTED(newDownloadPercent, newDownloadedBytes))

        val newAppState = Reducer.reduce(appState, UpdateDownloadAction(newDownloadInfo))

        val downloadState = newAppState.downloadInfo[0].downloadState as DownloadState.STARTED
        assertThat(downloadState.percentComplete).isEqualTo(newDownloadPercent)
        assertThat(downloadState.downloadedBytes).isEqualTo(newDownloadedBytes)
    }

    @Test
    fun reduce_updateDownloadAction_updatesAudiobookToBeAvailableOffline() {
        val appState = Reducer.reduce(MockModels.appState(), MockModels.updateDownloadAction())
        val url = appState.playbackInfo!!.audioPlayable.request.url
        val downloadInfoBefore = appState.downloadInfoForUrl(url)
        assertThat(downloadInfoBefore?.isDownloaded()).isFalse()

        val newDownloadInfo = MockModels.downloadInfo().copy(downloadState = DownloadState.COMPLETED)

        val newAppState = Reducer.reduce(appState, UpdateDownloadAction(newDownloadInfo))
        val downloadInfoAfter = newAppState.downloadInfoForUrl(url)
        assertThat(downloadInfoAfter!!.isDownloaded()).isTrue()
    }

    @Test
    fun reduce_skipNext_clearsOtherSeekControls() {
        Reducer.reduce(MockModels.appState(), SkipPrevAction(100.milliseconds))
        val secondSeek = Reducer.reduce(MockModels.appState(), SkipNextAction(4000.milliseconds))

        assertThat(secondSeek.playbackInfo?.controlState?.isFastForwarding ?: true).isFalse()
        assertThat(secondSeek.playbackInfo?.controlState?.isRewinding ?: true).isFalse()
        assertThat(secondSeek.playbackInfo?.controlState?.isPrevChapter ?: true).isFalse()
        assertThat(secondSeek.playbackInfo?.controlState?.isNextChapter).isTrue()
        assertThat(secondSeek.playbackInfo?.controlState?.isSeeking).isTrue()
    }

    @Test
    fun reduce_skipPrev_clearsOtherSeekControls() {
        Reducer.reduce(MockModels.appState(), SkipNextAction(100.milliseconds))
        val secondSeek = Reducer.reduce(MockModels.appState(), SkipPrevAction(4000.milliseconds))

        assertThat(secondSeek.playbackInfo?.controlState?.isFastForwarding ?: true).isFalse()
        assertThat(secondSeek.playbackInfo?.controlState?.isRewinding ?: true).isFalse()
        assertThat(secondSeek.playbackInfo?.controlState?.isNextChapter ?: true).isFalse()
        assertThat(secondSeek.playbackInfo?.controlState?.isPrevChapter).isTrue()
        assertThat(secondSeek.playbackInfo?.controlState?.isSeeking).isTrue()
    }

    @Test
    fun reduce_fastForward_clearsOtherSeekControls() {
        Reducer.reduce(MockModels.appState(), RewindAction(100.milliseconds))
        val secondSeek = Reducer.reduce(MockModels.appState(), FastForwardAction(4000.milliseconds))

        assertThat(secondSeek.playbackInfo?.controlState?.isPrevChapter ?: true).isFalse()
        assertThat(secondSeek.playbackInfo?.controlState?.isRewinding ?: true).isFalse()
        assertThat(secondSeek.playbackInfo?.controlState?.isNextChapter ?: true).isFalse()
        assertThat(secondSeek.playbackInfo?.controlState?.isFastForwarding).isTrue()
        assertThat(secondSeek.playbackInfo?.controlState?.isSeeking).isTrue()
    }

    @Test
    fun reduce_rewind_clearsOtherSeekControls() {
        Reducer.reduce(MockModels.appState(), FastForwardAction(100.milliseconds))
        val secondSeek = Reducer.reduce(MockModels.appState(), RewindAction(4000.milliseconds))

        assertThat(secondSeek.playbackInfo?.controlState?.isPrevChapter ?: true).isFalse()
        assertThat(secondSeek.playbackInfo?.controlState?.isFastForwarding ?: true).isFalse()
        assertThat(secondSeek.playbackInfo?.controlState?.isNextChapter ?: true).isFalse()
        assertThat(secondSeek.playbackInfo?.controlState?.isRewinding).isTrue()
        assertThat(secondSeek.playbackInfo?.controlState?.isSeeking).isTrue()
    }

    @Test
    fun reduce_seekFinish_allSeekControlsFalse() {
        Reducer.reduce(MockModels.appState(), FastForwardAction(100.milliseconds))
        val secondSeek = Reducer.reduce(MockModels.appState(), SeekAction(false, null))

        assertThat(secondSeek.playbackInfo?.controlState?.isFastForwarding ?: true).isFalse()
        assertThat(secondSeek.playbackInfo?.controlState?.isRewinding ?: true).isFalse()
        assertThat(secondSeek.playbackInfo?.controlState?.isPrevChapter ?: true).isFalse()
        assertThat(secondSeek.playbackInfo?.controlState?.isNextChapter ?: true).isFalse()
        assertThat(secondSeek.playbackInfo?.controlState?.isSeeking).isFalse()
    }

    @Test
    fun reduce_playbackStopDuringSeek_clearSeekControls() {
        Reducer.reduce(MockModels.appState(), FastForwardAction(100000.milliseconds))
        val state = Reducer.reduce(MockModels.appState(), PlayerStateAction(PlaybackState.NONE))

        assertThat(state.playbackInfo?.controlState?.isStopping).isTrue()
        assertThat(state.playbackInfo?.controlState?.isFastForwarding ?: true).isFalse()
    }

    @Test
    fun reduce_positionIsLessThenDuration_noError() {
        val progress = PlaybackProgress(
            positionInDuration = 0.seconds.inMilliseconds,
            totalChaptersDuration = 5.minutes.inMilliseconds)
        val error = Reducer.verifyProgressIsNotBeyondDuration(progress)
        assertThat(error).isNull()
    }

    @Test
    fun reduce_positionIsBeyondDuration_error() {
        val progress = PlaybackProgress(
            positionInDuration = 7.seconds.inMilliseconds,
            totalChaptersDuration = 5.seconds.inMilliseconds)
        val error = Reducer.verifyProgressIsNotBeyondDuration(progress)
        assertThat(error).isInstanceOf(IncorrectChapterMetadataException::class.java)
    }

    @Test
    fun reduce_positionIsAtEndOfDuration_noError() {
        val progress = PlaybackProgress(
            positionInDuration = 7.seconds.inMilliseconds,
            totalChaptersDuration = 7.seconds.inMilliseconds)
        val error = Reducer.verifyAudiobookIsComplete(progress)
        assertThat(error).isNull()
    }

    @Test
    fun reduce_positionIsBeforeEndOfDuration_error() {
        val progress = PlaybackProgress(
            positionInDuration = 5.seconds.inMilliseconds,
            totalChaptersDuration = 7.seconds.inMilliseconds)
        val error = Reducer.verifyAudiobookIsComplete(progress)
        assertThat(error).isInstanceOf(IncorrectChapterMetadataException::class.java)
    }

    @Test
    fun reduce_disableDiscrepanciesPositionIsBeyondDuration_noError() {
        Reducer.reduce(MockModels.appState(),
            NewAudioPlayableAction(MockModels.audiobook(), ArmadilloConfiguration.MAX_DISCREPANCY_DISABLE, 7000.milliseconds))
        val progress = PlaybackProgress(
            positionInDuration = 7.seconds.inMilliseconds,
            totalChaptersDuration = 5.seconds.inMilliseconds)
        val error = Reducer.verifyProgressIsNotBeyondDuration(progress)
        assertThat(error).isNull()
        resetMaxDurationDiscrepancy()
    }

    @Test
    fun reduce_disableDiscrepanciesPositionIsBeforeEndOfDuration_noError() {
        Reducer.reduce(MockModels.appState(),
            NewAudioPlayableAction(MockModels.audiobook(), ArmadilloConfiguration.MAX_DISCREPANCY_DISABLE, 5000.milliseconds))
        val progress = PlaybackProgress(
            positionInDuration = 5.seconds.inMilliseconds,
            totalChaptersDuration = 7.seconds.inMilliseconds)
        val error = Reducer.verifyAudiobookIsComplete(progress)
        assertThat(error).isNull()
        resetMaxDurationDiscrepancy()
    }

    @Test
    fun reduce_contentEndedAction_updatesControlState() {
        val appState = Reducer.reduce(MockModels.appState(), ContentEndedAction)

        assertThat(appState.playbackInfo!!.controlState.hasContentEnded).isTrue()
    }

    private fun resetMaxDurationDiscrepancy() {
        Reducer.reduce(MockModels.appState(),
            NewAudioPlayableAction(MockModels.audiobook(), ArmadilloConfiguration.MAX_DISCREPANCY_DEFAULT, 0.milliseconds))
    }
}