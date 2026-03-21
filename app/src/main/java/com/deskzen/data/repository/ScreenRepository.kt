package com.deskzen.data.repository

import com.deskzen.domain.model.ScreenPage
import kotlinx.coroutines.flow.Flow

interface ScreenRepository {
    fun getScreenLayout(): Flow<List<ScreenPage>>
    suspend fun saveScreenLayout(pages: List<ScreenPage>)
    suspend fun addAppToFolder(folderId: Long, packageName: String)
    suspend fun removeAppFromFolder(folderId: Long, packageName: String)
    suspend fun createFolder(pageIndex: Int, position: Int, name: String, apps: List<String>)
    suspend fun deleteFolder(folderId: Long)
    suspend fun renameFolder(folderId: Long, newName: String)
    suspend fun moveItem(fromPage: Int, fromPos: Int, toPage: Int, toPos: Int)
}
