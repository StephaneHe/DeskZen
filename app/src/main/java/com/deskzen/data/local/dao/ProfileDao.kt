package com.deskzen.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.deskzen.data.local.entity.ProfileEntity
import com.deskzen.data.local.entity.ProfileItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Query("SELECT * FROM profiles ORDER BY updatedAt DESC")
    fun getAllProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :profileId")
    suspend fun getProfile(profileId: Long): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProfile(): ProfileEntity?

    @Query("SELECT * FROM profile_items WHERE profileId = :profileId ORDER BY pageIndex, position")
    suspend fun getProfileItems(profileId: Long): List<ProfileItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfileItems(items: List<ProfileItemEntity>)

    @Query("UPDATE profiles SET isActive = 0")
    suspend fun deactivateAllProfiles()

    @Query("UPDATE profiles SET isActive = 1 WHERE id = :profileId")
    suspend fun activateProfile(profileId: Long)

    @Query("DELETE FROM profiles WHERE id = :profileId")
    suspend fun deleteProfile(profileId: Long)

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun getProfileCount(): Int

    @Transaction
    suspend fun saveAndActivateProfile(profile: ProfileEntity, items: List<ProfileItemEntity>) {
        deactivateAllProfiles()
        val id = insertProfile(profile.copy(isActive = true))
        insertProfileItems(items.map { it.copy(profileId = id) })
    }
}
