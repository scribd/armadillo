package com.scribd.armadillo.playback

import android.content.Context
import android.util.Log
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.audio.AudioCapabilities
import com.google.android.exoplayer2.audio.DefaultAudioSink
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.source.dash.manifest.DashManifest
import com.google.android.exoplayer2.source.hls.HlsManifest
import com.scribd.armadillo.Milliseconds
import com.scribd.armadillo.time.milliseconds

/**
 * Call this method before queries to player progress.
 * During setup, [ExoPlayer.getCurrentManifest] will be null.
 */
internal fun ExoPlayer.hasProgressAvailable(): Boolean {
    return when (val m = currentManifest) {
        is HlsManifest -> m.mediaPlaylist.durationUs != C.TIME_UNSET
        is DashManifest -> m.durationMs != C.TIME_UNSET
        else -> m == null && !currentTimeline.isEmpty
    }
}

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
internal fun createExoplayerInstance(context: Context, attributes: AudioAttributes): ExoPlayer {
    val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            20000,
            120 * 60 * 1000, // 2hrs??
            5000,
            5000,
        )
        .build()

    Log.e("ExoplayerExt", "++++ USING LOAD CONTROL 2hrs")
    return ExoPlayer.Builder(context, createRenderersFactory(context))
        .setLoadControl(loadControl)
        .build().apply {
            setAudioAttributes(attributes, true)
            addAnalyticsListener(ArmadilloAnalyticsListener())
        }
}
internal fun createRenderersFactory(context: Context): RenderersFactory =
    RenderersFactory { eventHandler, _, audioRendererEventListener, _, _ ->
        // Default audio sink taken from DefaultRenderersFactory. We need to provide it in order to enable offloading
        // Note that we need to provide a new audio sink for each call - playback fails if we reuse the sink
        val audioSink = DefaultAudioSink.Builder()
            .setAudioCapabilities(AudioCapabilities.getCapabilities(context))
            .setAudioProcessorChain(DefaultAudioSink.DefaultAudioProcessorChain())
            .setEnableFloatOutput(false)
            .setEnableAudioTrackPlaybackParams(true)
            .setOffloadMode(DefaultAudioSink.OFFLOAD_MODE_DISABLED)
            .build()
        arrayOf(MediaCodecAudioRenderer(context, MediaCodecSelector.DEFAULT, eventHandler, audioRendererEventListener, audioSink))
    }