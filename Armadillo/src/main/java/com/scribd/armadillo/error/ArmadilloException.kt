package com.scribd.armadillo.error

import com.google.android.exoplayer2.upstream.HttpDataSource.HttpDataSourceException
import com.scribd.armadillo.actions.Action
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed class ArmadilloException(cause: Throwable? = null,
                                isNetworkRelatedError: Boolean = false,
                                message: String = "Unhandled Armadillo Error")
    : Exception(message, cause) {
    abstract val errorCode: Int
}

/**
 * Errors in Armadillo
 */
class EngineNotInitialized(message: String)
    : ArmadilloException(message = "Armadillo Engine is not initialized: $message") {
    override val errorCode = 100
}

class TransportControlsNull
    : ArmadilloException(message = "Internal error, the transport controls are null. Are the controls being initialized?") {
    override val errorCode = 101
}

class MissingDataException(message: String)
    : ArmadilloException(message = "Internal error, unexpectedly missing internal data: $message") {
    override val errorCode = 102
}

data class ActionBeforeSetup(val action: Action)
    : ArmadilloException(message = "Internal Error, using an Action before setup is finished: ${action.name}") {
    override val errorCode = 103
}

data class UnrecognizedAction(val action: Action)
    : ArmadilloException(message = "Internal error, unknown action being taken: ${action.name}") {
    override val errorCode = 104
}

class NoPlaybackInfo : ArmadilloException(message = "Internal error, playback info is missing. Is the player initialized?") {
    override val errorCode = 105
}

class InvalidPlaybackState
    : ArmadilloException(message = "Internal Error, playback state is missing. Has the player been initialized or destroyed?") {
    override val errorCode = 106
}

class InvalidRequest(message: String)
    : ArmadilloException(message = "Internal Error, certain media requests cannot be taken: $message") {
    override val errorCode: Int = 107
}

/**
 * Playback Errors
 */
data class HttpResponseCodeException(
    val responseCode: Int,
    val url: String?,
    override val cause: Exception,
    val extraData: Map<String, String> = emptyMap(),
) : ArmadilloException(cause = cause, message = "HTTP Error $responseCode.", isNetworkRelatedError = true) {
    override val errorCode: Int = 200
}

class ArmadilloIOException(cause: Exception, actionThatFailedMessage: String)
    : ArmadilloException(cause = cause, message = "A critical playback issue occurred: $actionThatFailedMessage") {
    override val errorCode = 201
}

class UpdateProgressFailureException(cause: Exception)
    : ArmadilloException(cause = cause, message = "Progress failed to update suddenly during playback. Download progress or playback " +
    "progress may not be reporting correctly.") {
    override val errorCode = 202
}

class PlaybackStartFailureException(cause: Exception)
    : ArmadilloException(cause = cause, message = "The player is initialized, but it failed to begin.") {
    override val errorCode = 203
}

class ActionListenerException(cause: Exception)
    : ArmadilloException(cause = cause, message = "A problem occurred processing state updates.") {
    override val errorCode = 204
}

class IncorrectChapterMetadataException
    : ArmadilloException(message = "The metadata for chapter durations are wrong and cannot be used for this content.") {
    override val errorCode = 205
}

class ConnectivityException(cause: Exception)
    : ArmadilloException(
    cause = cause,
    message = "Internet connection to remote is not reliable.",
    isNetworkRelatedError = true) {
    override val errorCode: Int = 206
}

/**
 * Download Errors
 */
class MissingInfoDownloadException(message: String)
    : ArmadilloException(message = "Download info is missing: $message") {
    override val errorCode = 301
}

class DownloadFailed(val extraData: Map<String, String>)
    : ArmadilloException(message = "The download has failed to finish.", isNetworkRelatedError = true) {
    override val errorCode = 302
}

class UnableToLoadDownloadInfo
    : ArmadilloException(message = "Failed to load download metadata, queued downloads may fail to resume.") {
    override val errorCode = 303
}

/**
 * Occurs when a device supporting Android 12 (SDK 31+) attempts to launch the ExoPlayer DownloadService while the user has put the app
 * into the background, often during low connectivity when the DownloadHelper.prepare() stalls long enough for the user to put the app in
 * the background before DownloadService.sendAddDownload
 */

data class DownloadServiceLaunchedInBackground(val id: Int, override val cause: Exception)
    : ArmadilloException(cause = cause,
    message = "Android 12 compliance problem: ${cause.message}") {
    override val errorCode = 304
}

class UnexpectedDownloadException(throwable: Throwable)
    : ArmadilloException(cause = throwable, message = "Unknown problem while downloading.", isNetworkRelatedError = throwable is HttpDataSourceException) {
    override val errorCode = 305
}

/**
 * Misc Errors
 */
class UnexpectedException(cause: Exception, actionThatFailedMessage: String)
    : ArmadilloException(cause = cause, message = "Unanticipated error: $actionThatFailedMessage.") {
    override val errorCode = 400
}

/** Media Browse Errors */
class BadMediaHierarchyException(cause: Exception?, message: String)
    : ArmadilloException(cause = cause, message = message) {
    override val errorCode: Int = 501
}

/**
 * Audio Renderer Errors
 */

class RendererConfigurationException(cause: Exception)
    : ArmadilloException(cause = cause, message = "The audio data buffer (AudioSink) has been misconfigured.") {
    override val errorCode: Int = 601
}

class RendererInitializationException(cause: Exception)
    : ArmadilloException(cause = cause, message = "The audio data buffer (AudioSink) isn't initialized.") {
    override val errorCode: Int = 602
}

class RendererWriteException(cause: Exception)
    : ArmadilloException(cause = cause, message = "Cannot write data into the audio data buffer (AudioSink).") {
    override val errorCode: Int = 603
}

class UnknownRendererException(cause: Exception)
    : ArmadilloException(cause = cause, message = "Unknown problem with the audio data buffer (AudioSink).") {
    override val errorCode: Int = 604
}

/**
 * DRM errors
 */

data class DrmContentTypeUnsupportedException(val contentType: Int)
    : ArmadilloException(message = "This DRM content type is not yet supported. Please consider opening a pull request.") {
    override val errorCode = 700
}

class DrmDownloadException(cause: Exception)
    : ArmadilloException(
    cause = cause,
    message = "Failed to process DRM license for downloading.",
    isNetworkRelatedError = (cause is HttpDataSourceException) || (cause is SocketTimeoutException) || (cause is UnknownHostException)) {
    override val errorCode = 701
}

class DrmPlaybackException(cause: Exception)
    : ArmadilloException(cause = cause, message = "Cannot play DRM content.") {
    override val errorCode = 702
}

