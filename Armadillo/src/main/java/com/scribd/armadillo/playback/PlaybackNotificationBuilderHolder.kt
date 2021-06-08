package com.scribd.armadillo.playback

/**
 * This class:
 *  - makes the client's [PlaybackNotificationBuilder] accessible to [PlaybackService]
 */
internal object PlaybackNotificationBuilderHolder {
    var builder: PlaybackNotificationBuilder? = null
}
