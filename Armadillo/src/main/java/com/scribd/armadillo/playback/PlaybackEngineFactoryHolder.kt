package com.scribd.armadillo.playback

import com.scribd.armadillo.models.AudioPlayable

internal interface PlaybackEngineFactory {
    fun createPlaybackEngine(audioPlayable: AudioPlayable): AudioPlaybackEngine
}

internal object PlaybackEngineFactoryHolder {

    val factory = object : PlaybackEngineFactory {
        override fun createPlaybackEngine(audioPlayable: AudioPlayable): AudioPlaybackEngine = ExoplayerPlaybackEngine(audioPlayable)
    }
}