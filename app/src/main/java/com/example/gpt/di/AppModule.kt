package com.example.gpt.di

import android.content.Context
import com.example.gpt.core.audio.AudioEngine
import com.example.gpt.core.haptic.HapticManager
import com.example.gpt.data.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAudioEngine(): AudioEngine = AudioEngine()

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository {
        return SettingsRepository(context)
    }

    @Provides
    @Singleton
    fun provideHapticManager(@ApplicationContext context: Context): HapticManager {
        return HapticManager(context)
    }
}