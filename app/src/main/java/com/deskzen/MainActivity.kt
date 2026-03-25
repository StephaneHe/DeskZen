package com.deskzen

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.deskzen.ui.launcher.LauncherScreen
import com.deskzen.ui.launcher.LauncherViewModel
import com.deskzen.ui.theme.DeskZenTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()
    private var lastHomePressMs = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DeskZenTheme {
                LauncherScreen()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN) {
            val now = System.currentTimeMillis()
            if (now - lastHomePressMs < 500) {
                viewModel.onHomeDoubleTap()
            }
            lastHomePressMs = now
        }
    }

    @Deprecated("Use onBackPressedDispatcher")
    override fun onBackPressed() {
        // Launcher should not go back
    }
}
