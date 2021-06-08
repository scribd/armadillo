package com.scribd.armadillotestapp.presentation.di

import com.scribd.armadillotestapp.presentation.AudioPlayerActivity
import com.scribd.armadillotestapp.presentation.AudioPlayerApplication
import com.scribd.armadillotestapp.presentation.ContentAdapter
import com.scribd.armadillotestapp.presentation.MainActivity
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [
    (AppModule::class),
    (UtilModule::class),
    (AudioplayerModule::class)])
interface MainComponent {
    fun inject(mainActivity: MainActivity)
    fun inject(audioPlayerActivity: AudioPlayerActivity)
    fun inject(audioPlayerApplication: AudioPlayerApplication)
    fun inject(contentAdapter: ContentAdapter)
}