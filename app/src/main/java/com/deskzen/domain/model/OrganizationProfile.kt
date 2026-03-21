package com.deskzen.domain.model

data class OrganizationProfile(
    val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val pages: List<ScreenPage>,
    val isActive: Boolean = false,
    val source: ProfileSource = ProfileSource.USER
)

enum class ProfileSource {
    USER,
    AI,
    IMPORTED
}
