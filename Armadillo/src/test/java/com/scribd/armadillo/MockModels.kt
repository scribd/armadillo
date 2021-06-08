package com.scribd.armadillo

import com.scribd.armadillo.actions.PlaybackProgressAction
import com.scribd.armadillo.actions.UpdateDownloadAction
import com.scribd.armadillo.models.ArmadilloState
import com.scribd.armadillo.models.AudioPlayable
import com.scribd.armadillo.models.Chapter
import com.scribd.armadillo.models.DownloadProgressInfo
import com.scribd.armadillo.models.DownloadState
import com.scribd.armadillo.models.PlaybackInfo
import com.scribd.armadillo.models.PlaybackProgress
import com.scribd.armadillo.models.PlaybackState
import com.scribd.armadillo.models.MediaControlState
import com.scribd.armadillo.time.milliseconds

internal class MockModels {
    companion object {
        private const val CURRENT_AUDIOBOOK_URL = "http://www.awesomeaudiobooks.com/books/12345"
        fun appState(): ArmadilloState = ArmadilloState(
                playbackInfo = PlaybackInfo(
                        audioPlayable = audiobook(),
                        playbackState = PlaybackState.NONE,
                        playbackSpeed = 1.0f,
                        controlState = MediaControlState(),
                        isLoading = true,
                        skipDistance = Constants.AUDIO_SKIP_DURATION,
                        progress = PlaybackProgress(totalChaptersDuration = audiobook().duration)),
                downloadInfo = emptyList())

        fun audiobook(): AudioPlayable = AudioPlayable(id = 123, title = "Into the Wild", chapters = chapters(), request = AudioPlayable.MediaRequest.createHttpUri(CURRENT_AUDIOBOOK_URL))

        fun progressAction(): PlaybackProgressAction = PlaybackProgressAction(
            currentPosition = 100.milliseconds,
            playerDuration = null)

        fun downloadInfo(): DownloadProgressInfo = DownloadProgressInfo(
                id = 123456,
                url = CURRENT_AUDIOBOOK_URL,
                downloadState = DownloadState.STARTED(50, 100L))

        fun updateDownloadAction(): UpdateDownloadAction = UpdateDownloadAction(downloadInfo())

        fun chapters(): List<Chapter> {
            val chp1 = Chapter("Chapter 0 part 0", 0, 1,
                    0.milliseconds, 405582.milliseconds)
            val chp2 = Chapter("Chapter 1 part 1", 0, 2,
                    405582.milliseconds, 1645772.milliseconds)
            val chp3 = Chapter("Chapter 2 part 1", 0, 3,
                    2051354.milliseconds, 1750.milliseconds)
            val chp4 = Chapter("Chapter 3 part 1", 0, 4,
                    2053104.milliseconds, 134566.milliseconds)
            val chp5 = Chapter("Chapter 4 part 1", 0, 5,
                    2187670.milliseconds, 589376.milliseconds)

            return listOf(chp1, chp2, chp3, chp4, chp5)
        }
    }
}