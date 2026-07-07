package com.aichat.app.di

import android.content.Context
import androidx.room.Room
import com.aichat.app.data.local.ApiEndpointDao
import com.aichat.app.data.local.AppDatabase
import com.aichat.app.data.local.ConversationDao
import com.aichat.app.data.local.MessageDao
import com.aichat.app.data.remote.ApiManager
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "aichat_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideConversationDao(database: AppDatabase): ConversationDao {
        return database.conversationDao()
    }

    @Provides
    @Singleton
    fun provideMessageDao(database: AppDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    @Singleton
    fun provideApiEndpointDao(database: AppDatabase): ApiEndpointDao {
        return database.apiEndpointDao()
    }
}
