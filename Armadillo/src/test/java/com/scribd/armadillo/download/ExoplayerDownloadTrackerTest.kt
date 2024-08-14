package com.scribd.armadillo.download

import com.google.android.exoplayer2.offline.DownloadIndex
import com.google.android.exoplayer2.offline.DownloadManager
import com.scribd.armadillo.StateStore
import com.scribd.armadillo.actions.ErrorAction
import com.scribd.armadillo.actions.StopTrackingDownloadAction
import com.scribd.armadillo.actions.UpdateDownloadAction
import com.scribd.armadillo.models.DownloadProgressInfo
import com.scribd.armadillo.models.DownloadState
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.isA
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
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
        val downloadIndex: DownloadIndex = mock()
        whenever(downloadManager.downloadIndex).thenReturn(downloadIndex)
        exoplayerDownloadTracker = ExoplayerDownloadTracker(mock(), downloadManager, stateModifier)
    }

    @Test
    fun dispatchActionsForProgress_taskFailed_dispatchesActions() {
        downloadInfo = DownloadProgressInfo(ID, URL, DownloadState.FAILED)
        exoplayerDownloadTracker.dispatchActionsForProgress(downloadInfo)
        verify(stateModifier).dispatch(UpdateDownloadAction(downloadInfo))
        verify(stateModifier).dispatch(isA<ErrorAction>())
        verify(stateModifier).dispatch(StopTrackingDownloadAction(downloadInfo))
        verify(stateModifier, times(3)).dispatch(any())
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