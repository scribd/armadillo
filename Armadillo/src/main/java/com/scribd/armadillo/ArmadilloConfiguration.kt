package com.scribd.armadillo

import com.google.android.exoplayer2.DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
import com.google.android.exoplayer2.DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS
import com.google.android.exoplayer2.DefaultLoadControl.DEFAULT_MIN_BUFFER_MS
import com.google.android.exoplayer2.DefaultLoadControl.DEFAULT_MAX_BUFFER_MS
import com.google.android.exoplayer2.DefaultLoadControl.DEFAULT_PRIORITIZE_TIME_OVER_SIZE_THRESHOLDS
import com.google.android.exoplayer2.DefaultLoadControl.DEFAULT_TARGET_BUFFER_BYTES
import com.scribd.armadillo.models.AudioPlayable
import com.scribd.armadillo.time.milliseconds
import java.io.Serializable

/**
 * Used to specify various settings when starting playback on a new [AudioPlayable]
 *
 * @property initialOffset The initial position to begin playback from.
 * @property isAutoPlay Flag to begin playback as soon as the content is loaded.
 * @property maxDurationDiscrepancy Armadillo will output errors if the metadata for the audio duration doesn't match the
 * actual duration of playback. This value can be used to set the allowed maximum difference in seconds between stated vs. actual duration.
 * Can also be set to a negative value to ignore any discrepancies.
 * @property minBufferMs The minumum amount of audio attempted to be buffered at all times in milliseconds.
 * @property maxBufferMs The maximum amount of audio attempted to be buffered at all times in milliseconds.
 * @property bufferForPlaybackMs The duration of media that must be buffered for playback to start or
 * resume following a user action such as a seek, in milliseconds.
 * @property bufferForPlaybackAfterRebufferMs The duration of media that must be buffered for playback
 * to resume after a rebuffer, in milliseconds. A rebuffer is defined to be caused by buffer depletion
 * rather than a user action.
 * @property targetBufferSize The desired size of the media buffer in bytes. An unset buffer size will
 * will be calculated based on the selected tracks.
 * @property prioritizeTimeOverSizeThresholds Whether the load control prioritizes buffer time constraints
 * over buffer size constraints.
 */
data class ArmadilloConfiguration(val initialOffset: Milliseconds = 0.milliseconds,
                                  val isAutoPlay: Boolean = true,
                                  val maxDurationDiscrepancy: Int = MAX_DISCREPANCY_DEFAULT,
                                  val minBufferMs: Int = DEFAULT_MIN_BUFFER_MS,
                                  val maxBufferMs: Int = DEFAULT_MAX_BUFFER_MS,
                                  val bufferForPlaybackMs: Int = DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                                  val bufferForPlaybackAfterRebufferMs: Int = DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
                                  val targetBufferSize: Int = DEFAULT_TARGET_BUFFER_BYTES,
                                  val prioritizeTimeOverSizeThresholds: Boolean = DEFAULT_PRIORITIZE_TIME_OVER_SIZE_THRESHOLDS): Serializable {

    companion object {
        // Default duration discrepancy values in seconds
        const val MAX_DISCREPANCY_DEFAULT = 1
        const val MAX_DISCREPANCY_DISABLE = -1
    }

}