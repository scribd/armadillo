package com.scribd.armadillo

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PRIVATE
import com.scribd.armadillo.actions.Action
import com.scribd.armadillo.actions.ErrorAction
import com.scribd.armadillo.actions.MediaRequestUpdateAction
import com.scribd.armadillo.actions.MetadataUpdateAction
import com.scribd.armadillo.actions.SkipDistanceAction
import com.scribd.armadillo.analytics.PlaybackActionListener
import com.scribd.armadillo.analytics.PlaybackActionListenerHolder
import com.scribd.armadillo.analytics.PlaybackActionTransmitter
import com.scribd.armadillo.di.Injector
import com.scribd.armadillo.download.CacheManager
import com.scribd.armadillo.download.DownloadEngine
import com.scribd.armadillo.error.EngineNotInitialized
import com.scribd.armadillo.error.InvalidPlaybackState
import com.scribd.armadillo.error.NoPlaybackInfo
import com.scribd.armadillo.error.PlaybackStartFailureException
import com.scribd.armadillo.error.TransportControlsNull
import com.scribd.armadillo.error.UnexpectedException
import com.scribd.armadillo.error.UpdateProgressFailureException
import com.scribd.armadillo.extensions.CustomAction
import com.scribd.armadillo.extensions.sendCustomAction
import com.scribd.armadillo.extensions.toUri
import com.scribd.armadillo.mediaitems.ArmadilloMediaBrowse
import com.scribd.armadillo.models.ArmadilloState
import com.scribd.armadillo.models.AudioPlayable
import com.scribd.armadillo.models.Chapter
import com.scribd.armadillo.models.PlaybackState
import com.scribd.armadillo.playback.AudioPlaybackEngine
import com.scribd.armadillo.playback.MediaSessionConnection
import com.scribd.armadillo.time.milliseconds
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * The client will use [ArmadilloPlayer] to communicate with the [ArmadilloPlayerChoreographer].
 *
 * The [DownloadEngine] must be initialized for every instance. The [AudioPlaybackEngine] is optional.
 *
 * The methods allow the client to control playback & downloading
 */
interface ArmadilloPlayer {
    var skipDistance: Milliseconds
    var playbackSpeed: Float
    var isInForeground: Boolean

    val downloadCacheSize: Long
    val playbackCacheSize: Long

    fun initDownloadEngine(): ArmadilloPlayer
    fun beginDownload(audioPlayable: AudioPlayable)
    fun removeDownload(audioPlayable: AudioPlayable)
    fun removeAllDownloads()
    fun clearCache()

    /**
     * Starts playback with the given [AudioPlayable], allowing for configuration through a given [ArmadilloConfiguration]
     */
    fun beginPlayback(audioPlayable: AudioPlayable, config: ArmadilloConfiguration = ArmadilloConfiguration())

    /**
     * Provide new media request data to the currently playing content, such as request headers or DRM updates.
     * It is an error to call this with a different URL from the currently playing media.
     * It is also an error to call this when no content is currently loaded.
     * If you want to start playback from a new URL, use [beginPlayback].
     */
    fun updateMediaRequest(mediaRequest: AudioPlayable.MediaRequest)

    /**
     * Update the metadata of the currently playing content
     */
    fun updatePlaybackMetadata(title: String, chapters: List<Chapter>)

    fun endPlayback()
    fun deinit()
    fun playOrPause()
    fun nextChapter()
    fun previousChapter()
    fun skipForward()
    fun skipBackward()

    /**
     * [position] in relation to the beginning of the [AudioPlayable]
     */
    fun seekTo(position: Milliseconds)

    /**
     * [percent] percent in the current chapter. eg. (0 = beginning, 100 = end)
     */
    fun seekWithinChapter(percent: Int)

    fun addPlaybackActionListener(listener: PlaybackActionListener)

    /**
     * subscribers should subscribe to [armadilloStateObservable] for state updates ~ every 500ms
     */
    val armadilloStateObservable: Observable<ArmadilloState>

    /** Permits Armadillo player to publish MediaItems for the Media Browse framework, allowing external clients to use this audio player
    with content from your app.
     * @param browseController - callback for the choices the external client requests. */
    fun enableExternalMediaBrowsing(browseController: MediaBrowseController)

    /** removes the media browse controller associated with armadillo */
    fun disableExternalMediaBrowsing()

    /** Tells external clients using the Media Browser that content has changed and that they should reload.
     * @param rootId - specifies which parent should refresh its content */
    fun notifyMediaBrowseContentChanged(rootId: String)

    /** Communication between Armadillo and its user to inform the user that a new content id is requested to play.
     * Armadillo will not automatically play the content - the user must handle the content and any of its preconditions themselves and
     * then call [ArmadilloPlayer.beginPlayback] with the new content id. */
    interface MediaBrowseController {
        val root: MediaBrowserCompat.MediaItem?

        /** Return true if this client can connect to this media browser */
        fun authorizeExternalClient(client: ArmadilloMediaBrowse.ExternalClient): Boolean

        /** Return true if the content can be translated into an AudioPlayable, false if it represents a browsable branch of the content
         * hierarchy. */
        fun isItemPlayable(mediaId: String): Boolean

        fun getItemFromId(mediaId: String): MediaBrowserCompat.MediaItem?

        /** the external client wishes to play the given media item. Retrieve the [AudioPlayable] content associated with
         *  the media ID and return if it exists. */
        fun onContentToPlaySelected(mediaId: String, playImmediately: Boolean): MediaBrowserCompat.MediaItem?

        /** The external content is requesting content items that belong to the given browsable category. Collect the child content of
         * the given media item, and then call [showMediaBrowseCategory] */
        fun onChildrenOfCategoryRequested(parentId: String): List<MediaBrowserCompat.MediaItem>

        /** The external client is requesting a content item from a search. Collect the appropriate title to play and return it if it
         * exists */
        fun onContentSearchToPlaySelected(query: String?, playImmediately: Boolean): MediaBrowserCompat.MediaItem?

        /** The external client is sending an authorization status. If the status is
         * [ArmadilloMediaBrowse.Browser.AuthorizationStatus.Unauthorized] an error message is sent to be displayed on Android Auto
         * screen. */
        fun authorizationStatus(): ArmadilloMediaBrowse.Browser.AuthorizationStatus
    }
}

/**
 * Controls, decision making, & command center for the application.
 */
internal class ArmadilloPlayerChoreographer : ArmadilloPlayer {

    init {
        Injector.mainComponent.inject(this)
    }

    override var skipDistance = Constants.AUDIO_SKIP_DURATION
        set(value) {
            field = value
            doWhenPlaybackReady {
                stateModifier.dispatch(SkipDistanceAction(field))
            }
        }

    override var playbackSpeed: Float = Constants.DEFAULT_PLAYBACK_SPEED
        set(value) {
            field = value
            doWhenPlaybackReady {
                it.sendCustomAction(CustomAction.SetPlaybackSpeed(playbackSpeed))
            }
        }

    override var isInForeground: Boolean = false
        set(value) {
            field = value
            doWhenPlaybackReady {
                it.sendCustomAction(CustomAction.SetIsInForeground(isInForeground))
            }
        }

    @Inject
    internal lateinit var context: Context

    @Inject
    internal lateinit var downloadEngine: DownloadEngine

    @Inject
    internal lateinit var cacheManager: CacheManager

    @Inject
    internal lateinit var stateProvider: StateStore.Provider

    @Inject
    internal lateinit var stateModifier: StateStore.Modifier

    @Inject
    internal lateinit var actionTransmitter: PlaybackActionTransmitter

    @Inject
    internal lateinit var mediaContentSharer: ArmadilloMediaBrowse.ContentSharer

    companion object {
        private const val TAG = "ArmadilloChoreographer"
        private val observerPollIntervalMillis = 500.milliseconds

        @VisibleForTesting(otherwise = PRIVATE)
        val handlerThread: HandlerThread by lazy {
            HandlerThread("ArmadilloChoreographer")
                .also { it.start() }
        }

        @VisibleForTesting(otherwise = PRIVATE)
        val handler: Handler by lazy { Handler(handlerThread.looper) }
    }

    override val playbackCacheSize: Long
        get() = cacheManager.playbackCacheSize
    override val downloadCacheSize: Long
        get() = cacheManager.downloadCacheSize

    /**
     * emits the most recently emitted state and all the subsequent states when an observer subscribes to it.
     */
    val armadilloStateSubject: BehaviorSubject<ArmadilloState>
        get() = stateProvider.stateSubject
    override val armadilloStateObservable: Observable<ArmadilloState>
        get() = armadilloStateSubject.observeOn(AndroidSchedulers.mainThread())

    private val pollingInterval =
        Observable.interval(observerPollIntervalMillis.longValue, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())

    private val disposables = CompositeDisposable()

    private var pollingSubscription: Disposable? = null

    private var isDownloadEngineInit = false

    /**
     * Used to delegates all actions to the player. ex. [MediaControllerCompat.TransportControls.play]
     */
    @VisibleForTesting
    internal var playbackConnection: MediaSessionConnection? = null

    private fun runHandler(lambda: () -> Unit) {
        handler.post { lambda() }
    }

    private fun runIfPlaybackReady(lambda: (controls: MediaControllerCompat.TransportControls, playbackState: PlaybackState) -> Unit) {
        runHandler {
            doIfPlaybackReady { controls, playbackState ->
                lambda(controls, playbackState)
            }
        }
    }

    override fun initDownloadEngine(): ArmadilloPlayer {
        isDownloadEngineInit = true
        downloadEngine.init()
        updateProgressPollTask()
        return this
    }

    override fun beginDownload(audioPlayable: AudioPlayable) = runHandler {
        if (!isDownloadEngineInit) {
            stateModifier.dispatch(ErrorAction(EngineNotInitialized("download engine cannot start download.")))
        } else {
            downloadEngine.download(audioPlayable)
        }
    }

    override fun clearCache() = runHandler {
        cacheManager.clearPlaybackCache()
    }

    override fun removeAllDownloads() = runHandler {
        if (!isDownloadEngineInit) {
            stateModifier.dispatch(ErrorAction(EngineNotInitialized("Cannot remove all the downloads.")))
        } else {
            downloadEngine.removeAllDownloads()
        }
    }

    override fun beginPlayback(audioPlayable: AudioPlayable, config: ArmadilloConfiguration) = runHandler {
        disposables.clear()
        actionTransmitter.begin(observerPollIntervalMillis)
        val mediaSessionConnection = MediaSessionConnection(context)
        playbackConnection = mediaSessionConnection

        mediaSessionConnection.connectToMediaSession(object : MediaSessionConnection.Listener {
            override fun onConnectionCallback(transportControls: MediaControllerCompat.TransportControls) {
                val bundle = Bundle()
                bundle.putSerializable(Constants.Keys.KEY_ARMADILLO_CONFIG, config)
                bundle.putSerializable(Constants.Keys.KEY_AUDIO_PLAYABLE, audioPlayable)
                transportControls.playFromUri(audioPlayable.request.url.toUri(), bundle)
                updateProgressPollTask()
            }
        })
    }

    override fun updateMediaRequest(mediaRequest: AudioPlayable.MediaRequest) =
        runIfPlaybackReady { controls, _ ->
            stateModifier.dispatch(MediaRequestUpdateAction(mediaRequest))
            controls.sendCustomAction(CustomAction.UpdateMediaRequest(mediaRequest))
        }

    override fun updatePlaybackMetadata(title: String, chapters: List<Chapter>) =
        doWhenPlaybackReady { controls ->
            stateModifier.dispatch(MetadataUpdateAction(title, chapters))
            controls.sendCustomAction(CustomAction.UpdatePlaybackMetadata(title, chapters))
        }

    override fun endPlayback() = runHandler {
        playbackConnection?.transportControls?.stop()
    }

    override fun deinit() = runHandler {
        Log.v(TAG, "deinit")
        disposables.clear()
        pollingSubscription = null
        isDownloadEngineInit = false
        actionTransmitter.destroy()
    }

    override fun playOrPause() = runIfPlaybackReady { controls, playbackState ->
        when (playbackState) {
            PlaybackState.PLAYING -> controls.pause()
            PlaybackState.PAUSED -> controls.play()
            else -> {
                stateModifier.dispatch(ErrorAction(
                    UnexpectedException(cause = IllegalStateException("Neither playing nor paused"),
                        actionThatFailedMessage = "Trying to play or pause media."))
                )
            }
        }
    }

    // Note: chapter skip and jump-skip behaviours are swapped. See MediaSessionCallback - we are using a jump-skip for skip-forward, as
    // most headphones only have a skip-forward button, and this is the ideal behaviour for spoken audio.
    override fun nextChapter() = runIfPlaybackReady { controls, _ -> controls.fastForward() }

    override fun previousChapter() = runIfPlaybackReady { controls, _ -> controls.rewind() }

    override fun skipForward() = runIfPlaybackReady { controls, _ -> controls.skipToNext() }

    override fun skipBackward() = runIfPlaybackReady { controls, _ -> controls.skipToPrevious() }

    override fun seekTo(position: Milliseconds) = runIfPlaybackReady { controls, _ ->
        // Add a shift constant to all seeks originating from the client application
        // as opposed to system originated, such as from notification
        controls.seekTo(position.longValue + Constants.AUDIO_POSITION_SHIFT_IN_MS)
    }

    override fun seekWithinChapter(percent: Int) = runHandler {
        val position = stateProvider.currentState.positionFromChapterPercent(percent)
            ?: run {
                stateModifier.dispatch(ErrorAction(
                    UnexpectedException(cause = KotlinNullPointerException("Current state's position is null"),
                        actionThatFailedMessage = "seeking within chapter"))
                )
                return@runHandler
            }
        seekTo(position)
    }

    override fun removeDownload(audioPlayable: AudioPlayable) = runHandler {
        if (!isDownloadEngineInit) {
            stateModifier.dispatch(ErrorAction(EngineNotInitialized("Cannot remove a download.")))
        } else {
            downloadEngine.removeDownload(audioPlayable)
        }
    }

    override fun addPlaybackActionListener(listener: PlaybackActionListener) = runHandler {
        PlaybackActionListenerHolder.actionlisteners.add(listener)
    }

    override fun enableExternalMediaBrowsing(browseController: ArmadilloPlayer.MediaBrowseController) {
        mediaContentSharer.isBrowsingEnabled = true
        mediaContentSharer.browseController = browseController
    }

    override fun disableExternalMediaBrowsing() {
        mediaContentSharer.isBrowsingEnabled = false
        mediaContentSharer.browseController = null
    }

    override fun notifyMediaBrowseContentChanged(rootId: String) = runHandler { mediaContentSharer.notifyContentChanged(rootId) }

    /**
     * [ArmadilloPlayerChoreographer] polls for updates of playback & downloading
     *
     * For playback:
     *  - uses transport controls to tell the [AudioPlaybackEngine] to update [MediaSessionCompat] with an updated [PlaybackStateCompat]
     *  - then queries this updated state object from the [MediaControllerCompat]
     *  - then converts [PlaybackStateCompat] into [Action] which is dispatched to [ArmadilloPlayerChoreographer]
     *
     *  For downloading:
     *   - [DownloadEngine] uses [StateStore.Modifier] to send actions to [ArmadilloPlayerChoreographer]
     */
    private fun updateProgressPollTask() {
        pollingSubscription?.let {
            disposables.remove(it)
        }
        pollingSubscription = pollingInterval
            .doFinally {
                // When we end, send one last update
                playbackConnection?.mediaController?.transportControls?.sendCustomAction(CustomAction.UpdateProgress)
            }
            .retry()
            .subscribe({
                playbackConnection?.mediaController?.transportControls?.sendCustomAction(CustomAction.UpdateProgress)

                if (isDownloadEngineInit) {
                    downloadEngine.updateProgress()
                }
            }, { throwable ->
                (throwable as? Exception)?.let {
                    stateModifier.dispatch(ErrorAction(UpdateProgressFailureException(it)))
                }
            }).also {
                disposables.add(it)
            }
    }

    /**
     * This method purposely crashes aggressively right now for development purposes
     * in order to ensure that we understand the playback init process.
     */
    private fun doIfPlaybackReady(lambda: (controls: MediaControllerCompat.TransportControls, playbackState: PlaybackState) -> Unit) {
        val controls = playbackConnection?.transportControls
        val playbackState = stateProvider.currentState.playbackInfo?.playbackState
        val playbackReady = stateProvider.currentState.internalState.isPlaybackEngineReady

        if (controls != null && playbackState != null && PlaybackState.NONE != playbackState && playbackReady) {
            lambda.invoke(controls, playbackState)
        } else {
            val error = if (controls == null) {
                Log.e(TAG, "The transport controls are null. Are the controls initialized?")
                TransportControlsNull()
            } else if (playbackState == null) {
                Log.e(TAG, "The playbackState is null.")
                NoPlaybackInfo()
            } else if (PlaybackState.NONE == playbackState) {
                Log.e(TAG, "There is no playback State.")
                InvalidPlaybackState()
            } else {
                EngineNotInitialized("Playback is not ready.")
            }

            stateModifier.dispatch(ErrorAction(error))
        }
    }

    @VisibleForTesting
    internal fun doWhenPlaybackReady(lambda: (controls: MediaControllerCompat.TransportControls) -> Unit) {
        data class PlaybackReadyData(val transportControls: MediaControllerCompat.TransportControls?, val engineIsReady: Boolean)

        Log.d(TAG, "Waiting for engine before invoking callback")
        disposables.add(armadilloStateSubject
            .map {
                PlaybackReadyData(playbackConnection?.transportControls, it.internalState.isPlaybackEngineReady)
            }
            .filter {
                // Need controls set and engine ready
                it.transportControls != null && it.engineIsReady
            }
            .firstElement()
            .subscribe({
                // Deliver the desired command
                Log.i(TAG, "Engine is ready, Invoking callback")
                lambda.invoke(it.transportControls!!)
            }, { throwable ->
                (throwable as? Exception)?.let {
                    stateModifier.dispatch(ErrorAction(PlaybackStartFailureException(it)))
                }
            })
        )
    }
}