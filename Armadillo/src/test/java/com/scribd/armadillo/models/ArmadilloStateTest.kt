package com.scribd.armadillo.models

import com.scribd.armadillo.MockModels
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class ArmadilloStateTest {

    private lateinit var state: ArmadilloState
    private lateinit var currentChapter: Chapter
    @Before
    fun setUp() {
        state = MockModels.appState()
        val playbackInfo = state.playbackInfo!!
        val currentChapterIndex = playbackInfo.progress.currentChapterIndex
        currentChapter = playbackInfo.audioPlayable.chapters[currentChapterIndex]
    }

    @Test
    fun positionFromChapterPercent_atBeginning_returnsStart() {
        val position = state.positionFromChapterPercent(0)
        val expectedPosition = currentChapter.startTime

        assertThat(position).isEqualTo(expectedPosition)
    }

    @Test
    fun positionFromChapterPercent_atEnd_returnsEndOfChapter() {
        val position = state.positionFromChapterPercent(100)
        val expectedPosition = currentChapter.startTime + currentChapter.duration

        assertThat(position).isEqualTo(expectedPosition)
    }

    @Test
    fun positionFromChapterPercent_inMiddle_returnsMiddleOfChapter() {
        val position = state.positionFromChapterPercent(50)
        val expectedPosition = currentChapter.startTime + (currentChapter.duration / 2)

        assertThat(position).isEqualTo(expectedPosition)
    }
}