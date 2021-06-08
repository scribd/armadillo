package com.scribd.armadillotestapp.presentation.di

import android.content.Context
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AppModule(private val context: Context) {

    @Singleton
    @Provides
    fun provideAppContext(): Context = context.applicationContext
}