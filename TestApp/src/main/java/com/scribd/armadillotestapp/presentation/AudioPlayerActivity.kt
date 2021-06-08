package com.scribd.armadillotestapp.presentation

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.format.Formatter
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.scribd.armadillo.ArmadilloDebugView
import com.scribd.armadillo.ArmadilloPlayer
import com.scribd.armadillo.models.AudioPlayable
import com.scribd.armadillo.models.Chapter
import com.scribd.armadillo.models.PlaybackState
import com.scribd.armadillo.time.Interval
import com.scribd.armadillo.time.Millisecond
import com.scribd.armadillo.time.seconds
import com.scribd.armadillotestapp.R
import com.scribd.armadillotestapp.presentation.analytics.ArmadilloPlaybackActionListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AudioPlayerActivity : AppCompatActivity() {
    @Inject
    internal lateinit var armadilloPlayer: ArmadilloPlayer
    private lateinit var audiobook: AudioPlayable
    private lateinit var playButton: ImageView
    private lateinit var speedButton: Button
    private lateinit var stopPlayback: Button
    private lateinit var skipDistanceButton: Button
    private lateinit var clearCacheButton: Button
    private lateinit var removeAllDownloadsButton: Button
    private val playIcon = android.R.drawable.ic_media_play
    private val pauseIcon = android.R.drawable.ic_media_pause
    private val armadilloDebugView: ArmadilloDebugView by lazy { findViewById(R.id.armadilloDebugView) }
    private val disposables = CompositeDisposable()
    private var skipDistance = 30.seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as AudioPlayerApplication).mainComponent.inject(this)
        setContentView(R.layout.audio_player)
        playButton = findViewById(R.id.playButton)
        speedButton = findViewById(R.id.changePlaybackSpeedBtn)
        skipDistanceButton = findViewById(R.id.skipDistanceBtn)
        stopPlayback = findViewById(R.id.stopPlaybackBtn)
        clearCacheButton = findViewById(R.id.clearPlaybackCacheBtn)
        removeAllDownloadsButton = findViewById(R.id.removeAllDownloadsBtn)
        audiobook = intent.getSerializableExtra(MainActivity.AUDIOBOOK_EXTRA) as AudioPlayable

        val previousTrackBtn: View = findViewById(R.id.previousTrack)
        val removeDownloadBtn: View = findViewById(R.id.removeDownloadBtn)
        val downloadBtn: View = findViewById(R.id.downloadBtn)
        val nextTrackBtn: View = findViewById(R.id.nextTrack)
        val skipBackBtn: View = findViewById(R.id.skipBack)
        val skipForwardBtn: View = findViewById(R.id.skipForward)

        playButton.setOnClickListener { armadilloPlayer.playOrPause() }
        previousTrackBtn.setOnClickListener { armadilloPlayer.previousChapter() }
        nextTrackBtn.setOnClickListener { armadilloPlayer.nextChapter() }
        skipBackBtn.setOnClickListener { armadilloPlayer.skipBackward() }
        skipForwardBtn.setOnClickListener { armadilloPlayer.skipForward() }
        speedButton.setOnClickListener(SpeedButtonClickListener(speedButton, 3.0f))
        downloadBtn.setOnClickListener {
            armadilloPlayer.beginDownload(audiobook)
        }
        removeDownloadBtn.setOnClickListener { armadilloPlayer.removeDownload(audiobook) }
        clearCacheButton.setOnClickListener { armadilloPlayer.clearCache() }
        stopPlayback.setOnClickListener { armadilloPlayer.endPlayback() }
        skipDistanceButton.setOnClickListener { changeSkipDistance() }
        removeAllDownloadsButton.setOnClickListener { armadilloPlayer.removeAllDownloads() }

        initArmadilloAndBeginPlayback()
    }

    override fun onStart() {
        super.onStart()

        armadilloPlayer.isInForeground = true
    }

    override fun onStop() {
        super.onStop()

        armadilloPlayer.isInForeground = false
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }

    private fun initArmadilloAndBeginPlayback() {
        armadilloPlayer.addPlaybackActionListener(ArmadilloPlaybackActionListener())
        armadilloPlayer.beginPlayback(audiobook)
        disposables.add(armadilloPlayer.armadilloStateObservable.subscribe { armadilloState ->
            val playButtonIcon = when (armadilloState.playbackInfo?.playbackState) {
                PlaybackState.PLAYING -> pauseIcon
                else -> playIcon
            }

            playButton.setImageResource(playButtonIcon)

            removeAllDownloadsButton.text = getString(R.string.remove_downloads_with_size,
                Formatter.formatShortFileSize(this, armadilloPlayer.downloadCacheSize))
            clearCacheButton.text = getString(R.string.clear_playback_cache_size,
                Formatter.formatShortFileSize(this, armadilloPlayer.playbackCacheSize))
            armadilloDebugView.update(armadilloState, audiobook)
        })
        disposables.add(armadilloPlayer.armadilloStateObservable.filter {
            it.playbackInfo?.progress?.totalPlayerDuration != null
        }.map {
            it.playbackInfo?.progress?.totalPlayerDuration!!
        }.distinctUntilChanged()
            .delay(3, TimeUnit.SECONDS) // Delay so it doesn't happen immediately on startup
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { playerDuration ->
                val chapters = updateChaptersForNewTime(playerDuration)
                armadilloPlayer.updatePlaybackMetadata("${audiobook.title} - Updated", chapters)
            })
    }

    @SuppressLint("SetTextI18n")
    private fun changeSkipDistance() {
        when (skipDistance) {
            30.seconds -> {
                skipDistance = 60.seconds
                skipDistanceButton.text = "Skip - 60 Seconds"
            }
            60.seconds -> {
                skipDistance = 5.seconds
                skipDistanceButton.text = "Skip - 5 Seconds"
            }
            else -> {
                skipDistance = 30.seconds
                skipDistanceButton.text = "Skip - 30 Seconds"
            }
        }
        armadilloPlayer.skipDistance = skipDistance.inMilliseconds
    }

    private fun updateChaptersForNewTime(time: Interval<Millisecond>): List<Chapter> =
        // Muddle with the data to make it right
        audiobook.chapters.mapIndexed { index, chapter ->
            if (index == (audiobook.chapters.size - 1)) {
                chapter.copy(duration = (time - chapter.startTime)) // Alter last chapter duration
            } else {
                chapter
            }
        }

    // This is all poor practice, but was good for quickly getting a test together
    private inner class SpeedButtonClickListener(private val speedButton: Button, private val speedOnClick: Float) : View.OnClickListener {
        init {
            speedButton.text = getString(R.string.speed_button_text, speedOnClick)
        }

        override fun onClick(v: View?) {
            armadilloPlayer.playbackSpeed = speedOnClick
            val newSpeed = if (speedOnClick == 1.0f) {
                3.0f
            } else {
                1.0f
            }
            speedButton.setOnClickListener(SpeedButtonClickListener(speedButton, newSpeed))
        }

    }
}