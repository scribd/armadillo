package com.scribd.armadillotestapp.data

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import com.scribd.armadillo.extensions.toUri
import com.scribd.armadillo.models.AudioPlayable
import com.scribd.armadillo.models.Chapter
import com.scribd.armadillo.time.milliseconds
import com.scribd.armadillo.time.minutes
import com.scribd.armadillo.time.seconds
import javax.inject.Inject

interface Content {
    val playables: List<AudioPlayable>

    val mediaItems: List<MediaBrowserCompat.MediaItem>

    fun mediaForId(mediaId: String): MediaBrowserCompat.MediaItem?

    fun randomMediaItem(): MediaBrowserCompat.MediaItem
}

class TestContent @Inject constructor() : Content {
    override val playables: List<AudioPlayable> = listOf(googleHostedMp3, podcastMedia, appleMasterHls, dashAudio)

    override val mediaItems: List<MediaBrowserCompat.MediaItem>
        get() {
            val playables = listOf(googleHostedMp3, podcastMedia, appleMasterHls, dashAudio)
            val mediaItems = playables.mapIndexed { i, playable ->
                val mediaDescription = MediaDescriptionCompat.Builder()
                    .setTitle(playable.title)
                    .setMediaId(i.toString())
                    .setDescription("{$i: playable.title")
                    .setMediaUri(playable.request.url.toUri()).build()
                MediaBrowserCompat.MediaItem(mediaDescription, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
            }

            return mediaItems.toMutableList().apply {
                val errorItem = MediaDescriptionCompat.Builder()
                    .setTitle("Media with error")
                    .setMediaId("error")
                    .setDescription("Item which triggers an error")
                    .build()
                MediaBrowserCompat.MediaItem(errorItem, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
                add(MediaBrowserCompat.MediaItem(errorItem, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE))
            }
        }

    override fun randomMediaItem(): MediaBrowserCompat.MediaItem = mediaItems.shuffled().first()

    override fun mediaForId(mediaId: String): MediaBrowserCompat.MediaItem? =
        mediaItems.firstOrNull {
            it.mediaId == mediaId
        }

    private val googleHostedMp3: AudioPlayable
        get() {
            val url = "https://storage.googleapis.com/exoplayer-test-media-0/play.mp3"
            return AudioPlayable(
                id = 100,
                title = "Google Hosted Mp3",
                request = AudioPlayable.MediaRequest.createHttpUri(url),
                chapters = listOf(Chapter(
                    title = "Chapter 0",
                    part = 0,
                    chapter = 0,
                    startTime = 0.milliseconds,
                    duration = 59.seconds.inMilliseconds
                ))
            )
        }

    private val podcastMedia: AudioPlayable
        get() {
            val url = "http://hwcdn.libsyn.com/p/d/9/1/d91963d23151f153/Dr._Christiane_Northrup_M.D" +
                "._-_The_Secret_Prescription_for_Radiance_Vitality_and_Well-Being" +
                ".mp3?c_id=11477225&cs_id=11477225&destination_id=267562&expiration=1561681068&hwt=37823474dc47528c88086a888a532df3"

            return AudioPlayable(
                id = 101,
                title = "Mp3 Podcast",
                request = AudioPlayable.MediaRequest.createHttpUri(url),
                chapters = listOf(Chapter(
                    title = "Chapter 0",
                    part = 0,
                    chapter = 0,
                    startTime = 0.milliseconds,
                    duration = 1808.seconds.inMilliseconds
                ))
            )
        }

    private val appleMasterHls: AudioPlayable
        get() {
            val url = "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_ts/master.m3u8"
            return AudioPlayable(
                id = 104,
                title = "Apple master playlist advanced (TS)",
                request = AudioPlayable.MediaRequest.createHttpUri(url),
                chapters = listOf(Chapter(
                    title = "Chapter 0",
                    part = 0,
                    chapter = 0,
                    startTime = 0.milliseconds,
                    duration = 10.minutes.inMilliseconds
                ))
            )
        }

    private val dashAudio: AudioPlayable
        get() {
            val url = "https://livesim.dashif.org/dash/vod/testpic_2s/audio.mpd"
            return AudioPlayable(
                id = 105,
                title = "MPEG-DASH Audio Stream",
                request = AudioPlayable.MediaRequest.createHttpUri(url),
                chapters = listOf(
                    Chapter(
                        title = "Chapter 0",
                        part = 0,
                        chapter = 0,
                        startTime = 0.milliseconds,
                        duration = 10.minutes.inMilliseconds
                    ),
                    Chapter(
                        title = "Chapter 1",
                        part = 0,
                        chapter = 1,
                        startTime = 10.minutes.inMilliseconds,
                        duration = 10.minutes.inMilliseconds
                    ),
                    Chapter(
                        title = "Chapter 2",
                        part = 0,
                        chapter = 2,
                        startTime = 20.minutes.inMilliseconds,
                        duration = 10.minutes.inMilliseconds
                    ),
                    Chapter(
                        title = "Chapter 3",
                        part = 0,
                        chapter = 3,
                        startTime = 30.minutes.inMilliseconds,
                        duration = 30.minutes.inMilliseconds
                    ),
                )
            )
        }
}

