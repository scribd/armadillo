package com.scribd.armadillotestapp.presentation

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import com.scribd.armadillo.download.DownloadNotificationFactory
import com.scribd.armadillo.models.DownloadProgressInfo
import com.scribd.armadillo.models.DownloadState
import com.scribd.armadillotestapp.R


class AudioDownloadManager : DownloadNotificationFactory {
    companion object {
        const val CHANNEL_ID = "test_app_downloads"
        const val CHANNEL_NAME_RES = R.string.downloads_channel
    }

    override fun getForegroundNotification(context: Context, downloadStates: Array<DownloadProgressInfo>): Notification {
        var totalPercentage = 0
        var downloadTaskCount = 0
        var allDownloadPercentagesUnknown = true
        var hasDownloadedAnyBytes = false

        downloadStates.filter { it.downloadState is DownloadState.STARTED }.forEach { downloadInfo ->
            val downloadState: DownloadState.STARTED = downloadInfo.downloadState as DownloadState.STARTED
            if (downloadState.percentComplete != DownloadProgressInfo.PROGRESS_UNSET) {
                allDownloadPercentagesUnknown = false
                totalPercentage += downloadState.percentComplete
            }
            hasDownloadedAnyBytes = hasDownloadedAnyBytes or (downloadState.downloadedBytes > 0)
            downloadTaskCount++
        }

        val message = context.getString(R.string.download_message, downloadTaskCount)
        val haveDownloadTasks = downloadTaskCount > 0

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(R.drawable.ic_favicon_round)

        val progress = if (haveDownloadTasks) (totalPercentage / downloadTaskCount) else 0
        val indeterminate = !haveDownloadTasks || allDownloadPercentagesUnknown && hasDownloadedAnyBytes
        return notificationBuilder.setProgress(100, progress, indeterminate)
                .setOngoing(true)
                .setShowWhen(false)
                .setOnlyAlertOnce(true)
                .setContentTitle(context.getString(R.string.download_title))
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .build()

    }
}