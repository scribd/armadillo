package com.scribd.armadillotestapp.presentation.di

import android.content.Context
import com.scribd.armadillo.ArmadilloPlayer
import com.scribd.armadillo.ArmadilloPlayerFactory
import com.scribd.armadillo.download.DownloadNotificationFactory
import com.scribd.armadillo.playback.PlaybackNotificationBuilder
import com.scribd.armadillotestapp.presentation.AudioDownloadManager
import com.scribd.armadillotestapp.presentation.PlaybackNotificationManager
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AudioplayerModule {

    @Singleton
    @Provides
    fun downloadNotification(): DownloadNotificationFactory = AudioDownloadManager()

    @Singleton
    @Provides
    fun playbackNotification(context: Context): PlaybackNotificationBuilder = PlaybackNotificationManager(context)

    @Singleton
    @Provides
    fun audioPlayer(downloadNotificationFactory: DownloadNotificationFactory,
                    playbackNotificationBuilder: PlaybackNotificationBuilder): ArmadilloPlayer {
        return ArmadilloPlayerFactory.init(downloadNotificationFactory, playbackNotificationBuilder)
    }
}