package com.deskzen.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.deskzen.data.local.dao.ProfileDao
import com.deskzen.data.local.dao.ScreenDao
import com.deskzen.data.local.entity.FolderAppEntity
import com.deskzen.data.local.entity.ProfileEntity
import com.deskzen.data.local.entity.ProfileItemEntity
import com.deskzen.data.local.entity.ScreenItemEntity
import com.deskzen.data.local.entity.ScreenPageEntity

@Database(
    entities = [
        ScreenPageEntity::class,
        ScreenItemEntity::class,
        FolderAppEntity::class,
        ProfileEntity::class,
        ProfileItemEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class DeskZenDatabase : RoomDatabase() {
    abstract fun screenDao(): ScreenDao
    abstract fun profileDao(): ProfileDao
}
