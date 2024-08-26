package com.scribd.armadillo.di

import com.scribd.armadillo.ArmadilloPlayerChoreographer
import com.scribd.armadillo.ArmadilloPlayerFactory
import com.scribd.armadillo.analytics.PlaybackActionTransmitterImpl
import com.scribd.armadillo.download.DefaultExoplayerDownloadService
import com.scribd.armadillo.download.drm.events.WidevineSessionEventListener
import com.scribd.armadillo.playback.ExoplayerPlaybackEngine
import com.scribd.armadillo.playback.MediaSessionCallback
import com.scribd.armadillo.playback.PlaybackService
import com.scribd.armadillo.playback.PlayerEventListener
import com.scribd.armadillo.playback.mediasource.MediaSourceRetrieverImpl
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [
    (AppModule::class),
    (DownloadModule::class),
    (PlaybackModule::class)])
internal interface MainComponent {
    fun inject(armadilloPlayerChoreographer: ArmadilloPlayerChoreographer)
    fun inject(playbackService: PlaybackService)
    fun inject(mediaSessionCallback: MediaSessionCallback)
    fun inject(exoplayerPlaybackEngine: ExoplayerPlaybackEngine)
    fun inject(armadilloPlayerFactory: ArmadilloPlayerFactory)
    fun inject(defaultExoplayerDownloadService: DefaultExoplayerDownloadService)
    fun inject(playerEventListener: PlayerEventListener)
    fun inject(playbackActionTransmitterImpl: PlaybackActionTransmitterImpl)
    fun inject(mediaSourceRetrieverImpl: MediaSourceRetrieverImpl)
    fun inject(widevineSessionEventListener: WidevineSessionEventListener)
}