package com.scribd.armadillo.download

import com.scribd.armadillo.models.DownloadProgressInfo
import com.scribd.armadillo.models.DownloadState
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class DownloadManagerExtKtTest {
    private companion object {
        const val ID = 1234
        const val URL = "www.coolaudiobook.com"
        const val DOWNLOAD_PERCENT = 50
        const val DOWNLOADED_BYTES = 100L
    }

    private lateinit var downloadState: TestableDownloadState
    @Before
    fun setUp() {
        downloadState = TestableDownloadState(
                ID,
                URL,
                TestableDownloadState.COMPLETED,
                DOWNLOAD_PERCENT,
                DOWNLOADED_BYTES)
    }

    @Test
    fun toDownloadInfo_isQueuedAction_returnsNull() {
        val state = downloadState.copy(state = TestableDownloadState.QUEUED)
        val downloadInfo = state.toDownloadInfo()
        assertThat(downloadInfo).isNull()
    }

    @Test
    fun toDownloadInfo_downloadRemovalComplete_returnsRemoveProgress() {
        val state = downloadState.copy(
                state = TestableDownloadState.REMOVING)
        val downloadInfo = state.toDownloadInfo()!!
        assertThat(downloadInfo.id).isEqualTo(ID)
        assertThat(downloadInfo.url).isEqualTo(URL)
        assertThat(downloadInfo.downloadState).isEqualTo(DownloadState.REMOVED)
    }

    @Test
    fun toDownloadInfo_downloadComplete_returnsCompletedProgress() {
        val state = downloadState.copy(
                state = TestableDownloadState.COMPLETED)
        val downloadInfo = state.toDownloadInfo()!!
        assertThat(downloadInfo.id).isEqualTo(ID)
        assertThat(downloadInfo.url).isEqualTo(URL)
        assertThat(downloadInfo.downloadState).isEqualTo(DownloadState.COMPLETED)
    }

    @Test
    fun toDownloadInfo_downloadProgressJustBegan_returnsProgress() {
        val state = downloadState.copy(
                state = TestableDownloadState.IN_PROGRESS,
                downloadPercentage = DownloadProgressInfo.PROGRESS_UNSET)
        val downloadInfo = state.toDownloadInfo()!!
        assertThat(downloadInfo.id).isEqualTo(ID)
        assertThat(downloadInfo.url).isEqualTo(URL)
        assertThat(downloadInfo.downloadState).isEqualTo(DownloadState.STARTED(0, DOWNLOADED_BYTES))
    }

    @Test
    fun toDownloadInfo_downloadProgressWithProgress_returnsProgress() {
        val state = downloadState.copy(
                state = TestableDownloadState.IN_PROGRESS)
        val downloadInfo = state.toDownloadInfo()!!
        assertThat(downloadInfo.id).isEqualTo(ID)
        assertThat(downloadInfo.url).isEqualTo(URL)
        assertThat(downloadInfo.downloadState).isEqualTo(DownloadState.STARTED(DOWNLOAD_PERCENT, DOWNLOADED_BYTES))
    }

    @Test
    fun toDownloadInfo_unknownState_returnsFailed() {
        val state = downloadState.copy(
                state = 1000)
        val downloadInfo = state.toDownloadInfo()!!
        assertThat(downloadInfo.id).isEqualTo(ID)
        assertThat(downloadInfo.url).isEqualTo(URL)
        assertThat(downloadInfo.downloadState).isEqualTo(DownloadState.FAILED)
    }
}