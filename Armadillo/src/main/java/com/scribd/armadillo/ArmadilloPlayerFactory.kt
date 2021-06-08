package com.scribd.armadillo

import android.annotation.SuppressLint
import android.content.Context
import com.scribd.armadillo.di.Injector
import com.scribd.armadillo.download.DownloadNotificationFactory
import com.scribd.armadillo.download.DownloadNotificationHolder
import com.scribd.armadillo.download.DownloadTracker
import com.scribd.armadillo.models.ArmadilloState
import com.scribd.armadillo.playback.PlaybackNotificationBuilder
import com.scribd.armadillo.playback.PlaybackNotificationBuilderHolder
import javax.inject.Inject

/**
 * Client will use to create an [ArmadilloPlayer] instance
 */
object ArmadilloPlayerFactory {
    @set:Inject
    internal lateinit var stateInitializer: StateStore.Initializer

    @set:Inject
    internal lateinit var downloadTracker: DownloadTracker

    /**
     * This should be initialized before [init]. It can be done when the app is first launched.
     *
     * Currently, the client is expected to track & stores all info related to downloads. Armadillo stores references to which tracks are
     * downloaded so that multi-track content can be played offline, but does not expose any methods for tracking what content is downloaded
     */
    fun initDownloadTracker(context: Context) {
        Injector.buildDependencyGraph(context)
        Injector.mainComponent.inject(this)
        stateInitializer.init(ArmadilloState(downloadInfo = emptyList()))
        downloadTracker.init()
    }

    /**
     * Initialize the armadillo engine.
     *
     * @param downloadNotificationFactory This should be a simple factory class, as it will be held by the library to use to create
     * download notifications
     * @param playbackNotificationBuilder This should be a simple factory class, as it will be held by the library to use to create
     * playback notifications
     */
    @SuppressLint("VisibleForTests")
    @JvmOverloads
    fun init(downloadNotificationFactory: DownloadNotificationFactory? = null,
             playbackNotificationBuilder: PlaybackNotificationBuilder? = PlaybackNotificationBuilderHolder.builder): ArmadilloPlayer {
        DownloadNotificationHolder.downloadNotificationFactory = downloadNotificationFactory
        PlaybackNotificationBuilderHolder.builder = playbackNotificationBuilder
        return ArmadilloPlayerChoreographer()
    }
}
