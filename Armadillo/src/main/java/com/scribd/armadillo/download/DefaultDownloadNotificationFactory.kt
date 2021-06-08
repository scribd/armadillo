package com.scribd.armadillo.download

import android.app.Notification
import android.content.Context
import com.scribd.armadillo.R
import com.scribd.armadillo.models.DownloadProgressInfo

internal class DefaultDownloadNotificationFactory : DownloadNotificationFactory {
    override fun getForegroundNotification(context: Context, downloadStates: Array<DownloadProgressInfo>): Notification {
        return ExoplayerNotificationUtil.buildProgressNotification(context, R.drawable.arm_download_icon,
                DefaultExoplayerDownloadService.CHANNEL_ID, null, context.getString(R.string.arm_downloading), downloadStates)
    }
}