package com.deskzen.ui.launcher

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader

data class BackupData(
    val customFolders: List<String>,
    val manualPlacements: Map<String, String>,
    val version: Int = 1
)

object BackupManager {

    fun exportToJson(customFolders: Set<String>, manualPlacements: Map<String, String>): String {
        val json = JSONObject()
        json.put("version", 1)
        json.put("app", "DeskZen")

        val foldersArray = JSONArray()
        customFolders.forEach { foldersArray.put(it) }
        json.put("customFolders", foldersArray)

        val placementsObj = JSONObject()
        manualPlacements.forEach { (pkg, folder) ->
            placementsObj.put(pkg, folder)
        }
        json.put("manualPlacements", placementsObj)

        return json.toString(2)
    }

    fun importFromJson(jsonString: String): BackupData? {
        return try {
            val json = JSONObject(jsonString)
            val version = json.optInt("version", 1)

            val folders = mutableListOf<String>()
            val foldersArray = json.optJSONArray("customFolders")
            if (foldersArray != null) {
                for (i in 0 until foldersArray.length()) {
                    folders.add(foldersArray.getString(i))
                }
            }

            val placements = mutableMapOf<String, String>()
            val placementsObj = json.optJSONObject("manualPlacements")
            if (placementsObj != null) {
                placementsObj.keys().forEach { key ->
                    placements[key] = placementsObj.getString(key)
                }
            }

            BackupData(
                customFolders = folders,
                manualPlacements = placements,
                version = version
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse backup JSON")
            null
        }
    }

    fun writeToUri(context: Context, uri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(content.toByteArray())
            }
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to write backup")
            false
        }
    }

    fun readFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).readText()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to read backup")
            null
        }
    }
}
