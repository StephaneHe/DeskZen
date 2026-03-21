package com.deskzen

import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.deskzen.ui.launcher.LauncherScreen
import com.deskzen.ui.launcher.WidgetManager
import com.deskzen.ui.theme.DeskZenTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    lateinit var widgetManager: WidgetManager
        private set

    // Pending widget ID stored across activity result
    private var pendingWidgetId: Int = -1

    val widgetBindLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val success = result.resultCode == RESULT_OK
        Timber.d("Widget bind result: success=$success, widgetId=$pendingWidgetId")
        if (success && pendingWidgetId != -1) {
            // Save the widget ID immediately via SharedPreferences
            val wm = widgetManager
            val currentIds = wm.loadWidgetIds().toMutableList()
            if (pendingWidgetId !in currentIds) {
                currentIds.add(pendingWidgetId)
                wm.saveWidgetIds(currentIds)
                Timber.d("Widget $pendingWidgetId saved to prefs, total: ${currentIds.size}")
            }
        } else if (pendingWidgetId != -1) {
            widgetManager.deallocateWidgetId(pendingWidgetId)
        }
        pendingWidgetId = -1
        // Recreate content to pick up new widget
        recreate()
    }

    fun requestWidgetBind(widgetId: Int, intent: android.content.Intent) {
        pendingWidgetId = widgetId
        Timber.d("Requesting widget bind for id=$widgetId")
        widgetBindLauncher.launch(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetManager = WidgetManager(applicationContext)
        widgetManager.startListening()
        enableEdgeToEdge()
        setContent {
            DeskZenTheme {
                LauncherScreen()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::widgetManager.isInitialized) {
            widgetManager.stopListening()
        }
    }

    @Deprecated("Use onBackPressedDispatcher")
    override fun onBackPressed() {
        // Launcher should not go back
    }
}
