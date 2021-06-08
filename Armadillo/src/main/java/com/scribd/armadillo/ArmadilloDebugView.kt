package com.scribd.armadillo

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.scribd.armadillo.models.ArmadilloState
import com.scribd.armadillo.models.AudioPlayable
import com.scribd.armadillo.models.DownloadState
import java.util.concurrent.TimeUnit

/**
 * Debugging Tool, should be always hidden in production!
 * A Ui element for visualizing the state of the [ArmadilloPlayer]
 */
class ArmadilloDebugView : FrameLayout {
    private companion object {
        const val TAG = "ArmadilloDebugView"
    }

    constructor(context: Context) : super(context) {
        initLayout()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initLayout()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initLayout()
    }

    private val titleTv: TextView by lazy { findViewById<TextView>(R.id.audiobookTitle) }
    private val playlistSizeInfoTv: TextView by lazy { findViewById<TextView>(R.id.playlistSizeInfoTv) }
    private val appStateUpdateCountTv: TextView by lazy { findViewById<TextView>(R.id.appStateUpdateCountTv) }
    private val actionsDispatchedTv: TextView by lazy { findViewById<TextView>(R.id.actionsDispatchedTv) }
    private val positionAndDurationTv: TextView by lazy { findViewById<TextView>(R.id.positionAndDurationTv) }
    private val loadingTv: View by lazy { findViewById<View>(R.id.loadingTv) }
    private val playbackSpeedTv: TextView by lazy { findViewById<TextView>(R.id.playbackSpeedTv) }
    private val downloadStateTv: TextView by lazy { findViewById<TextView>(R.id.downloadStateTv) }
    private val downloadPercentTv: TextView by lazy { findViewById<TextView>(R.id.downloadPercentTv) }

    fun update(state: ArmadilloState, audioPlayable: AudioPlayable) {
        val activity = context as Activity

        val appStateCountText = activity.getString(R.string.arm_state_update_count, state.debugState.appStateUpdateCount)
        appStateUpdateCountTv.text = appStateCountText
        actionsDispatchedTv.text = state.debugState.getActionHistoryDisplayString()

        val playbackInfo = state.playbackInfo
        if (playbackInfo != null) {
            titleTv.text = activity.getString(R.string.arm_title, state.playbackInfo.audioPlayable.title)
            val playlistIndexText = activity.getString(R.string.arm_current_chapter,
                (state.playbackInfo.progress.currentChapterIndex + 1).toString(), playbackInfo.audioPlayable.chapters.size.toString())
            playlistSizeInfoTv.text = playlistIndexText
            val positionDurationString = activity.getString(R.string.arm_position_duration,
                playbackInfo.progress.positionInDuration.displayString(),
                playbackInfo.progress.totalChaptersDuration.displayString(),
                playbackInfo.progress.totalPlayerDuration?.displayString() ?: context.getString(R.string.arm_duration_unknown))
            positionAndDurationTv.text = positionDurationString

            loadingTv.visibility = if (playbackInfo.isLoading) View.VISIBLE else View.GONE

            playbackSpeedTv.text = context.getString(R.string.arm_playback_speed, playbackInfo.playbackSpeed)

            titleTv.visibility = View.VISIBLE
            playlistSizeInfoTv.visibility = View.VISIBLE
            positionAndDurationTv.visibility = View.VISIBLE
        } else {
            titleTv.visibility = View.GONE
            playlistSizeInfoTv.visibility = View.GONE
            positionAndDurationTv.visibility = View.GONE
            loadingTv.visibility = View.GONE
        }

        val currentAudiobookDownloadInfo = state.downloadInfo.find { it.url == audioPlayable.request.url }
        if (currentAudiobookDownloadInfo != null) {
            val downloadState = currentAudiobookDownloadInfo.downloadState
            Log.i(TAG, downloadState.toString())
            downloadStateTv.text = activity.getString(R.string.arm_download_state, downloadState.toString())
            val downloadPercent = when (downloadState) {
                is DownloadState.STARTED -> downloadState.percentComplete
                is DownloadState.COMPLETED -> 100
                else -> 0
            }

            downloadPercentTv.text = activity.getString(R.string.arm_download_percent, downloadPercent)
            downloadStateTv.visibility = View.VISIBLE
            downloadPercentTv.visibility = View.VISIBLE
        } else {
            downloadStateTv.visibility = View.GONE
            downloadPercentTv.visibility = View.GONE
        }

        if (state.error != null) {
            Log.e(TAG, state.error.cause?.message ?: state.error.cause.toString())
        }
    }

    private fun Milliseconds.displayString(): String = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(longValue),
        TimeUnit.MILLISECONDS.toMinutes(longValue) % TimeUnit.HOURS.toMinutes(1),
        TimeUnit.MILLISECONDS.toSeconds(longValue) % TimeUnit.MINUTES.toSeconds(1))

    private fun initLayout() {
        LayoutInflater.from(context).inflate(R.layout.arm_audio_engine_debug_layout, this, true)
    }
}