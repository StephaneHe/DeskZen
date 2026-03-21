package com.deskzen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.deskzen.ui.launcher.LauncherScreen
import com.deskzen.ui.theme.DeskZenTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

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
