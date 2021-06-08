package com.scribd.armadillo

import com.scribd.armadillo.extensions.filterOutCompletedItems
import com.scribd.armadillo.extensions.removeItemsByUrl
import com.scribd.armadillo.extensions.replaceDownloadProgressItemsByUrl
import com.scribd.armadillo.models.DownloadProgressInfo
import com.scribd.armadillo.models.DownloadState
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class UtilKtTest {

    private companion object {
        const val DOWNLOAD_ID = 1234
    }

    private val downloadProgressList = listOf("aaa", "bbb", "ccc").map { url ->
        DownloadProgressInfo(DOWNLOAD_ID, url, DownloadState.COMPLETED)
    }

    @Test
    fun replaceDownloadProgressItemsByUrl_hasOldItemsOnly_returnsAllDownloaded() {
        val updatedList = downloadProgressList.replaceDownloadProgressItemsByUrl(emptyList())

        assertThat(updatedList).isEqualTo(downloadProgressList)
    }

    @Test
    fun replaceDownloadProgressItemsByUrl_hasNewUniqueItems_returnsOldAndNewItems() {
        val newItems = listOf("ddd", "eee").map { url ->
            DownloadProgressInfo(DOWNLOAD_ID, url, DownloadState.STARTED(0, 0))
        }

        val updatedList = downloadProgressList.replaceDownloadProgressItemsByUrl(newItems)

        val correctItems = downloadProgressList.plus(newItems)

        assertThat(updatedList).isEqualTo(correctItems)
    }

    @Test
    fun replaceDownloadProgressItemsByUrl_hasItemsWithSameUrl_returnsAnUpdatedList() {
        val newItems = listOf("ccc").map { url ->
            DownloadProgressInfo(DOWNLOAD_ID, url, DownloadState.STARTED(10, 10))
        }

        val updatedList = downloadProgressList.replaceDownloadProgressItemsByUrl(newItems)

        val correctItems = listOf(
                DownloadProgressInfo(DOWNLOAD_ID, "aaa", DownloadState.COMPLETED),
                DownloadProgressInfo(DOWNLOAD_ID, "bbb", DownloadState.COMPLETED),
                DownloadProgressInfo(DOWNLOAD_ID, "ccc", DownloadState.STARTED(10, 10)))

        assertThat(updatedList).isEqualTo(correctItems)
    }

    @Test
    fun removeItemsByUrl_hasItemsToRemove_removesItems() {
        val items = listOf("aaa", "bbb", "ccc").map { url ->
            DownloadProgressInfo(DOWNLOAD_ID, url, DownloadState.STARTED(10, 10))
        }

        val itemsToRemove = listOf("bbb", "ccc").map { url ->
            DownloadProgressInfo(DOWNLOAD_ID, url, DownloadState.REMOVED)
        }

        val updatedList = items.removeItemsByUrl(itemsToRemove)
        val expectedList = listOf("aaa").map { url ->
            DownloadProgressInfo(DOWNLOAD_ID, url, DownloadState.STARTED(10, 10))
        }

        assertThat(updatedList).isEqualTo(expectedList)
    }

    @Test
    fun removeItemsByUrl_hasNoItemsToRemove_returnsOriginalList() {
        val items = listOf("aaa", "bbb", "ccc").map { url ->
            DownloadProgressInfo(DOWNLOAD_ID, url, DownloadState.STARTED(10, 10))
        }

        val updatedList = items.removeItemsByUrl(emptyList())

        assertThat(updatedList).isEqualTo(items)
    }

    @Test
    fun removeItemsByUrl_shouldRemoveAllItems_returnsEmptyList() {
        val items = listOf("aaa", "bbb", "ccc").map { url ->
            DownloadProgressInfo(DOWNLOAD_ID, url, DownloadState.STARTED(10, 10))
        }

        val updatedList = items.removeItemsByUrl(items)

        val expectedList = emptyList<DownloadProgressInfo>()

        assertThat(updatedList).isEqualTo(expectedList)
    }

    @Test
    fun filterOutCompletedItems_allItemsComplete_returnsEmptyList() {
        val filteredItems = downloadProgressList.filterOutCompletedItems()
        assertThat(filteredItems).isEmpty()
    }

    @Test
    fun filterOutCompletedItems_someItemsComplete_returnsInProgressItems() {
        val items = listOf(
                DownloadProgressInfo(DOWNLOAD_ID, "", DownloadState.STARTED(10, 10)),
                DownloadProgressInfo(DOWNLOAD_ID, "", DownloadState.COMPLETED))

        val filteredItems = items.filterOutCompletedItems()
        val expectedItems = listOf(DownloadProgressInfo(DOWNLOAD_ID, "", DownloadState.STARTED(10, 10)))

        assertThat(filteredItems).isEqualTo(expectedItems)
    }
}