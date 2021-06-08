package com.scribd.armadillotestapp.presentation.extensions

import android.net.Uri
import com.scribd.armadillo.models.AudioPlayable
import com.scribd.armadillo.models.Chapter
import com.scribd.armadillo.time.Interval
import com.scribd.armadillo.time.Millisecond
import com.scribd.armadillo.time.Second
import com.scribd.armadillo.time.milliseconds

data class DataAudiobook(val chapters: List<DataChapter>, val duration: Interval<Second>)

data class DataChapter(
    val title: String,
    val part: Int,
    val chapter: Int,
    val duration: Interval<Millisecond>)

fun DataAudiobook.toArmadilloAudiobook(id: Int, title: String, uri: Uri): AudioPlayable =
    AudioPlayable(
        id = id,
        title = title,
        request = AudioPlayable.MediaRequest.createHttpUri(uri.toString()),
        chapters = this.chapters.toArmadilloChapters())

private fun List<DataChapter>.toArmadilloChapters(): List<Chapter> {
    val chapters = mutableListOf<Chapter>()

    this.forEachIndexed { i, displayChapter ->
        val chapter = when (i) {
            0 -> {
                Chapter(
                    title = displayChapter.title,
                    part = displayChapter.part,
                    chapter = displayChapter.chapter,
                    startTime = 0.milliseconds,
                    duration = displayChapter.duration)
            }
            this.size - 1 -> {
                val previousChapter = chapters[i - 1]
                val startTime = previousChapter.endTime

                // Math.floor used because chapter runtime must be slightly less then audioPlayable runtime in order to detect end of book
                val endTime = Math.floor(previousChapter.endTime.value + displayChapter.duration.value).milliseconds
                Chapter(
                    title = displayChapter.title,
                    part = displayChapter.part,
                    chapter = displayChapter.chapter,
                    startTime = startTime,
                    duration = endTime - startTime)
            }
            else -> {
                val previousChapter = chapters[i - 1]
                Chapter(
                    title = displayChapter.title,
                    part = displayChapter.part,
                    chapter = displayChapter.chapter,
                    startTime = Math.round(previousChapter.startTime.value + previousChapter.duration.value).milliseconds,
                    duration = Math.round(displayChapter.duration.value).milliseconds)
            }
        }
        chapters.add(chapter)
    }

    return chapters
}