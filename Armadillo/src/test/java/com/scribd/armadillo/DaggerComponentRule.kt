package com.scribd.armadillo

import com.scribd.armadillo.analytics.PlaybackActionTransmitterImpl
import com.scribd.armadillo.di.Injector
import com.scribd.armadillo.di.MainComponent
import com.scribd.armadillo.download.DefaultExoplayerDownloadService
import com.scribd.armadillo.playback.ExoplayerPlaybackEngine
import com.scribd.armadillo.playback.MediaSessionCallback
import com.scribd.armadillo.playback.PlaybackService
import com.scribd.armadillo.playback.PlayerEventListener
import com.scribd.armadillo.playback.mediasource.MediaSourceRetrieverImpl
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class DaggerComponentRule : TestRule {
    override fun apply(base: Statement, description: Description?): Statement {
        return object : Statement() {
            @Throws(Throwable::class)
            override fun evaluate() {
                Injector.mainComponent = MockMainComponent()
                try {
                    base.evaluate()
                } finally {
                    // Nothing to do after the test
                }
            }
        }
    }

    private class MockMainComponent : MainComponent {
        override fun inject(playbackActionTransmitterImpl: PlaybackActionTransmitterImpl) = Unit
        override fun inject(mediaSourceRetrieverImpl: MediaSourceRetrieverImpl) = Unit
        override fun inject(exoplayerPlaybackEngine: ExoplayerPlaybackEngine) = Unit
        override fun inject(mediaSessionCallback: MediaSessionCallback) = Unit
        override fun inject(armadilloPlayerChoreographer: ArmadilloPlayerChoreographer) = Unit
        override fun inject(playbackService: PlaybackService) = Unit
        override fun inject(armadilloPlayerFactory: ArmadilloPlayerFactory) = Unit
        override fun inject(defaultExoplayerDownloadService: DefaultExoplayerDownloadService) = Unit
        override fun inject(playerEventListener: PlayerEventListener) = Unit
    }
}