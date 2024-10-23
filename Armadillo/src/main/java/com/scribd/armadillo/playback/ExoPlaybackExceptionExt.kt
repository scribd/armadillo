package com.scribd.armadillo.playback

import android.content.Context
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlaybackException.TYPE_RENDERER
import com.google.android.exoplayer2.ExoPlaybackException.TYPE_SOURCE
import com.google.android.exoplayer2.ParserException
import com.google.android.exoplayer2.audio.AudioSink
import com.google.android.exoplayer2.drm.DrmSession.DrmSessionException
import com.google.android.exoplayer2.drm.MediaDrmCallbackException
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.scribd.armadillo.error.ArmadilloException
import com.scribd.armadillo.error.ArmadilloIOException
import com.scribd.armadillo.error.ConnectivityException
import com.scribd.armadillo.error.DrmPlaybackException
import com.scribd.armadillo.error.HttpResponseCodeException
import com.scribd.armadillo.error.ParsingException
import com.scribd.armadillo.error.RendererConfigurationException
import com.scribd.armadillo.error.RendererInitializationException
import com.scribd.armadillo.error.RendererWriteException
import com.scribd.armadillo.error.UnexpectedException
import com.scribd.armadillo.error.UnknownRendererException
import com.scribd.armadillo.isInternetAvailable
import java.net.SocketTimeoutException
import java.net.UnknownHostException

internal fun ExoPlaybackException.toArmadilloException(context: Context): ArmadilloException {
    return if (TYPE_SOURCE == type) {
        return this.sourceException.let { source ->
            when (source) {
                is HttpDataSource.InvalidResponseCodeException ->
                    HttpResponseCodeException(source.responseCode, source.dataSpec.uri.toString(), source, source.dataSpec.toAnalyticsMap
                    (context))

                is HttpDataSource.HttpDataSourceException ->
                    HttpResponseCodeException(source.reason, source.dataSpec.uri.toString(), source, source.dataSpec.toAnalyticsMap(context))

                is MediaDrmCallbackException -> {
                    val httpCause = source.cause as? HttpDataSource.InvalidResponseCodeException
                    HttpResponseCodeException(httpCause?.responseCode
                        ?: 0, httpCause?.dataSpec?.uri.toString(), source, source.dataSpec.toAnalyticsMap(context))
                }
                is DrmSessionException -> {
                    DrmPlaybackException(cause = this)
                }

                is UnknownHostException,
                is SocketTimeoutException -> ConnectivityException(source)

                else -> {
                    var cause: Throwable? = source
                    while (source.cause != null && cause !is ParserException) {
                        cause = source.cause
                    }
                    when (cause) {
                        is ParserException -> ParsingException(cause = this)
                        else -> ArmadilloIOException(cause = this, actionThatFailedMessage = "Exoplayer error.")
                    }
                }
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

private fun DataSpec.toAnalyticsMap(context: Context): Map<String, String> {
    return mapOf(
        "uri" to uri.toString(),
        "uriPositionOffset" to uriPositionOffset.toString(),
        "httpMethod" to httpMethod.toString(),
        "position" to position.toString(),
        "length" to length.toString(),
        "key" to key.toString(),
        "flags" to flags.toString(),
        "customData" to customData.toString(),
        "isInternetConnected" to isInternetAvailable(context).toString(),
    )
}