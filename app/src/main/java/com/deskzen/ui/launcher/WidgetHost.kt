package com.deskzen.ui.launcher

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.pm.PackageManager

data class WidgetInfo(
    val providerInfo: AppWidgetProviderInfo,
    val label: String,
    val icon: android.graphics.drawable.Drawable?,
    val appLabel: String
)

fun getAvailableWidgets(context: Context): List<WidgetInfo> {
    val pm = context.packageManager
    val awm = android.appwidget.AppWidgetManager.getInstance(context)
    return awm.installedProviders.mapNotNull { info ->
        try {
            WidgetInfo(
                providerInfo = info,
                label = info.loadLabel(pm) ?: info.provider.className,
                icon = info.loadIcon(context, 0),
                appLabel = try {
                    pm.getApplicationLabel(
                        pm.getApplicationInfo(info.provider.packageName, 0)
                    ).toString()
                } catch (_: Exception) { info.provider.packageName }
            )
        } catch (_: Exception) { null }
    }.sortedBy { it.appLabel }
}
