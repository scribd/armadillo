package com.scribd.armadillo.mediaitems

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import androidx.media.MediaBrowserServiceCompat
import com.scribd.armadillo.ArmadilloPlayer
import com.scribd.armadillo.error.BadMediaHierarchyException

/**
 *  Provide Media Content to External Clients outside this app, using the Media Browse Framework built into Android.
 *  This interface defines the relationship between several classes.
 *
 *  Armadillo's user - the app that has Armadillo built into it - can be considered its "internal client."  This user provides media
 *  content so that other apps, "external clients", can browse the content and play them through Armadillo. External clients can be Android
 *  Auto, Android Wear, or other third parties. An authenticator interface is provided to separate desired clients and nondesired ones.
 *
 *  MediaItems exist in a hierarchy with a root item that leads to one or more children.
 *
 *  There are two types of MediaItems, playable content and browsable content.
 *  "Browsable" content works as a type of "folder" that can hold more browsable items and playable items.
 *  Playable elements represent audio content. Multiple playable contents under a single browsable parent can be considered a sort
 *  of "playlist" and may be presented as such on an external client designed to play music. Playable items should always be leaves.
 *
 *  Use https://github.com/googlesamples/android-media-controller to test Media Sessions from external clients.
 */
interface ArmadilloMediaBrowse {

    /** Representation of an external client and possible fields it may have to identify itself with. */
    data class ExternalClient(val clientUid: Int?, val clientPackageName: String?)

    /*
    * [PlaybackService] : MediaBrowserServiceCompat -> This Android Framework has the following relevant methods:
    *
    *  //first method called by an external client. Performs authorization and returns the root media item if authorization is valid.
    *  fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot?
    *
    *  //Method called by external clients to browse content provided by Armadillo, starting at a point in the hierarchy defined by parent.
    *  fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>)
    */

    /** Hierarchy Browser of the MediaItem Hierarchy. To be used by [PlaybackService] to browse media content and serve it to external
     * clients. */
    interface Browser {
        val mediaRootId: String
        val isBrowsingEnabled: Boolean
        var externalServiceListener: ExternalServiceListener?

        fun determineBrowserRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): MediaBrowserServiceCompat.BrowserRoot?

        /** throws [BadMediaHierarchyException] if the item does not exist in the hierarchy. */
        fun loadChildrenOf(parentId: String, result: MediaBrowserServiceCompat.Result<MutableList<MediaBrowserCompat.MediaItem>>)

        fun prepareMedia(mediaId: String, playImmediately: Boolean = true): MediaBrowserCompat.MediaItem?

        fun searchForMedia(query: String?, playImmediately: Boolean = true)

        fun checkAuthorization(): AuthorizationStatus

        sealed class AuthorizationStatus {
            object Authorized : AuthorizationStatus()
            data class Unauthorized(val errorMessage: String) : AuthorizationStatus()
        }
    }

    /**
     * Creator of the MediaItem Hierarchy, to be used by [ArmadilloPlayer] and accessible by the user.
     * These fields should be given to Armadillo after initialization, and they shall persist over the life of multiple documents.
     * Changes in this class may not be immediately detectable by an external client, not until they browse for content again. */
    interface ContentSharer {
        var isBrowsingEnabled: Boolean
        var browseController: ArmadilloPlayer.MediaBrowseController?

        /**Tells the external client to reload the content for a given rootId because its children have changed. */
        fun notifyContentChanged(rootId: String)
    }

    /** Give to the Playback Service so that it can hear requests to refresh content data for the given rootId.*/
    interface ExternalServiceListener {
        fun onContentChangedAndRefreshNeeded(rootId: String)
    }
}
