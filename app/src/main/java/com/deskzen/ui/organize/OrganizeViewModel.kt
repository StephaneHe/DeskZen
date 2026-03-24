package com.deskzen.ui.organize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deskzen.data.local.dao.ProfileDao
import com.deskzen.data.local.entity.ProfileEntity
import com.deskzen.data.local.entity.ProfileItemEntity
import com.deskzen.data.repository.ScreenRepository
import com.deskzen.domain.model.OrganizationProfile
import com.deskzen.domain.model.ProfileSource
import com.deskzen.domain.model.ScreenItem
import com.deskzen.domain.model.ScreenPage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed interface OrganizeUiState {
    data object Loading : OrganizeUiState
    data class Editing(
        val pages: List<ScreenPage>,
        val dragState: DragState = DragState(),
        val isSelectionMode: Boolean = false,
        val profiles: List<OrganizationProfile> = emptyList()
    ) : OrganizeUiState
    data class Error(val message: String) : OrganizeUiState
}

@HiltViewModel
class OrganizeViewModel @Inject constructor(
    private val screenRepository: ScreenRepository,
    private val profileDao: ProfileDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<OrganizeUiState>(OrganizeUiState.Loading)
    val uiState: StateFlow<OrganizeUiState> = _uiState.asStateFlow()

    init {
        loadLayout()
    }

    private fun loadLayout() {
        viewModelScope.launch {
            screenRepository.getScreenLayout().collect { pages ->
                val currentProfiles = loadProfiles()
                _uiState.value = OrganizeUiState.Editing(
                    pages = pages.ifEmpty {
                        listOf(ScreenPage(pageIndex = 0, items = emptyList()))
                    },
                    profiles = currentProfiles
                )
            }
        }
    }

    fun onMoveItem(fromPage: Int, fromPos: Int, toPage: Int, toPos: Int) {
        viewModelScope.launch {
            screenRepository.moveItem(fromPage, fromPos, toPage, toPos)
        }
    }

    fun onCreateFolder(pageIndex: Int, position: Int, name: String, apps: List<String>) {
        viewModelScope.launch {
            screenRepository.createFolder(pageIndex, position, name, apps)
        }
    }

    fun onDeleteFolder(folderId: Long) {
        viewModelScope.launch {
            screenRepository.deleteFolder(folderId)
        }
    }

    fun onRenameFolder(folderId: Long, newName: String) {
        viewModelScope.launch {
            screenRepository.renameFolder(folderId, newName)
        }
    }

    fun onSaveProfile(name: String) {
        viewModelScope.launch {
            val state = _uiState.value as? OrganizeUiState.Editing ?: return@launch
            val now = System.currentTimeMillis()

            val count = profileDao.getProfileCount()
            if (count >= 10) {
                Timber.w("Max 10 profiles reached")
                return@launch
            }

            val profileEntity = ProfileEntity(
                name = name,
                createdAt = now,
                updatedAt = now,
                isActive = true,
                source = ProfileSource.USER.name
            )

            val items = state.pages.flatMap { page ->
                page.items.map { item ->
                    ProfileItemEntity(
                        profileId = 0,
                        pageIndex = page.pageIndex,
                        position = item.position,
                        type = when (item) {
                            is ScreenItem.AppShortcut -> "app"
                            is ScreenItem.Folder -> "folder"
                            is ScreenItem.WebShortcut -> "web"
                        },
                        packageName = when (item) {
                            is ScreenItem.AppShortcut -> item.appInfo.packageName
                            is ScreenItem.Folder -> null
                            is ScreenItem.WebShortcut -> item.url
                        },
                        folderName = when (item) {
                            is ScreenItem.Folder -> item.name
                            is ScreenItem.WebShortcut -> item.label
                            else -> null
                        },
                        folderColor = when (item) {
                            is ScreenItem.Folder -> item.color
                            else -> null
                        }
                    )
                }
            }

            profileDao.saveAndActivateProfile(profileEntity, items)
        }
    }

    fun onLoadProfile(profileId: Long) {
        viewModelScope.launch {
            profileDao.deactivateAllProfiles()
            profileDao.activateProfile(profileId)
            // Reload layout from profile
            Timber.d("Loading profile $profileId")
        }
    }

    fun onDeleteProfile(profileId: Long) {
        viewModelScope.launch {
            profileDao.deleteProfile(profileId)
        }
    }

    private suspend fun loadProfiles(): List<OrganizationProfile> {
        return try {
            val entities = profileDao.getAllProfiles()
            var result = emptyList<OrganizationProfile>()
            entities.collect { profiles ->
                result = profiles.map { entity ->
                    OrganizationProfile(
                        id = entity.id,
                        name = entity.name,
                        createdAt = entity.createdAt,
                        updatedAt = entity.updatedAt,
                        pages = emptyList(),
                        isActive = entity.isActive,
                        source = ProfileSource.valueOf(entity.source)
                    )
                }
            }
            result
        } catch (e: Exception) {
            Timber.e(e, "Failed to load profiles")
            emptyList()
        }
    }
}
