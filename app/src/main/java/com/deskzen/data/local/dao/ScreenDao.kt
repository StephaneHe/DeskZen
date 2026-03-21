package com.deskzen.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.deskzen.data.local.entity.FolderAppEntity
import com.deskzen.data.local.entity.ScreenItemEntity
import com.deskzen.data.local.entity.ScreenPageEntity
import com.deskzen.data.local.entity.ScreenPageWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreenDao {

    @Query("SELECT * FROM screen_pages ORDER BY pageIndex")
    fun getAllPages(): Flow<List<ScreenPageEntity>>

    @Query("SELECT * FROM screen_items WHERE pageIndex = :pageIndex ORDER BY position")
    fun getItemsForPage(pageIndex: Int): Flow<List<ScreenItemEntity>>

    @Query("SELECT * FROM folder_apps WHERE folderId = :folderId ORDER BY positionInFolder")
    fun getAppsInFolder(folderId: Long): Flow<List<FolderAppEntity>>

    @Transaction
    @Query("SELECT * FROM screen_pages ORDER BY pageIndex")
    fun getFullScreen(): Flow<List<ScreenPageWithItems>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: ScreenPageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ScreenItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolderApp(folderApp: FolderAppEntity)

    @Query("DELETE FROM screen_pages")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceFullScreen(
        pages: List<ScreenPageEntity>,
        items: List<ScreenItemEntity>,
        folderApps: List<FolderAppEntity>
    ) {
        clearAll()
        pages.forEach { insertPage(it) }
        items.forEach { insertItem(it) }
        folderApps.forEach { insertFolderApp(it) }
    }
}
