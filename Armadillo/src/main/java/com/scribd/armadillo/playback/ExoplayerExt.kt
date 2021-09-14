package com.scribd.armadillo.playback

import android.content.Context
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.audio.AudioCapabilities
import com.google.android.exoplayer2.audio.DefaultAudioSink
import com.google.android.exoplayer2.audio.DefaultAudioSink.DefaultAudioProcessorChain
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.source.hls.HlsManifest
import com.scribd.armadillo.Milliseconds
import com.scribd.armadillo.time.milliseconds

/**
 * Call this method before queries to player progress.
 * During setup, [ExoPlayer.getCurrentManifest] will be null.
 */
internal fun ExoPlayer.hasProgressAvailable(): Boolean {
    return (isPlayingHls() && (currentManifest as HlsManifest).mediaPlaylist.durationUs != C.TIME_UNSET)
        ||
        (currentManifest == null && !currentTimeline.isEmpty)
}

internal fun ExoPlayer.isPlayingHls(): Boolean = currentManifest is HlsManifest

/**
 * Current position in relation to all audio files.
 */
internal fun ExoPlayer.currentPositionInDuration(): Milliseconds = currentPosition.milliseconds

/**
 * The total duration reported by the player, or null if it is not yet known
 */
internal fun ExoPlayer.playerDuration(): Milliseconds? = if (duration == C.TIME_UNSET) { null } else { duration.milliseconds }

/**
 * builds [ExoPlayer] instance to be used all across [ExoplayerPlaybackEngine].
 *
 * We provide our own renderers factory so that Proguard can remove any non-audio rendering code.
 */
internal fun createExoplayerInstance(context: Context, attributes: AudioAttributes): ExoPlayer =
    SimpleExoPlayer.Builder(context, createRenderersFactory(context))
        .build().apply {
            setAudioAttributes(attributes, true)
        }

internal fun createRenderersFactory(context: Context): RenderersFactory =
    RenderersFactory { eventHandler, _, audioRendererEventListener, _, _ ->
        // Default audio sink taken from DefaultRenderersFactory. We need to provide it in order to enable offloading
        // Note that we need to provide a new audio sink for each call - playback fails if we reuse the sink
        val audioSink = DefaultAudioSink(
            AudioCapabilities.getCapabilities(context),
            DefaultAudioProcessorChain(),
            false,
            true,
            DefaultAudioSink.OFFLOAD_MODE_DISABLED)
        arrayOf(MediaCodecAudioRenderer(context, MediaCodecSelector.DEFAULT, eventHandler, audioRendererEventListener, audioSink))
    }