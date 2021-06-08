package com.scribd.armadillotestapp.presentation.di

import com.scribd.armadillotestapp.data.Content
import com.scribd.armadillotestapp.data.TestContent
import dagger.Module
import dagger.Provides

@Module
class UtilModule {
    @Provides
    fun content(content: TestContent): Content = content
}