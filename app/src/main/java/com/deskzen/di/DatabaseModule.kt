package com.deskzen.di

import android.content.Context
import androidx.room.Room
import com.deskzen.data.local.DeskZenDatabase
import com.deskzen.data.local.dao.ProfileDao
import com.deskzen.data.local.dao.ScreenDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DeskZenDatabase {
        return Room.databaseBuilder(
            context,
            DeskZenDatabase::class.java,
            "deskzen.db"
        ).build()
    }

    @Provides
    fun provideScreenDao(database: DeskZenDatabase): ScreenDao {
        return database.screenDao()
    }

    @Provides
    fun provideProfileDao(database: DeskZenDatabase): ProfileDao {
        return database.profileDao()
    }
}
