package com.scribd.armadillo.playback

import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlaybackException.TYPE_RENDERER
import com.google.android.exoplayer2.ExoPlaybackException.TYPE_SOURCE
import com.google.android.exoplayer2.audio.AudioSink
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.scribd.armadillo.error.ArmadilloException
import com.scribd.armadillo.error.ArmadilloIOException
import com.scribd.armadillo.error.HttpResponseCodeException
import com.scribd.armadillo.error.RendererConfigurationException
import com.scribd.armadillo.error.RendererInitializationException
import com.scribd.armadillo.error.RendererWriteException
import com.scribd.armadillo.error.UnexpectedException
import com.scribd.armadillo.error.UnknownRendererException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

internal fun ExoPlaybackException.toArmadilloException(): ArmadilloException {
    return if (TYPE_SOURCE == type) {
        return this.sourceException.let { source ->
            when (source) {
                is HttpDataSource.InvalidResponseCodeException ->
                    HttpResponseCodeException(source.responseCode, source.dataSpec.uri.toString(), source)
                is HttpDataSource.HttpDataSourceException ->
                    HttpResponseCodeException(0, source.dataSpec.uri.toString(), source)
                is SocketTimeoutException -> HttpResponseCodeException(0, null, source)
                is UnknownHostException ->
                    HttpResponseCodeException(0, source.message, source) // Message is supposed to be the host for UnknownHostException
                else -> ArmadilloIOException(cause = this, whatActionFailedMessage = "Exoplayer error.")
            }
        }
    } else if (TYPE_RENDERER == type) {
        return this.cause.let { source ->
            when (source) {
                is AudioSink.ConfigurationException -> RendererConfigurationException(this)
                is AudioSink.InitializationException -> RendererInitializationException(this)
                is AudioSink.WriteException -> RendererWriteException(this)
                else -> UnknownRendererException(this)
            }
        }
    } else {
        UnexpectedException(cause = this, actionThatFailedMessage = "Exoplayer error")
    }
}