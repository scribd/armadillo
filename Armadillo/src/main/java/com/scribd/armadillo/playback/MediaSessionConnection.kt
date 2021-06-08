package com.scribd.armadillo.playback

import android.content.ComponentName
import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import com.scribd.armadillo.error.MissingDataException

/**
 * This class allows the client to connect to an instance of [MediaBrowserServiceCompat] by managing a connection to [MediaBrowserCompat]
 *
 * This class provides the client with:
 *  - [MediaControllerCompat.TransportControls] in order to dispatch actions to control the player
 *  - [MediaControllerCompat.getPlaybackState] to get the updated player state
 */
internal class MediaSessionConnection(
        private val context: Context,
        private val serviceComponent: ComponentName = ComponentName(context, PlaybackService::class.java)) {

    interface Listener {
        fun onConnectionCallback(transportControls: MediaControllerCompat.TransportControls)
    }

    var mediaController: MediaControllerCompat? = null

    val transportControls: MediaControllerCompat.TransportControls?
        get() = mediaController?.transportControls

    private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(context)

    private lateinit var connectionListener: Listener

    private var mediaBrowser: MediaBrowserCompat? = null

    fun connectToMediaSession(listener: Listener) {
        connectionListener = listener
        mediaBrowser = MediaBrowserCompat(
                context,
                serviceComponent,
                mediaBrowserConnectionCallback, null)
                .apply { connect() }
    }

    private inner class MediaBrowserConnectionCallback(private val context: Context) : MediaBrowserCompat.ConnectionCallback() {

        /**
         * Invoked after [MediaBrowserCompat.connect] when the request has successfully
         * completed.
         */
        override fun onConnected() {
            val mediaBrowserCompat = mediaBrowser ?: throw MissingDataException("media browser should be init")
            val mediaControllerCompat = MediaControllerCompat(context, mediaBrowserCompat.sessionToken)
            mediaController = mediaControllerCompat
            connectionListener.onConnectionCallback(mediaControllerCompat.transportControls)
        }

        /**
         * Invoked when the client is disconnected from the media browser.
         */
        override fun onConnectionSuspended() {
        }

        /**
         * Invoked when the connection to the media browser failed.
         */
        // TODO: [AND-10537] handle case where user is unable to get mediaBrowser
        override fun onConnectionFailed() {
        }
    }
}