package com.scribd.armadillo.models

import com.scribd.armadillo.MockModels
import com.scribd.armadillo.time.milliseconds
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

class ModelsTest {

    @Rule
    @JvmField
    val exception: ExpectedException = ExpectedException.none()

    @Test
    fun findChapterAtOffset_bookHasJustBegun_returnsChapter() {
        val audiobook = MockModels.audiobook()

        val currentListeningOffset = audiobook.chapters.first().startTime

        val chapter = audiobook.getChapterAtOffset(currentListeningOffset)

        assertThat(audiobook.chapters[0]).isEqualTo(chapter)
    }

    @Test
    fun findChapterAtOffset_chapterHasJustBegun_returnsChapter() {
        val audiobook = MockModels.audiobook()

        val currentListeningOffset = 405582.milliseconds

        val chapter = audiobook.getChapterAtOffset(currentListeningOffset)

        assertThat(audiobook.chapters[1]).isEqualTo(chapter)
    }

    @Test
    fun findChapterAtOffset_chapterIsInMiddle_returnsChapter() {
        val audiobook = MockModels.audiobook()

        val currentListeningOffset = 2103104.milliseconds

        val chapter = audiobook.getChapterAtOffset(currentListeningOffset)

        assertThat(audiobook.chapters[3]).isEqualTo(chapter)
    }

    @Test
    fun findChapterAtOffset_atEndOfAudiobook_returnsLastChapter() {
        val audiobook = MockModels.audiobook()

        val lastChapter = audiobook.chapters.last()

        val listeningOffset = lastChapter.startTime + lastChapter.duration

        val chapter = audiobook.getChapterAtOffset(listeningOffset)

        assertThat(lastChapter).isEqualTo(chapter)
    }

    @Test
    fun findChapterAtOffset_intervalGreaterThenPlaylist_shouldReturnLastChapter() {
        val audiobook = MockModels.audiobook()
        val lastChapter = audiobook.chapters.last()

        val currentListeningOffset =
                lastChapter.startTime + lastChapter.duration + 1.milliseconds

        val chapter = audiobook.getChapterAtOffset(currentListeningOffset)
        assertThat(lastChapter).isEqualTo(chapter)
    }

    @Test
    fun findChapterAtOffset_intervalLessThenPlaylist_shouldGetFirst() {
        val audiobook = MockModels.audiobook()

        val currentListeningOffset = (-1).milliseconds
        val chapter = audiobook.getChapterAtOffset(currentListeningOffset)
        assertThat(audiobook.chapters[0]).isEqualTo(chapter)
    }
}