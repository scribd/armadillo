package com.scribd.armadillo.download

import com.google.android.exoplayer2.offline.DownloadIndex
import com.google.android.exoplayer2.offline.DownloadManager
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import com.scribd.armadillo.StateStore
import com.scribd.armadillo.actions.ErrorAction
import com.scribd.armadillo.actions.StopTrackingDownloadAction
import com.scribd.armadillo.actions.UpdateDownloadAction
import com.scribd.armadillo.error.DownloadFailed
import com.scribd.armadillo.models.DownloadProgressInfo
import com.scribd.armadillo.models.DownloadState
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class ExoplayerDownloadTrackerTest {
    private companion object {
        const val ID = 1
        const val URL = "cool url"
    }

    private lateinit var exoplayerDownloadTracker: ExoplayerDownloadTracker
    private lateinit var downloadManager: DownloadManager
    private lateinit var stateModifier: StateStore.Modifier
    private lateinit var downloadInfo: DownloadProgressInfo
    @Before
    fun setUp() {
        stateModifier = mock()
        downloadManager = mock()
        val downloadIndex : DownloadIndex = mock()
        whenever(downloadManager.downloadIndex).thenReturn(downloadIndex)
        exoplayerDownloadTracker = ExoplayerDownloadTracker(mock(), downloadManager, stateModifier)
    }

    @Test
    fun dispatchActionsForProgress_taskFailed_dispatchesActions() {
        downloadInfo = DownloadProgressInfo(ID, URL, DownloadState.FAILED)
        exoplayerDownloadTracker.dispatchActionsForProgress(downloadInfo)
        verify(stateModifier).dispatch(UpdateDownloadAction(downloadInfo))
        verify(stateModifier).dispatch(ErrorAction(DownloadFailed))
        verify(stateModifier).dispatch(StopTrackingDownloadAction(downloadInfo))
        verifyNoMoreInteractions(stateModifier)
    }

    @Test
    fun dispatchActionsForProgress_isComplete_dispatchesActions() {
        downloadInfo = DownloadProgressInfo(ID, URL, DownloadState.COMPLETED)
        exoplayerDownloadTracker.dispatchActionsForProgress(downloadInfo)
        verify(stateModifier).dispatch(UpdateDownloadAction(downloadInfo))
        verify(stateModifier).dispatch(StopTrackingDownloadAction(downloadInfo))
        verifyNoMoreInteractions(stateModifier)
    }
}