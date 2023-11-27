package com.scribd.armadillo.playback

import android.media.AudioAttributes
import androidx.media3.common.C

/**
 * Creates AudioAttributes - for ExoPlayer and also for Android notifications.
 *
 * [exoPlayerAttrs] is used in Simple Exo Player to define the attributes for playback.
 * ExoPlayer uses the info here to know how to handle audio focus.
 *
 * [getAndroidAttributes] is useful for declaring attributes for notifications.
 */
interface ArmadilloAudioAttributes {
    val exoPlayerAttrs: androidx.media3.common.AudioAttributes
    fun getAndroidAttributes(): AudioAttributes
}

class AudioAttributesBuilderImpl : ArmadilloAudioAttributes {
    override val exoPlayerAttrs: androidx.media3.common.AudioAttributes
        get() = androidx.media3.common.AudioAttributes.Builder().run {
            setUsage(C.USAGE_MEDIA)
            setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
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