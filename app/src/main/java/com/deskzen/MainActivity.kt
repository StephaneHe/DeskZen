package com.deskzen

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.deskzen.ui.launcher.LauncherScreen
import com.deskzen.ui.launcher.LauncherViewModel
import com.deskzen.ui.theme.DeskZenTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Pending widget ID waiting for bind/config result
    var pendingWidgetId: Int = -1
    var onWidgetReady: ((Int) -> Unit)? = null

    val widgetBindLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Timber.d("Widget bind result: ${result.resultCode}, widgetId: $pendingWidgetId")
        if (result.resultCode == RESULT_OK && pendingWidgetId != -1) {
            onWidgetReady?.invoke(pendingWidgetId)
        }
        pendingWidgetId = -1
    }

    val widgetConfigLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Timber.d("Widget config result: ${result.resultCode}, widgetId: $pendingWidgetId")
        if (result.resultCode == RESULT_OK && pendingWidgetId != -1) {
            onWidgetReady?.invoke(pendingWidgetId)
        }
        pendingWidgetId = -1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DeskZenTheme {
                LauncherScreen()
            }
        }
    }

    @Deprecated("Use onBackPressedDispatcher")
    override fun onBackPressed() {
        // Launcher should not go back
    }
}
