package com.scribd.armadillo.extensions

import android.os.Bundle
import android.support.v4.media.session.MediaControllerCompat
import com.scribd.armadillo.Constants
import com.scribd.armadillo.models.Chapter

internal fun MediaControllerCompat.TransportControls.sendCustomAction(customAction: CustomAction) {
    sendCustomAction(customAction.action, customAction.toBundle())
}

internal sealed class CustomAction {
    abstract fun toBundle(): Bundle
    abstract val action: String

    companion object {
        @JvmStatic
        fun build(action: String?, bundle: Bundle?): CustomAction? {
            return when (action) {
                Constants.Actions.UPDATE_PROGRESS -> UpdateProgress
                Constants.Actions.SET_PLAYBACK_SPEED -> {
                    val playbackSpeed = bundle?.getFloat(Constants.Actions.Extras.PLAYBACK_SPEED, Constants.DEFAULT_PLAYBACK_SPEED)
                        ?: Constants.DEFAULT_PLAYBACK_SPEED
                    SetPlaybackSpeed(playbackSpeed)
                }
                Constants.Actions.SET_IS_IN_FOREGROUND -> {
                    val isInForeground = bundle?.getBoolean(Constants.Actions.Extras.IS_IN_FOREGROUND) ?: false
                    SetIsInForeground(isInForeground)
                }
                Constants.Actions.UPDATE_METADATA -> {
                    val title = bundle?.getString(Constants.Actions.Extras.METADATA_TITLE)!!
                    val chapters = bundle.getParcelableArrayList<Chapter>(Constants.Actions.Extras.METADATA_CHAPTERS)!!
                    UpdatePlaybackMetadata(title, chapters)
                }
                else -> null
            }
        }
    }

    object UpdateProgress : CustomAction() {
        override val action: String = Constants.Actions.UPDATE_PROGRESS
        override fun toBundle(): Bundle = Bundle()
    }

    data class SetPlaybackSpeed(val playbackSpeed: Float) : CustomAction() {
        override val action: String = Constants.Actions.SET_PLAYBACK_SPEED
        override fun toBundle(): Bundle = Bundle().apply { putFloat(Constants.Actions.Extras.PLAYBACK_SPEED, playbackSpeed) }
    }

    data class UpdatePlaybackMetadata(val title: String, val chapters: List<Chapter>) : CustomAction() {
        override val action: String = Constants.Actions.UPDATE_METADATA
        override fun toBundle(): Bundle = Bundle().apply {
            putString(Constants.Actions.Extras.METADATA_TITLE, title)
            putParcelableArrayList(Constants.Actions.Extras.METADATA_CHAPTERS, ArrayList(chapters))
        }
    }

    /**
     * Signals to the engine that the player is visible and needs more frequent updates
     */
    data class SetIsInForeground(val isInForeground: Boolean) : CustomAction() {
        override val action: String = Constants.Actions.SET_IS_IN_FOREGROUND
        override fun toBundle(): Bundle = Bundle().apply { putBoolean(Constants.Actions.Extras.IS_IN_FOREGROUND, isInForeground) }
    }
}