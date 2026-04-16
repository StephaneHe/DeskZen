package com.deskzen.ui.launcher

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader

data class BackupStandaloneItem(
    val pageIndex: Int,
    val position: Int,
    val packageName: String? = null,
    val webUrl: String? = null,
    val webLabel: String? = null
)

data class BackupQuickContact(
    val position: Int,
    val contactName: String,
    val phoneNumber: String,
    val action: String
)

data class BackupData(
    val customFolders: List<String>,
    val manualPlacements: Map<String, String>,
    val standaloneItems: List<BackupStandaloneItem> = emptyList(),
    val quickContacts: List<BackupQuickContact?> = List(8) { null },
    val version: Int = 1
)

object BackupManager {

    fun exportToJson(
        customFolders: Set<String>,
        manualPlacements: Map<String, String>,
        standaloneItems: List<BackupStandaloneItem>,
        quickContacts: List<BackupQuickContact?> = emptyList()
    ): String {
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

        val standaloneArray = JSONArray()
        standaloneItems.forEach { item ->
            val obj = JSONObject()
            obj.put("pageIndex", item.pageIndex)
            obj.put("position", item.position)
            item.packageName?.let { obj.put("packageName", it) }
            item.webUrl?.let { obj.put("webUrl", it) }
            item.webLabel?.let { obj.put("webLabel", it) }
            standaloneArray.put(obj)
        }
        json.put("standaloneItems", standaloneArray)

        val contactsArray = JSONArray()
        quickContacts.forEach { contact ->
            if (contact != null) {
                val obj = JSONObject()
                obj.put("position", contact.position)
                obj.put("name", contact.contactName)
                obj.put("phone", contact.phoneNumber)
                obj.put("action", contact.action)
                contactsArray.put(obj)
            }
        }
        json.put("quickContacts", contactsArray)

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

            val standalone = mutableListOf<BackupStandaloneItem>()
            val standaloneArray = json.optJSONArray("standaloneItems")
            if (standaloneArray != null) {
                for (i in 0 until standaloneArray.length()) {
                    val obj = standaloneArray.getJSONObject(i)
                    standalone.add(BackupStandaloneItem(
                        pageIndex = obj.getInt("pageIndex"),
                        position = obj.getInt("position"),
                        packageName = obj.optString("packageName").ifEmpty { null },
                        webUrl = obj.optString("webUrl").ifEmpty { null },
                        webLabel = obj.optString("webLabel").ifEmpty { null }
                    ))
                }
            }

            val quickContacts = MutableList<BackupQuickContact?>(8) { null }
            val contactsArray = json.optJSONArray("quickContacts")
            if (contactsArray != null) {
                for (i in 0 until contactsArray.length()) {
                    val obj = contactsArray.getJSONObject(i)
                    val pos = obj.getInt("position")
                    if (pos in 0..7) {
                        quickContacts[pos] = BackupQuickContact(
                            position = pos,
                            contactName = obj.getString("name"),
                            phoneNumber = obj.getString("phone"),
                            action = obj.optString("action", "CALL_PHONE")
                        )
                    }
                }
            }

            BackupData(
                customFolders = folders,
                manualPlacements = placements,
                standaloneItems = standalone,
                quickContacts = quickContacts,
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
