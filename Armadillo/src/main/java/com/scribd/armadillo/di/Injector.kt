package com.scribd.armadillo.di

import android.content.Context

/**
 * Manages the dagger instance for dependency injection
 */
internal object Injector {
    lateinit var mainComponent: MainComponent
    fun buildDependencyGraph(context: Context) {
        mainComponent = DaggerMainComponent.builder()
                .appModule(AppModule(context))
                .playbackModule(PlaybackModule())
                .build()
    }
}