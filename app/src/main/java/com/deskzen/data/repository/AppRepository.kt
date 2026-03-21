package com.deskzen.data.repository

import com.deskzen.domain.model.AppInfo

interface AppRepository {
    suspend fun getInstalledApps(includeSystem: Boolean = false): List<AppInfo>
    suspend fun getAppInfo(packageName: String): AppInfo?
    suspend fun searchApps(query: String, includeSystem: Boolean = false): List<AppInfo>
}
