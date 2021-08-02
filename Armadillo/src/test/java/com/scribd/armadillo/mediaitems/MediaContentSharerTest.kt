package com.scribd.armadillo.mediaitems

import android.support.v4.media.MediaBrowserCompat
import androidx.media.MediaBrowserServiceCompat
import com.scribd.armadillo.ArmadilloPlayer
import com.scribd.armadillo.StateStore
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class MediaContentSharerTest {

    private lateinit var sharer: MediaContentSharer
    private lateinit var stateProvider: StateStore.Provider
    private lateinit var browseController: ArmadilloPlayer.MediaBrowseController
    private lateinit var mediaRoot: MediaBrowserCompat.MediaItem
    private lateinit var exampleMediaItem: MediaBrowserCompat.MediaItem

    private val testDocId = "8"

    @Before
    fun setUp() {
        stateProvider = mock()
        browseController = mock()
        mediaRoot = mock()
        whenever(browseController.root).thenReturn(mediaRoot)
        whenever(mediaRoot.mediaId).thenReturn("media_root_id")
        sharer = MediaContentSharer(stateProvider)
        exampleMediaItem = mock()
    }

    @Test
    fun getMediaRootId_notEnabled_emptyRoot() {
        assertThat(sharer.mediaRootId).isEqualTo("empty_root_id")
    }

    @Test
    fun getMediaRootId_isEnabled_browsableRoot() {
        sharer.isBrowsingEnabled = true
        sharer.browseController = browseController
        assertThat(sharer.mediaRootId).isEqualTo("media_root_id")
    }

    @Test
    fun determineBrowserRoot_exclusiveClientList_noRoot() {
        sharer.isBrowsingEnabled = true
        sharer.browseController = browseController
        whenever(browseController.authorizeExternalClient(ArmadilloMediaBrowse.ExternalClient(0, "bad"))).thenReturn(false)
        val root = sharer.determineBrowserRoot("bad", 0, null)
        assertThat(root).isNull()
    }

    @Test
    fun determineBrowserRoot_exclusiveClientListWithCorrectClient_allowed() {
        sharer.isBrowsingEnabled = true
        sharer.browseController = browseController
        whenever(browseController.authorizeExternalClient(ArmadilloMediaBrowse.ExternalClient(0, "good"))).thenReturn(true)
        val root = sharer.determineBrowserRoot("good", 0, null)
        assertThat(root!!.rootId).isEqualTo("media_root_id")
    }

    @Test
    fun loadChildrenOf_categoryBrowsed_receiveListOfItemsInCategory() {
        val result: MediaBrowserServiceCompat.Result<MutableList<MediaBrowserCompat.MediaItem>> = mock()
        val mediaList: MutableList<MediaBrowserCompat.MediaItem> = mutableListOf(exampleMediaItem)
        sharer.isBrowsingEnabled = true
        sharer.browseController = browseController
        whenever(browseController.isItemPlayable(testDocId)).thenReturn(false)
        whenever(browseController.onChildrenOfCategoryRequested(testDocId)).thenReturn(mediaList)
        sharer.loadChildrenOf(testDocId, result)
        verify(result).sendResult(mediaList)
    }

    @Test
    fun loadChildrenOf_itemSelected_receiveItemInResultAndPlay() {
        val result: MediaBrowserServiceCompat.Result<MutableList<MediaBrowserCompat.MediaItem>> = mock()
        val mediaList: MutableList<MediaBrowserCompat.MediaItem> = mutableListOf(exampleMediaItem)
        sharer.isBrowsingEnabled = true
        sharer.browseController = browseController
        whenever(browseController.isItemPlayable(testDocId)).thenReturn(true)
        whenever(browseController.getItemFromId(testDocId)).thenReturn(exampleMediaItem)
        sharer.loadChildrenOf(testDocId, result)
        verify(result).sendResult(mediaList)
        verify(browseController).getItemFromId(testDocId)
    }

    @Test
    fun prepareMedia_dontAutoplay() {
        whenever(browseController.isItemPlayable(testDocId)).thenReturn(true)
        sharer.isBrowsingEnabled = true
        sharer.browseController = browseController
        sharer.prepareMedia(testDocId, false)
        verify(browseController).onContentToPlaySelected(testDocId, false)
    }

    @Test
    fun prepareMedia_doAutoplay() {
        whenever(browseController.isItemPlayable(testDocId)).thenReturn(true)
        sharer.isBrowsingEnabled = true
        sharer.browseController = browseController
        sharer.prepareMedia(testDocId, true)
        verify(browseController).onContentToPlaySelected(testDocId, true)
    }

    @Test
    fun notifyContentChanged__hasListener_callsListener() {
        val listener: ArmadilloMediaBrowse.ExternalServiceListener = mock()
        sharer.externalServiceListener = listener
        sharer.notifyContentChanged("media_root_id")
        verify(listener).onContentChangedAndRefreshNeeded("media_root_id")
    }
}