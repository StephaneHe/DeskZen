package com.deskzen.domain.usecase

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.deskzen.domain.model.AppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManageShortcutUseCaseImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ManageShortcutUseCase {

    override suspend fun createShortcut(appInfo: AppInfo): ShortcutResult {
        return withContext(Dispatchers.IO) {
            if (!canPinShortcuts()) return@withContext ShortcutResult.NotSupported

            try {
                val launchIntent = context.packageManager
                    .getLaunchIntentForPackage(appInfo.packageName)
                    ?: return@withContext ShortcutResult.Error(
                        "Impossible de lancer ${appInfo.label}"
                    )

                val pm = context.packageManager
                val iconCompat = getAppIcon(pm, appInfo.packageName)
                    ?: IconCompat.createWithResource(context, android.R.mipmap.sym_def_app_icon)

                val shortcutInfo = ShortcutInfoCompat.Builder(
                    context,
                    "deskzen_${appInfo.packageName}"
                )
                    .setShortLabel(appInfo.label)
                    .setIcon(iconCompat)
                    .setIntent(launchIntent)
                    .build()

                val success = ShortcutManagerCompat.requestPinShortcut(
                    context, shortcutInfo, null
                )

                if (success) ShortcutResult.Success
                else ShortcutResult.Error("Le launcher ne supporte pas les raccourcis pinnés")
            } catch (e: Exception) {
                Timber.e(e, "Failed to create shortcut for ${appInfo.packageName}")
                ShortcutResult.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    override suspend fun removeShortcut(packageName: String): ShortcutResult {
        return withContext(Dispatchers.IO) {
            try {
                ShortcutManagerCompat.removeDynamicShortcuts(
                    context,
                    listOf("deskzen_$packageName")
                )
                ShortcutResult.Success
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove shortcut for $packageName")
                ShortcutResult.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    override suspend fun isShortcutPinned(packageName: String): Boolean {
        return ShortcutManagerCompat.getShortcuts(context, ShortcutManagerCompat.FLAG_MATCH_PINNED)
            .any { it.id == "deskzen_$packageName" }
    }

    override fun canPinShortcuts(): Boolean {
        return ShortcutManagerCompat.isRequestPinShortcutSupported(context)
    }

    private fun getAppIcon(pm: PackageManager, packageName: String): IconCompat? {
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val drawable = pm.getApplicationIcon(appInfo)
            val size = 192 // Standard launcher icon size

            when (drawable) {
                is AdaptiveIconDrawable -> {
                    // Render the full adaptive icon as-is (foreground + background)
                    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    drawable.setBounds(0, 0, size, size)
                    drawable.draw(canvas)
                    IconCompat.createWithBitmap(bitmap)
                }
                is BitmapDrawable -> {
                    IconCompat.createWithBitmap(drawable.bitmap)
                }
                else -> {
                    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    drawable.setBounds(0, 0, size, size)
                    drawable.draw(canvas)
                    IconCompat.createWithBitmap(bitmap)
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to get icon for $packageName")
            null
        }
    }
}
