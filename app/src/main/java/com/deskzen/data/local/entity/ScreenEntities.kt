package com.deskzen.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "screen_pages")
data class ScreenPageEntity(
    @PrimaryKey val pageIndex: Int
)

@Entity(
    tableName = "screen_items",
    foreignKeys = [ForeignKey(
        entity = ScreenPageEntity::class,
        parentColumns = ["pageIndex"],
        childColumns = ["pageIndex"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("pageIndex")]
)
data class ScreenItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pageIndex: Int,
    val position: Int,
    val type: String,
    val packageName: String?,
    val folderName: String?,
    val folderColor: Long?
)

@Entity(
    tableName = "folder_apps",
    foreignKeys = [ForeignKey(
        entity = ScreenItemEntity::class,
        parentColumns = ["id"],
        childColumns = ["folderId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("folderId")]
)
data class FolderAppEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: Long,
    val packageName: String,
    val positionInFolder: Int
)

data class ScreenPageWithItems(
    @Embedded val page: ScreenPageEntity,
    @Relation(
        parentColumn = "pageIndex",
        entityColumn = "pageIndex"
    )
    val items: List<ScreenItemEntity>
)
