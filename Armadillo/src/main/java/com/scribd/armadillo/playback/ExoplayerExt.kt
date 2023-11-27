package com.scribd.armadillo.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.hls.HlsManifest
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import com.scribd.armadillo.Milliseconds
import com.scribd.armadillo.time.milliseconds

/**
 * Call this method before queries to player progress.
 * During setup, [ExoPlayer.getCurrentManifest] will be null.
 */
@OptIn(UnstableApi::class)
internal fun ExoPlayer.hasProgressAvailable(): Boolean {
    return (isPlayingHls() && (currentManifest as HlsManifest).mediaPlaylist.durationUs != C.TIME_UNSET)
        ||
        (currentManifest == null && !currentTimeline.isEmpty)
}

@OptIn(UnstableApi::class)
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
@OptIn(UnstableApi::class)
internal fun createExoplayerInstance(context: Context, attributes: AudioAttributes): ExoPlayer =
    ExoPlayer.Builder(context, createRenderersFactory(context))
        .build().apply {
            setAudioAttributes(attributes, true)
        }

@OptIn(UnstableApi::class)
internal fun createRenderersFactory(context: Context): RenderersFactory =
    RenderersFactory { eventHandler, _, audioRendererEventListener, _, _ ->
        // Default audio sink taken from DefaultRenderersFactory. We need to provide it in order to enable offloading
        // Note that we need to provide a new audio sink for each call - playback fails if we reuse the sink
        val audioSink = DefaultAudioSink.Builder(context)
            .setAudioProcessorChain(DefaultAudioSink.DefaultAudioProcessorChain())
            .setEnableFloatOutput(false)
            .setEnableAudioTrackPlaybackParams(true)
            .build()

        arrayOf(MediaCodecAudioRenderer(context, MediaCodecSelector.DEFAULT, eventHandler, audioRendererEventListener, audioSink))
    }