package com.scribd.armadillo.download

import android.app.Notification
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import androidx.media3.exoplayer.scheduler.Scheduler
import com.scribd.armadillo.R
import com.scribd.armadillo.di.Injector
import com.scribd.armadillo.models.DownloadState
import javax.inject.Inject

/**
 * This is a basic [DownloadService] that provides a simple default notification. It will store downloads in external storage. If you
 * need more control over the downloads or notifications, you should provide your own implementation class to the download configuration.
 */
@OptIn(UnstableApi::class) internal class DefaultExoplayerDownloadService : DownloadService(FOREGROUND_NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL, CHANNEL_ID, CHANNEL_NAME, 0) {

    init {
        Injector.mainComponent.inject(this)
    }

    companion object {
        internal const val CHANNEL_ID = "download_channel"
        private const val FOREGROUND_NOTIFICATION_ID = 1
        private const val JOB_ID = 1

        private val CHANNEL_NAME = R.string.arm_default_download_channel
    }

    @Inject
    internal lateinit var downloadManager: DownloadManager

    override fun getDownloadManager(): DownloadManager = downloadManager

    override fun getScheduler(): Scheduler? = if (Util.SDK_INT >= 21) PlatformScheduler(this, JOB_ID) else null

    override fun getForegroundNotification(downloads: MutableList<Download>, notMetRequirements: Int): Notification {
        return getNotificationFactory().getForegroundNotification(this, downloads.mapNotNull { TestableDownloadState(it).toDownloadInfo() }.filter {
            it.downloadState != DownloadState.REMOVED
        }.toTypedArray())
    }

    // TODO: [AND-10580] expecting data to stay here is unreliable. Need a default in case the object does not exist
    private fun getNotificationFactory(): DownloadNotificationFactory = DownloadNotificationHolder.downloadNotificationFactory
        ?: DefaultDownloadNotificationFactory()

    //TODO: Notification for finished / cancelled. Override onTaskStateChanged
}
