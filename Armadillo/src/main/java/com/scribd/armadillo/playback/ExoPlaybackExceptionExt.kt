package com.scribd.armadillo.playback

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.ExoPlaybackException.TYPE_RENDERER
import androidx.media3.exoplayer.ExoPlaybackException.TYPE_SOURCE
import androidx.media3.exoplayer.audio.AudioSink
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

@OptIn(UnstableApi::class)
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
                else -> ArmadilloIOException(this)
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
        UnexpectedException(this)
    }
}