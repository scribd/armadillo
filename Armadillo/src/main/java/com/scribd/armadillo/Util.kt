package com.scribd.armadillo

import android.content.Context
import android.os.Build
import com.scribd.armadillo.models.Chapter
import com.scribd.armadillo.time.Interval
import com.scribd.armadillo.time.Millisecond
import com.scribd.armadillo.time.milliseconds
import java.io.File

internal typealias ExoplayerDownload = com.google.android.exoplayer2.offline.Download
internal typealias Milliseconds = Interval<Millisecond>

fun exoplayerExternalDirectory(context: Context): File =
    File(context.getExternalFilesDir(null), Constants.Exoplayer.EXOPLAYER_DIRECTORY)

fun sanitizeChapters(chapters: List<Chapter>): List<Chapter> {
    return chapters.mapIndexed { i, chapter ->
        when (i) {
            0 -> {
                if (chapter.startTime != 0.milliseconds) {
                    throw IllegalStateException("Chapter must begin at 0")
                }
                chapter.copy(
                        startTime = 0.milliseconds,
                        duration = Math.round(chapter.duration.value).milliseconds)

            }
            chapters.size - 1 -> {
                // Math.floor used because chapter runtime must be slightly less then audioPlayable runtime in order to detect end of book
                val endTime = Math.floor(chapters.last().startTime.value + chapters.last().duration.value).milliseconds
                val previousChapter = chapters[i - 1]
                val startTime = previousChapter.startTime + previousChapter.duration
                chapter.copy(
                        startTime = startTime,
                        duration = endTime - startTime)
            }
            else -> {
                val previousChapter = chapters[i - 1]
                chapter.copy(
                        startTime = Math.round(previousChapter.startTime.value + previousChapter.duration.value).milliseconds,
                        duration = Math.round(chapter.duration.value).milliseconds)

            }
        }
    }
}

fun hasSnowCone() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S