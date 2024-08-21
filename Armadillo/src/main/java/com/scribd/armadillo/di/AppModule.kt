package com.scribd.armadillo.di

import android.content.Context
import com.scribd.armadillo.ArmadilloStateStore
import com.scribd.armadillo.Constants
import com.scribd.armadillo.Reducer
import com.scribd.armadillo.StateStore
import com.scribd.armadillo.analytics.PlaybackActionTransmitter
import com.scribd.armadillo.analytics.PlaybackActionTransmitterImpl
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import javax.inject.Named
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
internal class AppModule(private val context: Context) {
    @Singleton
    @Provides
    fun context(): Context = context.applicationContext

    @Singleton
    @Provides
    @Named(Constants.DI.GLOBAL_SCOPE)
    fun globalScope(): CoroutineScope = GlobalScope

    @Singleton
    @Provides
    fun reducer(): Reducer = Reducer

    @Singleton
    @PrivateModule
    @Provides
    fun stateStore(reducer: Reducer): ArmadilloStateStore = ArmadilloStateStore(reducer, context)

    @Singleton
    @Provides
    fun stateStoreInitializer(@PrivateModule stateStore: ArmadilloStateStore): StateStore.Initializer = stateStore

    @Singleton
    @Provides
    fun stateStoreModifier(@PrivateModule stateStore: ArmadilloStateStore): StateStore.Modifier = stateStore

    @Singleton
    @Provides
    fun stateStoreProvider(@PrivateModule stateStore: ArmadilloStateStore): StateStore.Provider = stateStore

    @Singleton
    @Provides
    fun playbackActionTransmitter(@PrivateModule stateStore: ArmadilloStateStore): PlaybackActionTransmitter =
        PlaybackActionTransmitterImpl(stateStore)

    @Qualifier
    @Retention(AnnotationRetention.RUNTIME)
    private annotation class PrivateModule
    // https://stackoverflow.com/questions/43272652/dagger-2-avoid-exporting-private-dependencies
}