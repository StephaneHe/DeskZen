package com.deskzen.ui.launcher

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import timber.log.Timber

class DeskZenWidgetHost(context: Context, hostId: Int) : AppWidgetHost(context, hostId) {
    companion object {
        const val HOST_ID = 1024
        private const val PREFS_NAME = "deskzen_widgets"
        private const val KEY_WIDGET_IDS = "active_widget_ids"
    }
}

class WidgetManager(private val context: Context) {

    val appWidgetHost = DeskZenWidgetHost(context, DeskZenWidgetHost.HOST_ID)
    val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
    private val prefs = context.getSharedPreferences("deskzen_widgets", Context.MODE_PRIVATE)

    fun startListening() {
        try {
            appWidgetHost.startListening()
        } catch (e: Exception) {
            Timber.e(e, "Failed to start widget host listening")
        }
    }

    fun stopListening() {
        try {
            appWidgetHost.stopListening()
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop widget host listening")
        }
    }

    fun allocateWidgetId(): Int {
        return appWidgetHost.allocateAppWidgetId()
    }

    fun deallocateWidgetId(widgetId: Int) {
        appWidgetHost.deleteAppWidgetId(widgetId)
    }

    fun createWidgetView(widgetId: Int): AppWidgetHostView? {
        return try {
            val widgetInfo = appWidgetManager.getAppWidgetInfo(widgetId) ?: return null
            val view = appWidgetHost.createView(context, widgetId, widgetInfo)
            view.setAppWidget(widgetId, widgetInfo)
            view
        } catch (e: Exception) {
            Timber.e(e, "Failed to create widget view for id $widgetId")
            null
        }
    }

    fun getInstalledWidgets(): List<AppWidgetProviderInfo> {
        return appWidgetManager.installedProviders
    }

    fun getBindIntent(widgetId: Int, provider: AppWidgetProviderInfo): Intent {
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
        return intent
    }

    fun bindWidget(widgetId: Int, provider: AppWidgetProviderInfo): Boolean {
        return appWidgetManager.bindAppWidgetIdIfAllowed(widgetId, provider.provider)
    }

    // Persistence
    fun saveWidgetIds(ids: List<Int>) {
        prefs.edit()
            .putString("active_widget_ids", ids.joinToString(","))
            .apply()
    }

    fun loadWidgetIds(): List<Int> {
        val str = prefs.getString("active_widget_ids", "") ?: ""
        if (str.isBlank()) return emptyList()
        return str.split(",").mapNotNull { it.toIntOrNull() }
            .filter { appWidgetManager.getAppWidgetInfo(it) != null } // only valid widgets
    }
}
