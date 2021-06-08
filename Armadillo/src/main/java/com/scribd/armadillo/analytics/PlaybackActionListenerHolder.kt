package com.scribd.armadillo.analytics

import com.scribd.armadillo.playback.PlaybackService


internal object PlaybackActionListenerHolder {
    var actionlisteners = mutableListOf<PlaybackActionListener>()
    var stateListener : PlaybackService.PlaybackStateListener? = null
}