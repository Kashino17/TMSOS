package com.tms.os.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.tms.os.launcher.ui.LauncherHomeScreen
import com.tms.os.launcher.viewmodel.LauncherViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // As a Home Launcher, pressing Back closes open modals/folders/search,
                    // and never exits the Home Screen!
                    viewModel.handleBackOrHomePress()
                }
            }
        )

        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                LauncherHomeScreen(
                    viewModel = viewModel,
                    onOpenSettingsActivity = {
                        val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                        startActivity(intent)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Pressing the Home button while already in the launcher resets open overlays
        viewModel.handleBackOrHomePress()
        viewModel.refreshSystemStatus()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshSystemStatus()
    }
}
