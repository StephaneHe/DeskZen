package com.deskzen

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.deskzen.ui.launcher.LauncherScreen
import com.deskzen.ui.theme.DeskZenTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        const val WIDGET_HOST_ID = 1024
        const val PREFS_WIDGETS = "deskzen_widgets"
        const val KEY_WIDGET_IDS = "active_widget_ids"
    }

    lateinit var appWidgetHost: AppWidgetHost
    lateinit var appWidgetManager: AppWidgetManager
    private lateinit var widgetContainer: LinearLayout

    private var pendingWidgetId = -1

    val widgetPickLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val widgetId = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
            if (widgetId != -1) {
                val widgetInfo = appWidgetManager.getAppWidgetInfo(widgetId)
                if (widgetInfo?.configure != null) {
                    pendingWidgetId = widgetId
                    configureWidget(widgetId, widgetInfo)
                } else {
                    saveWidgetId(widgetId)
                    addWidgetToContainer(widgetId)
                }
            }
        } else {
            val extras = result.data?.extras
            val widgetId = extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
            if (widgetId != -1) appWidgetHost.deleteAppWidgetId(widgetId)
        }
    }

    val widgetConfigLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && pendingWidgetId != -1) {
            saveWidgetId(pendingWidgetId)
            addWidgetToContainer(pendingWidgetId)
        } else if (pendingWidgetId != -1) {
            appWidgetHost.deleteAppWidgetId(pendingWidgetId)
        }
        pendingWidgetId = -1
    }

    fun launchNativeWidgetPicker() {
        val widgetId = appWidgetHost.allocateAppWidgetId()
        pendingWidgetId = widgetId
        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        widgetPickLauncher.launch(pickIntent)
    }

    private fun configureWidget(widgetId: Int, widgetInfo: AppWidgetProviderInfo) {
        val configIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
            component = widgetInfo.configure
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        widgetConfigLauncher.launch(configIntent)
    }

    fun addWidgetToContainer(widgetId: Int) {
        try {
            val widgetInfo = appWidgetManager.getAppWidgetInfo(widgetId) ?: return
            val hostView = appWidgetHost.createView(this, widgetId, widgetInfo)
            hostView.setAppWidget(widgetId, widgetInfo)

            // Size
            val density = resources.displayMetrics.density
            val minH = ((widgetInfo.minHeight) * density).toInt().coerceAtLeast((80 * density).toInt())

            // Remove button
            val wrapper = FrameLayout(this)
            wrapper.addView(hostView, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, minH
            ))

            val closeBtn = android.widget.ImageButton(this).apply {
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                setBackgroundColor(0x80000000.toInt())
                setPadding(8, 8, 8, 8)
                setOnClickListener {
                    removeWidgetFromContainer(widgetId, wrapper)
                }
            }
            val closeParams = FrameLayout.LayoutParams(
                (32 * density).toInt(), (32 * density).toInt()
            ).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.END
                setMargins(0, (4 * density).toInt(), (4 * density).toInt(), 0)
            }
            wrapper.addView(closeBtn, closeParams)

            widgetContainer.addView(wrapper, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))

            Timber.d("Widget $widgetId added to container")
        } catch (e: Exception) {
            Timber.e(e, "Failed to add widget $widgetId")
        }
    }

    private fun removeWidgetFromContainer(widgetId: Int, view: android.view.View) {
        widgetContainer.removeView(view)
        appWidgetHost.deleteAppWidgetId(widgetId)
        removeWidgetId(widgetId)
    }

    fun saveWidgetId(widgetId: Int) {
        val prefs = getSharedPreferences(PREFS_WIDGETS, MODE_PRIVATE)
        val ids = loadWidgetIds().toMutableList()
        if (widgetId !in ids) {
            ids.add(widgetId)
            prefs.edit().putString(KEY_WIDGET_IDS, ids.joinToString(",")).apply()
        }
    }

    fun loadWidgetIds(): List<Int> {
        val prefs = getSharedPreferences(PREFS_WIDGETS, MODE_PRIVATE)
        val str = prefs.getString(KEY_WIDGET_IDS, "") ?: ""
        if (str.isBlank()) return emptyList()
        return str.split(",").mapNotNull { it.toIntOrNull() }
            .filter { appWidgetManager.getAppWidgetInfo(it) != null }
    }

    fun removeWidgetId(widgetId: Int) {
        val ids = loadWidgetIds().toMutableList()
        ids.remove(widgetId)
        val prefs = getSharedPreferences(PREFS_WIDGETS, MODE_PRIVATE)
        prefs.edit().putString(KEY_WIDGET_IDS, ids.joinToString(",")).apply()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appWidgetManager = AppWidgetManager.getInstance(this)
        appWidgetHost = AppWidgetHost(this, WIDGET_HOST_ID)

        enableEdgeToEdge()

        // Root layout: widgets (native) on top, Compose below
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Widget container (native Android views)
        widgetContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        rootLayout.addView(widgetContainer)

        // Compose content
        val composeView = androidx.compose.ui.platform.ComposeView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f // take remaining space
            )
            setContent {
                DeskZenTheme {
                    LauncherScreen()
                }
            }
        }
        rootLayout.addView(composeView)

        setContentView(rootLayout)
    }

    override fun onStart() {
        super.onStart()
        appWidgetHost.startListening()

        // Restore saved widgets
        widgetContainer.removeAllViews()
        loadWidgetIds().forEach { widgetId ->
            addWidgetToContainer(widgetId)
        }
    }

    override fun onStop() {
        super.onStop()
        appWidgetHost.stopListening()
    }

    @Deprecated("Use onBackPressedDispatcher")
    override fun onBackPressed() {
        // Launcher should not go back
    }
}
