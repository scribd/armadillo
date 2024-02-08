package com.scribd.armadillo.error

import com.scribd.armadillo.actions.Action

sealed class ArmadilloException(exception: Exception) : Exception(exception) {
    abstract val errorCode: Int
}

/**
 * Errors in Armadillo
 */
data class EngineNotInitialized(val reason: String) : ArmadilloException(exception = Exception(reason)) {
    override val errorCode = 100
}

object TransportControlsNull : ArmadilloException(Exception()) {
    override val errorCode = 101
}

data class MissingDataException(val reason: String) : ArmadilloException(exception = Exception(reason)) {
    override val errorCode = 102
}

data class ActionBeforeSetup(val action: Action) : ArmadilloException(exception = Exception(action.name)) {
    override val errorCode = 103
}

data class UnrecognizedAction(val action: Action) : ArmadilloException(exception = Exception(action.name)) {
    override val errorCode = 104
}

object NoPlaybackInfo : ArmadilloException(Exception()) {
    override val errorCode = 105
}

object InvalidPlaybackState : ArmadilloException(Exception()) {
    override val errorCode = 106
}

data class InvalidRequest(val reason: String) : ArmadilloException(exception = Exception(reason)) {
    override val errorCode: Int = 107
}

/**
 * Playback Errors
 */
data class HttpResponseCodeException(val responseCode: Int, val url: String?, val exception: Exception) : ArmadilloException(exception) {
    override val errorCode: Int = 200
}

data class ArmadilloIOException(val exception: Exception) : ArmadilloException(exception) {
    override val errorCode = 201
}

data class UpdateProgressFailureException(val exception: Exception) : ArmadilloException(exception) {
    override val errorCode = 202
}

data class PlaybackStartFailureException(val exception: Exception) : ArmadilloException(exception) {
    override val errorCode = 203
}

data class ActionListenerException(val exception: Exception) : ArmadilloException(exception) {
    override val errorCode = 204
}

object IncorrectChapterMetadataException : ArmadilloException(Exception()) {
    override val errorCode = 205
}

/**
 * Download Errors
 */
data class MissingInfoDownloadException(val reason: String) : ArmadilloException(exception = Exception(reason)) {
    override val errorCode = 301
}

object DownloadFailed : ArmadilloException(Exception()) {
    override val errorCode = 302
}

object UnableToLoadDownloadInfo : ArmadilloException(Exception()) {
    override val errorCode = 303
}

/**
 * Occurs when a device supporting Android 12 (SDK 31+) attempts to launch the ExoPlayer DownloadService while the user has put the app
 * into the background, often during low connectivity when the DownloadHelper.prepare() stalls long enough for the user to put the app in
 * the background before DownloadService.sendAddDownload
 */

data class DownloadServiceLaunchedInBackground(val id: Int) : ArmadilloException(Exception()) {
    override val errorCode = 304
}

/**
 * Misc Errors
 */
data class UnexpectedException(val exception: Exception) : ArmadilloException(exception) {
    constructor(reason: String) : this(Exception(reason))

    override val errorCode = 400
}

/** Media Browse Errors */
data class BadMediaHierarchyException(val reason: String) : ArmadilloException(exception = Exception(reason)) {
    override val errorCode: Int = 501
}

/**
 * Audio Renderer Errors
 */

data class RendererConfigurationException(val exception: Exception) : ArmadilloException(exception) {
    override val errorCode: Int = 601
}

data class RendererInitializationException(val exception: Exception) : ArmadilloException(exception) {
    override val errorCode: Int = 602
}

data class RendererWriteException(val exception: Exception) : ArmadilloException(exception) {
    override val errorCode: Int = 603
}

data class UnknownRendererException(val exception: Exception) : ArmadilloException(exception) {
    override val errorCode: Int = 604
}

/**
 * DRM errors
 */

data class DrmContentTypeUnsupportedException(val contentType: Int) : ArmadilloException(exception = Exception()) {
    override val errorCode = 700
}

data class DrmDownloadException(val exception: Exception) : ArmadilloException(exception) {
    override val errorCode = 701
}

