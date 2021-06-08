package com.scribd.armadillo

import com.scribd.armadillo.models.AudioPlayable
import com.scribd.armadillo.time.milliseconds

/**
 * Used to specify various settings when starting playback on a new [AudioPlayable]
 *
 * @property initialOffset The initial position to begin playback from.
 * @property isAutoPlay Flag to begin playback as soon as the content is loaded.
 * @property maxDurationDiscrepancy Armadillo will output errors if the metadata for the audio duration doesn't match the
 * actual duration of playback. This value can be used to set the allowed maximum difference in seconds between stated vs. actual duration.
 * Can also be set to a negative value to ignore any discrepancies.
 */
data class ArmadilloConfiguration(val initialOffset: Milliseconds = 0.milliseconds,
                                  val isAutoPlay: Boolean = true,
                                  val maxDurationDiscrepancy: Int = MAX_DISCREPANCY_DEFAULT) {

    companion object {
        // Default duration discrepancy values in seconds
        const val MAX_DISCREPANCY_DEFAULT = 1
        const val MAX_DISCREPANCY_DISABLE = -1
    }

}