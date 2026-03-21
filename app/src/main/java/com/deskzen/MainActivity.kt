package com.deskzen

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.deskzen.ui.launcher.LauncherScreen
import com.deskzen.ui.theme.DeskZenTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var pendingWidgetCallback: ((Boolean) -> Unit)? = null

    val widgetBindLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val success = result.resultCode == RESULT_OK
            Timber.d("Widget bind result: $success")
            pendingWidgetCallback?.invoke(success)
            pendingWidgetCallback = null
        }

    val widgetConfigLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val success = result.resultCode == RESULT_OK
            Timber.d("Widget config result: $success")
            pendingWidgetCallback?.invoke(success)
            pendingWidgetCallback = null
        }

    fun requestWidgetBind(intent: Intent, callback: (Boolean) -> Unit) {
        pendingWidgetCallback = callback
        widgetBindLauncher.launch(intent)
    }

    fun launchWidgetConfig(intent: Intent, callback: (Boolean) -> Unit) {
        pendingWidgetCallback = callback
        widgetConfigLauncher.launch(intent)
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
