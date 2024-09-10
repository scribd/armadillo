package com.scribd.armadillo

import android.content.Context
import com.google.android.exoplayer2.util.Util
import com.scribd.armadillo.time.seconds

object Constants {
    const val LIBRARY_VERSION = BuildConfig.VERSION_NAME

    internal const val PLAYBACK_LOADING_STATUS = "playback_loading_status"

    internal const val DEFAULT_PLAYBACK_SPEED = 1.0f
    internal const val DEBUG_MAX_SIZE = 20

    internal const val MAX_PARALLEL_DOWNLOADS = 6

    // an arbitrarily long constant to add to seek positions from app UI
    internal const val AUDIO_POSITION_SHIFT_IN_MS = 5000000000L

    /**
     * A seek to previousChapter command beyond this will restart the current media source instead of skipping to the previousChapter media source
     */
    internal val MAX_POSITION_FOR_SEEK_TO_PREVIOUS = 3.seconds.inMilliseconds
    internal val AUDIO_SKIP_DURATION = 30.seconds.inMilliseconds

    internal fun getUserAgent(context: Context): String = Util.getUserAgent(context, context.getString(APP_NAME))

    private val APP_NAME = R.string.arm_app_name

    internal object Keys {
        const val KEY_ARMADILLO_CONFIG = "armadillo_config"
        const val KEY_AUDIO_PLAYABLE = "audio_playable"
    }

    internal object DI {
        const val DOWNLOAD_CACHE = "download_cache"
        const val PLAYBACK_CACHE = "playback_cache"

        const val EXOPLAYER_DIRECTORY = "exoplayer_directory"
        const val EXOPLAYER_CACHE_DIRECTORY = "exoplayer_cache_directory"

        const val GLOBAL_SCOPE = "global_scope"

        const val STANDARD_STORAGE = "standard_storage"
        const val DRM_DOWNLOAD_STORAGE = "drm_download_storage"
        const val DRM_SECURE_STORAGE = "drm_secure_storage"
    }

    internal object Exoplayer {
        const val EXOPLAYER_DIRECTORY = "exoplayer" // When changing this, please update the example in the test application backup config
        const val EXOPLAYER_DOWNLOADS_DIRECTORY = "downloads"
        const val EXOPLAYER_PLAYBACK_CACHE_DIRECTORY = "playback_cache"

        const val MAX_PLAYBACK_CACHE_SIZE = 25L * 1024 * 1024 // 25 MB - ~40m of audio
    }

    internal object Actions {
        const val SET_IS_IN_FOREGROUND = "set_is_in_foreground_action"
        const val SET_PLAYBACK_SPEED = "set_playback_speed_action"
        const val UPDATE_PROGRESS = "updated_progress_action"
        const val UPDATE_METADATA = "update_metadata"
        const val UPDATE_MEDIA_REQUEST = "update_media_request"

        object Extras {
            const val IS_IN_FOREGROUND = "is_in_foreground"
            const val PLAYBACK_SPEED = "playback_speed"
            const val METADATA_TITLE = "metadata_title"
            const val METADATA_CHAPTERS = "metadata_chapters"
            const val MEDIA_REQUEST = "media_request"
        }
    }
}