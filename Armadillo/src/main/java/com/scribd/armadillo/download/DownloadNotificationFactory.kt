package com.scribd.armadillo.download

import android.app.Notification
import android.content.Context
import com.scribd.armadillo.models.DownloadProgressInfo

/**
 * Used to build notifications for in-progress downloads.
 */
interface DownloadNotificationFactory {
    fun getForegroundNotification(context: Context, downloadStates: Array<DownloadProgressInfo>): Notification
}