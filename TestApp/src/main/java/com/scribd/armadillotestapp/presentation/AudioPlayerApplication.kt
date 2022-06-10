package com.scribd.armadillotestapp.presentation

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.core.app.NotificationCompat
import com.scribd.armadillo.ArmadilloPlayer
import com.scribd.armadillo.ArmadilloPlayerFactory
import com.scribd.armadillo.mediaitems.ArmadilloMediaBrowse
import com.scribd.armadillo.models.AudioPlayable
import com.scribd.armadillo.playback.PlaybackService
import com.scribd.armadillotestapp.data.Content
import com.scribd.armadillotestapp.presentation.di.AppModule
import com.scribd.armadillotestapp.presentation.di.DaggerMainComponent
import com.scribd.armadillotestapp.presentation.di.MainComponent
import javax.inject.Inject

class AudioPlayerApplication : Application() {
    lateinit var mainComponent: MainComponent

    @Inject
    lateinit var armadilloPlayer: dagger.Lazy<ArmadilloPlayer>

    @Inject
    lateinit var content: Content

    override fun onCreate() {
        super.onCreate()


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createDownloadNotificationChannel()
        }

        initDependencies()
        setExternalServiceEnabled()
        armadilloPlayer.get().initDownloadEngine()
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createDownloadNotificationChannel() {
        val channel = NotificationChannel(AudioDownloadManager.CHANNEL_ID,
            getString(AudioDownloadManager.CHANNEL_NAME_RES),
            NotificationManager.IMPORTANCE_HIGH)

        channel.enableLights(true)
        channel.enableVibration(true)
        channel.setShowBadge(true)
        channel.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }

    private fun initDependencies() {
        mainComponent = DaggerMainComponent.builder()
            .appModule(AppModule(applicationContext))
            .build()

    }

    private fun setExternalServiceEnabled() {
        mainComponent.inject(this)
        ArmadilloPlayerFactory.initDownloadTracker(this)
        armadilloPlayer.get().enableExternalMediaBrowsing(object : ArmadilloPlayer.MediaBrowseController {
            override fun authorizeExternalClient(client: ArmadilloMediaBrowse.ExternalClient): Boolean = true

            override val root: MediaBrowserCompat.MediaItem = MediaBrowserCompat.MediaItem(
                MediaDescriptionCompat.Builder().setMediaId("media_browse_id").build(),
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)

            override fun isItemPlayable(mediaId: String): Boolean = mediaId != root.mediaId

            override fun getItemFromId(mediaId: String): MediaBrowserCompat.MediaItem? = content.mediaForId(mediaId)

            @SuppressLint("CheckResult")
            override fun onContentToPlaySelected(mediaId: String, playImmediately: Boolean): MediaBrowserCompat.MediaItem? {
                return when (mediaId) {
                    "error" -> {
                        val mediaDescriptionCompat = MediaDescriptionCompat.Builder()
                            .setTitle("This is an error message")
                            .setMediaId(PlaybackService.MEDIA_ERROR)
                            .build()
                        MediaBrowserCompat.MediaItem(mediaDescriptionCompat, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
                    }
                    else -> {
                        content.mediaForId(mediaId)?.also { mediaItem ->
                            val playable = AudioPlayable(
                                id = mediaItem.mediaId!!.toInt(),
                                title = mediaItem.description.title.toString(),
                                request = AudioPlayable.MediaRequest.createHttpUri(mediaItem.description.mediaUri!!.toString()),
                                chapters = emptyList() // BUG BUG mediaItem chapters will not be correct here
                            )
                            armadilloPlayer.get().beginPlayback(playable)
                        }
                    }
                }
            }

            override fun onChildrenOfCategoryRequested(parentId: String): List<MediaBrowserCompat.MediaItem> = content.mediaItems

            override fun onContentSearchToPlaySelected(query: String?, playImmediately: Boolean): MediaBrowserCompat.MediaItem? {
                if (query.isNullOrEmpty()) {
                    return content.randomMediaItem()
                }
                return content.mediaItems.find {
                    it.description.title.toString() == query
                }
            }

            override fun authorizationStatus(): ArmadilloMediaBrowse.Browser.AuthorizationStatus =
                ArmadilloMediaBrowse.Browser.AuthorizationStatus.Authorized
        })
    }
}