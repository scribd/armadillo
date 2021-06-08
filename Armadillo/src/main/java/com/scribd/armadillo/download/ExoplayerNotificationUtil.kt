package com.scribd.armadillo.download

/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import com.scribd.armadillo.R
import com.scribd.armadillo.models.DownloadProgressInfo
import com.scribd.armadillo.models.DownloadState

/** Helper for creating download notifications. From ExoPlayer UI library  */
object ExoplayerNotificationUtil {

    @StringRes
    private val NULL_STRING_ID = 0

    /**
     * Returns a progress notification for the given download states.
     *
     * @param context        A context for accessing resources.
     * @param smallIcon      A small icon for the notification.
     * @param channelId      The id of the notification channel to use. Only required for API level 26 and
     * above.
     * @param contentIntent  An optional content intent to send when the notification is clicked.
     * @param message        An optional message to display on the notification.
     * @param downloadStates The download states
     * @return The notification.
     */
    fun buildProgressNotification(
            context: Context,
            @DrawableRes smallIcon: Int,
            channelId: String,
            contentIntent: PendingIntent?,
            message: String?,
            downloadStates: Array<DownloadProgressInfo>): Notification {
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

        val haveDownloadTasks = downloadTaskCount > 0
        val notificationBuilder = newNotificationBuilder(
                context, smallIcon, channelId, contentIntent, message, R.string.arm_downloading)

        val progress = if (haveDownloadTasks) (totalPercentage / downloadTaskCount) else 0
        val indeterminate = !haveDownloadTasks || allDownloadPercentagesUnknown && hasDownloadedAnyBytes
        notificationBuilder.setProgress(100, progress, indeterminate)
        notificationBuilder.setOngoing(true)
        notificationBuilder.setShowWhen(false)
        return notificationBuilder.build()
    }

    private fun newNotificationBuilder(
            context: Context,
            @DrawableRes smallIcon: Int,
            channelId: String,
            contentIntent: PendingIntent?,
            message: String?,
            @StringRes titleStringId: Int): NotificationCompat.Builder {
        val notificationBuilder = NotificationCompat.Builder(context, channelId).setSmallIcon(smallIcon)
        if (titleStringId != NULL_STRING_ID) {
            notificationBuilder.setContentTitle(context.resources.getString(titleStringId))
        }
        if (contentIntent != null) {
            notificationBuilder.setContentIntent(contentIntent)
        }
        if (message != null) {
            notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(message))
        }
        return notificationBuilder
    }
}
