package com.scribd.armadillo.playback

import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.scribd.armadillo.StateStore
import com.scribd.armadillo.actions.UpdateProgressAction
import com.scribd.armadillo.analytics.PlaybackActionListenerHolder
import com.scribd.armadillo.broadcast.ArmadilloNoisySpeakerReceiver
import com.scribd.armadillo.broadcast.NotificationDeleteReceiver
import com.scribd.armadillo.di.Injector
import com.scribd.armadillo.error.MissingDataException
import com.scribd.armadillo.mediaitems.ArmadilloMediaBrowse
import com.scribd.armadillo.models.AudioPlayable
import com.scribd.armadillo.models.PlaybackInfo
import com.scribd.armadillo.models.PlaybackState
import com.scribd.armadillo.time.seconds
import java.lang.Math.abs
import javax.inject.Inject

/**
 * This class is the entry point for browsing and playback commands from the App's UI
 * and other apps that wish to play audio via Armadillo (for example, Android Auto or
 * the Google Assistant).
 *
 * This class:
 * - delegates the control of a [AudioPlaybackEngine] using [MediaSessionCallback]
 * - places the service in the foreground during audio playback so it is not killed
 * - manages the display of a notification when the service is in the foreground
 */
class PlaybackService : MediaBrowserServiceCompat() {
    @Inject
    internal lateinit var noisyReceiver: ArmadilloNoisySpeakerReceiver
    @Inject
    internal lateinit var playbackStateBuilder: PlaybackStateCompatBuilder
    @Inject
    internal lateinit var mediaMetadataBuilder: MediaMetadataCompatBuilder
    @Inject
    internal lateinit var notificationDeleteReceiver: NotificationDeleteReceiver
    @Inject
    internal lateinit var stateModifier: StateStore.Modifier
    @Inject
    internal lateinit var mediaBrowser: ArmadilloMediaBrowse.Browser
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var playbackNotificationManager: PlaybackNotificationManager
    private val noisySpeakerListener = NoisySpeakerListener()
    private val notificationDeleteListener = NotificationDeleteListener()
    private var isInForeground = false
    private var isNotificationShown = false

    companion object {
        const val TAG = "PlaybackService"
        /**
         * [MAX_DISCREPANCY] is a value representing the maximum discrepancy that is allowed between the audioplayer and any external
         * apps such as Android Auto. This is so the external UI accurately represents the current duration in the chapter.
         */
        val MAX_DISCREPANCY = 5.seconds

        /**
         * When an external client decides that a media couldn't be played (eg lack permission to stream on cellular data),
         * it should send back an error media item. This item should have as MediaDescription :
         * - [MEDIA_ERROR] as media id
         * - the error message as title
         *
         * This media item is used by external app such as Android Auto to display the error in the playback screen
         */
        const val MEDIA_ERROR = "media_error"
    }

    init {
        Injector.mainComponent.inject(this)
    }

    override fun onCreate() {
        super.onCreate()
        Log.v(TAG, "onCreate")
        playbackNotificationManager = PlaybackNotificationManager(this, notificationBuilder)
        mediaSession = MediaSessionCompat(this, TAG)
        mediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)

        PlaybackActionListenerHolder.stateListener = AudioPlaybackStateListener()

        // allows the player to be controlled
        val mediaSessionCallback = MediaSessionCallback(OnMediaSessionEventListener())
        mediaSession.setCallback(mediaSessionCallback)
        mediaSession.isActive = true

        /**
         * In order for [MediaBrowserCompat.ConnectionCallback.onConnected] to be called,
         * a [MediaSessionCompat.Token] needs to be set on the [MediaBrowserServiceCompat].
         */
        sessionToken = mediaSession.sessionToken
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle media buttons when OS is below API 21
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * It's up to the system to call onDestroy and calling [onStopService] won't necessarily cause the service to be stopped immediately.
     * The user could still reopen the app and potentially begin playing an audioPlayable using the same [PlaybackService] instance
     */
    override fun onDestroy() {
        Log.v(TAG, "onDestroy")
        super.onDestroy()
        onStopService()
        mediaSession.isActive = false
        mediaSession.release()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.v(TAG, "onTaskRemoved")
        super.onTaskRemoved(rootIntent)
        onStopService()
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        when (val authorizationStatus: ArmadilloMediaBrowse.Browser.AuthorizationStatus = mediaBrowser.checkAuthorization()) {
            is ArmadilloMediaBrowse.Browser.AuthorizationStatus.Authorized -> {
                mediaBrowser.loadChildrenOf(parentId, result)
                if (mediaSession.controller.playbackState != null) {
                    if (mediaSession.controller.playbackState.state == PlaybackStateCompat.STATE_ERROR) {
                        val state = PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_NONE, 0, 0f).build()
                        mediaSession.setPlaybackState(state)
                    }
                }
            }
            is ArmadilloMediaBrowse.Browser.AuthorizationStatus.Unauthorized -> {
                val errorState = PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_ERROR, 0, 0f)
                    .setErrorMessage(PlaybackStateCompat.ERROR_CODE_AUTHENTICATION_EXPIRED, authorizationStatus.errorMessage).build()
                mediaSession.setPlaybackState(errorState)
                mediaBrowser.loadChildrenOf(parentId, result)
            }
        }
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        val root = mediaBrowser.determineBrowserRoot(clientPackageName, clientUid, rootHints)
        if (root != null) {
            mediaBrowser.externalServiceListener = object : ArmadilloMediaBrowse.ExternalServiceListener {
                override fun onContentChangedAndRefreshNeeded(rootId: String) = notifyChildrenChanged(rootId)
            }
        }
        return root
    }

    // TODO: [AND-10580] expecting data to stay here is unreliable. Need a default in case the object does not exist
    private val notificationBuilder = PlaybackNotificationBuilderHolder.builder
        ?: DefaultPlaybackNotificationBuilder(this)

    internal interface PlaybackStateListener {
        fun onNewAudiobook(audiobook: AudioPlayable)
        fun onPlaybackStateChange(playbackInfo: PlaybackInfo, audiobook: AudioPlayable, currentChapterIndex: Int)
        fun onPlaybackEnd()
    }

    private inner class AudioPlaybackStateListener : PlaybackStateListener {
        private val serviceManager: ServiceManager = PlaybackServiceManager()
        private var lastPlaybackInfo: PlaybackInfo? = null

        override fun onNewAudiobook(audiobook: AudioPlayable) {
            //New audio book should always start with null progress
            mediaSession.setMetadata(mediaMetadataBuilder.build(audiobook))
        }

        override fun onPlaybackStateChange(playbackInfo: PlaybackInfo, audiobook: AudioPlayable, currentChapterIndex: Int) {
            stateModifier.dispatch(UpdateProgressAction(false, currentChapterIndex))
            mediaSession.setMetadata(mediaMetadataBuilder.build(audiobook, playbackInfo.progress))
            val playbackState = playbackInfo.playbackState
            val lastInfo = lastPlaybackInfo
            val playerOutOfSync = if (lastInfo != null) {
                abs(playbackInfo.progress.positionInDuration.longValue -
                    lastInfo.progress.positionInDuration.longValue) > MAX_DISCREPANCY.longValue
            } else false

            if (playbackState != lastPlaybackInfo?.playbackState || playerOutOfSync) {
                mediaSession.setPlaybackState(playbackStateBuilder.build(playbackInfo))
            }

            when (playbackState) {
                PlaybackState.PLAYING -> {
                    serviceManager.startService(audiobook, currentChapterIndex)
                    noisyReceiver.registerForNoisyEvent(noisySpeakerListener)
                    notificationDeleteReceiver.register(notificationDeleteListener)
                }
                PlaybackState.PAUSED -> {
                    // Only update the notification if the playback info changes, e.g. skipping chapters,
                    // and the notification is still shown to prevent dismissed notifications coming back.
                    if (playbackInfo != lastPlaybackInfo && isNotificationShown) {
                        serviceManager.updateNotificationForPause(audiobook, currentChapterIndex)
                    }

                    noisyReceiver.unregisterForNoisyEvent()
                }
                PlaybackState.NONE -> {
                    serviceManager.stopService()
                    noisyReceiver.unregisterForNoisyEvent()
                    notificationDeleteReceiver.unregister()
                }
            }

            lastPlaybackInfo = playbackInfo
        }

        override fun onPlaybackEnd() {
            onStopService(false)
        }
    }

    internal interface ServiceManager {
        fun startService(audiobook: AudioPlayable, currentChapterIndex: Int)
        fun updateNotificationForPause(audiobook: AudioPlayable, currentChapterIndex: Int)
        fun removeNotification()
        fun stopService()
    }

    private inner class PlaybackServiceManager : ServiceManager {
        override fun startService(audiobook: AudioPlayable, currentChapterIndex: Int) {
            val token = sessionToken ?: throw MissingDataException("token should not be null")
            if (!isInForeground) {
                ContextCompat.startForegroundService(
                    this@PlaybackService,
                    Intent(this@PlaybackService, PlaybackService::class.java))
                isInForeground = true
            }

            val notification = playbackNotificationManager.getNotification(audiobook, currentChapterIndex, true, token)
            startForeground(notificationBuilder.notificationId, notification)
            isNotificationShown = true
        }

        override fun updateNotificationForPause(audiobook: AudioPlayable, currentChapterIndex: Int) {
            val token = sessionToken ?: throw MissingDataException("token should not be null")
            stopForeground(false)
            val notification = playbackNotificationManager.getNotification(audiobook, currentChapterIndex, false, token)
            notificationDeleteReceiver.setDeleteIntentOnNotification(notification)
            playbackNotificationManager.notificationManager.notify(notificationBuilder.notificationId, notification)
        }

        override fun removeNotification() {
            stopForeground(true)
            isNotificationShown = false
        }

        override fun stopService() {
            onStopService()
        }
    }

    /**
     * When the service should be shut down.
     *
     * @param stopPlayer If the player should be stopped. This service can be stopped from the player ending, but it can also be stopped
     * here first. If so, we need to clear out the player
     */
    private fun onStopService(stopPlayer: Boolean = true) {
        playbackNotificationManager.notificationManager.cancel(notificationBuilder.notificationId)
        if (stopPlayer) {
            mediaSession.controller.transportControls.stop()
        }
        stopForeground(true)
        stopSelf()
        isInForeground = false
        isNotificationShown = false
    }

    inner class NoisySpeakerListener : ArmadilloNoisySpeakerReceiver.Listener {
        override fun onBecomingNoisy() {
            mediaSession.controller.transportControls.pause()
        }
    }

    inner class NotificationDeleteListener : NotificationDeleteReceiver.Listener {
        override fun onNotificationDeleted() {
            onStopService()
        }
    }

    private inner class OnMediaSessionEventListener : MediaSessionCallback.OnMediaSessionEventListener {

        override fun onPlayMediaFailed(media: MediaBrowserCompat.MediaItem?) {
            when (media?.mediaId) {
                MEDIA_ERROR -> {
                    val errorState = PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_ERROR, 0, 0f)
                        .setErrorMessage(PlaybackStateCompat.ERROR_CODE_APP_ERROR, media.description.title).build()
                    mediaSession.setPlaybackState(errorState)
                }
            }
        }
    }
}