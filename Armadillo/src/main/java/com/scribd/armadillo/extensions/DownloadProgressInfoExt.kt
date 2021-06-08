package com.scribd.armadillo.extensions

import com.scribd.armadillo.models.DownloadProgressInfo
import com.scribd.armadillo.models.DownloadState

internal fun List<DownloadProgressInfo>.replaceDownloadProgressItemsByUrl(newItems: List<DownloadProgressInfo>): List<DownloadProgressInfo> =
        this.filter { oldItem ->
            newItems.find { it.url == oldItem.url } == null
        }.plus(newItems)

internal fun List<DownloadProgressInfo>.removeItemsByUrl(itemsToRemove: List<DownloadProgressInfo>): List<DownloadProgressInfo> =
        this.filter { oldItem ->
            itemsToRemove.find { it.url == oldItem.url } == null
        }

internal fun List<DownloadProgressInfo>.filterOutCompletedItems() =
        this.filterNot { DownloadState.COMPLETED == it.downloadState }