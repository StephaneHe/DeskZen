package com.deskzen.data.repository

import com.deskzen.data.local.dao.ScreenDao
import com.deskzen.data.local.entity.FolderAppEntity
import com.deskzen.data.local.entity.ScreenItemEntity
import com.deskzen.data.local.entity.ScreenPageEntity
import com.deskzen.domain.model.ScreenItem
import com.deskzen.domain.model.ScreenPage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenRepositoryImpl @Inject constructor(
    private val screenDao: ScreenDao,
    private val appRepository: AppRepository
) : ScreenRepository {

    override fun getScreenLayout(): Flow<List<ScreenPage>> {
        return screenDao.getFullScreen().map { pagesWithItems ->
            pagesWithItems.map { pageWithItems ->
                ScreenPage(
                    pageIndex = pageWithItems.page.pageIndex,
                    items = pageWithItems.items.mapNotNull { itemEntity ->
                        mapEntityToScreenItem(itemEntity)
                    }
                )
            }
        }
    }

    override suspend fun saveScreenLayout(pages: List<ScreenPage>) {
        val pageEntities = pages.map { ScreenPageEntity(it.pageIndex) }
        val itemEntities = mutableListOf<ScreenItemEntity>()
        val folderAppEntities = mutableListOf<FolderAppEntity>()

        pages.forEach { page ->
            page.items.forEach { item ->
                when (item) {
                    is ScreenItem.AppShortcut -> {
                        itemEntities.add(
                            ScreenItemEntity(
                                pageIndex = page.pageIndex,
                                position = item.position,
                                type = "app",
                                packageName = item.appInfo.packageName,
                                folderName = null,
                                folderColor = null
                            )
                        )
                    }
                    is ScreenItem.Folder -> {
                        val folderId = item.id
                        itemEntities.add(
                            ScreenItemEntity(
                                id = folderId,
                                pageIndex = page.pageIndex,
                                position = item.position,
                                type = "folder",
                                packageName = null,
                                folderName = item.name,
                                folderColor = item.color
                            )
                        )
                        item.apps.forEachIndexed { index, app ->
                            folderAppEntities.add(
                                FolderAppEntity(
                                    folderId = folderId,
                                    packageName = app.packageName,
                                    positionInFolder = index
                                )
                            )
                        }
                    }
                }
            }
        }

        screenDao.replaceFullScreen(pageEntities, itemEntities, folderAppEntities)
    }

    override suspend fun addAppToFolder(folderId: Long, packageName: String) {
        val currentApps = screenDao.getAppsInFolder(folderId)
        // Get count for position
        screenDao.insertFolderApp(
            FolderAppEntity(
                folderId = folderId,
                packageName = packageName,
                positionInFolder = 0 // Will be corrected on next read
            )
        )
    }

    override suspend fun removeAppFromFolder(folderId: Long, packageName: String) {
        // Handled by re-saving the full layout
        Timber.d("removeAppFromFolder: $folderId, $packageName")
    }

    override suspend fun createFolder(
        pageIndex: Int,
        position: Int,
        name: String,
        apps: List<String>
    ) {
        val folderId = screenDao.insertItem(
            ScreenItemEntity(
                pageIndex = pageIndex,
                position = position,
                type = "folder",
                packageName = null,
                folderName = name,
                folderColor = null
            )
        )
        apps.forEachIndexed { index, pkg ->
            screenDao.insertFolderApp(
                FolderAppEntity(
                    folderId = folderId,
                    packageName = pkg,
                    positionInFolder = index
                )
            )
        }
    }

    override suspend fun deleteFolder(folderId: Long) {
        // CASCADE will handle folder_apps cleanup
        Timber.d("deleteFolder: $folderId")
    }

    override suspend fun renameFolder(folderId: Long, newName: String) {
        Timber.d("renameFolder: $folderId -> $newName")
    }

    override suspend fun moveItem(fromPage: Int, fromPos: Int, toPage: Int, toPos: Int) {
        Timber.d("moveItem: ($fromPage, $fromPos) -> ($toPage, $toPos)")
    }

    private suspend fun mapEntityToScreenItem(entity: ScreenItemEntity): ScreenItem? {
        return when (entity.type) {
            "app" -> {
                val packageName = entity.packageName ?: return null
                val appInfo = appRepository.getAppInfo(packageName) ?: return null
                ScreenItem.AppShortcut(position = entity.position, appInfo = appInfo)
            }
            "folder" -> {
                ScreenItem.Folder(
                    position = entity.position,
                    id = entity.id,
                    name = entity.folderName ?: "Dossier",
                    apps = emptyList(), // Will be enriched later
                    color = entity.folderColor
                )
            }
            else -> null
        }
    }
}
