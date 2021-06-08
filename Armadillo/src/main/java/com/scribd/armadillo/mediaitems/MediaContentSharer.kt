package com.scribd.armadillo.mediaitems

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import androidx.media.MediaBrowserServiceCompat
import com.scribd.armadillo.ArmadilloPlayer
import com.scribd.armadillo.StateStore
import com.scribd.armadillo.extensions.getCurrentlyPlayingId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MediaContentSharer @Inject constructor(private val playerState: StateStore.Provider)
    : ArmadilloMediaBrowse.Browser, ArmadilloMediaBrowse.ContentSharer {

    companion object {
        private const val EMPTY_MEDIA_ROOT_ID = "empty_root_id"
    }

    //root id will change depending on whether or not the content is authorized to be shared.
    override val mediaRootId: String
        get() = if (isBrowsingEnabled) {
            browseController?.root?.mediaId ?: EMPTY_MEDIA_ROOT_ID
        } else {
            EMPTY_MEDIA_ROOT_ID
        }

    override var browseController: ArmadilloPlayer.MediaBrowseController? = null
    override var externalServiceListener: ArmadilloMediaBrowse.ExternalServiceListener? = null
    override var isBrowsingEnabled: Boolean = false

    override fun determineBrowserRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?)
        : MediaBrowserServiceCompat.BrowserRoot? {

        val authorized =
            browseController?.authorizeExternalClient(ArmadilloMediaBrowse.ExternalClient(clientUid, clientPackageName)) ?: false
        if (authorized) {
            return MediaBrowserServiceCompat.BrowserRoot(mediaRootId, null)
        }
        return null
    }

    override fun loadChildrenOf(parentId: String, result: MediaBrowserServiceCompat.Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        val resultList: MutableList<MediaBrowserCompat.MediaItem> = mutableListOf()
        //Empty users not allowed to use this service, / from google instruction guide
        if (parentId != EMPTY_MEDIA_ROOT_ID) {
            val searchId = getRealMediaId(parentId)

            result.detach() // Wait - expect the user to go to another thread. the external client is told to hold on.
            if (isCategoryBrowsable(searchId)) {
                val items = browseController?.onChildrenOfCategoryRequested(searchId) ?: emptyList()
                resultList.addAll(items)
            } else {
                //we are being asked to play one specific content item
                val currentItem = itemAtId(searchId)
                if (currentItem != null) {
                    resultList.add(currentItem)
                }
            }
        }

        if (resultList.isNotEmpty()) {
            result.sendResult(resultList)
        } else {
            result.sendResult(null)
        }
    }

    override fun prepareMedia(mediaId: String, playImmediately: Boolean): MediaBrowserCompat.MediaItem? =
        playContent(mediaId, playImmediately)

    override fun notifyContentChanged(rootId: String) {
        externalServiceListener?.onContentChangedAndRefreshNeeded(rootId)
    }

    override fun searchForMedia(query: String?, playImmediately: Boolean) {
        searchForContent(query, playImmediately)?.let {
            playContent(it.mediaId!!, playImmediately)
        }
    }

    override fun checkAuthorization(): ArmadilloMediaBrowse.Browser.AuthorizationStatus = browseController?.authorizationStatus() ?:
    ArmadilloMediaBrowse.Browser.AuthorizationStatus.Authorized

    private fun playContent(mediaId: String, playImmediately: Boolean): MediaBrowserCompat.MediaItem? =
        browseController?.onContentToPlaySelected(mediaId, playImmediately)

    private fun searchForContent(query: String?, playImmediately: Boolean): MediaBrowserCompat.MediaItem? =
        browseController?.onContentSearchToPlaySelected(query, playImmediately)

    private fun itemAtId(mediaId: String): MediaBrowserCompat.MediaItem? =
        browseController?.getItemFromId(mediaId)

    private fun isCategoryBrowsable(parentId: String) = browseController?.isItemPlayable(parentId) == false

    private fun getRealMediaId(oldMediaId: String): String =
        if (oldMediaId == 0.toString()) { //sometimes external clients will send a zero when they want to examine their 'current' object.
            (playerState.getCurrentlyPlayingId() ?: 0).toString()
        } else {
            oldMediaId
        }
}