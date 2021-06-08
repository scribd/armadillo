package com.scribd.armadillo.playback

import android.media.AudioAttributes
import com.google.android.exoplayer2.C

/**
 * Creates AudioAttributes - for ExoPlayer and also for Android notifications.
 *
 * [exoPlayerAttrs] is used in Simple Exo Player to define the attributes for playback.
 * ExoPlayer uses the info here to know how to handle audio focus.
 *
 * [getAndroidAttributes] is useful for declaring attributes for notifications.
 */
interface ArmadilloAudioAttributes {
    val exoPlayerAttrs: com.google.android.exoplayer2.audio.AudioAttributes
    fun getAndroidAttributes(): AudioAttributes
}

class AudioAttributesBuilderImpl : ArmadilloAudioAttributes {
    override val exoPlayerAttrs: com.google.android.exoplayer2.audio.AudioAttributes
        get() = com.google.android.exoplayer2.audio.AudioAttributes.Builder().run {
            setUsage(C.USAGE_MEDIA)
            setContentType(C.CONTENT_TYPE_SPEECH)
            build()
        }

    override fun getAndroidAttributes(): AudioAttributes {
        return AudioAttributes.Builder().run {
            setUsage(AudioAttributes.USAGE_MEDIA)
            setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            build()
        }
    }
}