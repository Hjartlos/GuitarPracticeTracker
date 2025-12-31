package com.example.gpt.di

import com.example.gpt.audio.AudioEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAudioEngine(): AudioEngine {
        return AudioEngine()
    }
}