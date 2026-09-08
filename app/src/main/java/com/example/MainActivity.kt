package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings as SettingsIcon
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.praxim.data.PraximDatabase
import com.example.praxim.data.ScanHistoryRepository
import com.example.praxim.service.OverlayHUDService
import com.example.praxim.ui.screens.HistoryAuditScreen
import com.example.praxim.ui.screens.HomeScreen
import com.example.praxim.ui.screens.InteractiveSimulatorScreen
import com.example.praxim.ui.screens.SettingsScreen
import com.example.praxim.ui.theme.AmoledBlack
import com.example.praxim.ui.theme.DarkObsidian
import com.example.praxim.ui.theme.NeonGreen
import com.example.praxim.ui.theme.PraximTheme
import com.example.praxim.ui.theme.TextPrimaryDark
import com.example.praxim.ui.theme.TextSecondaryDark

class MainActivity : ComponentActivity() {

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data != null) {
                val intent = Intent(this, OverlayHUDService::class.java).apply {
                    action = "ACTION_START_WITH_PROJECTION"
                    putExtra("EXTRA_RESULT_CODE", result.resultCode)
                    putExtra("EXTRA_RESULT_DATA", data)
                }
                ContextCompat.startForegroundService(this, intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = PraximDatabase.getDatabase(this)
        val repository = ScanHistoryRepository(database.scanHistoryDao())

        setContent {
            PraximTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: "home"

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = AmoledBlack,
                    bottomBar = {
                        NavigationBar(
                            containerColor = DarkObsidian,
                            contentColor = TextPrimaryDark,
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            NavigationBarItem(
                                selected = currentRoute == "home",
                                onClick = { navController.navigate("home") { popUpTo("home") { saveState = true } } },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                label = { Text("Home") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AmoledBlack,
                                    selectedTextColor = NeonGreen,
                                    indicatorColor = NeonGreen,
                                    unselectedIconColor = TextSecondaryDark,
                                    unselectedTextColor = TextSecondaryDark
                                )
                            )

                            NavigationBarItem(
                                selected = currentRoute == "simulator",
                                onClick = { navController.navigate("simulator") { popUpTo("home") { saveState = true } } },
                                icon = { Icon(Icons.Default.PlayCircle, contentDescription = "Simulator") },
                                label = { Text("Simulator") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AmoledBlack,
                                    selectedTextColor = NeonGreen,
                                    indicatorColor = NeonGreen,
                                    unselectedIconColor = TextSecondaryDark,
                                    unselectedTextColor = TextSecondaryDark
                                )
                            )

                            NavigationBarItem(
                                selected = currentRoute == "history",
                                onClick = { navController.navigate("history") { popUpTo("home") { saveState = true } } },
                                icon = { Icon(Icons.Default.Security, contentDescription = "Audit Log") },
                                label = { Text("Audit Log") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AmoledBlack,
                                    selectedTextColor = NeonGreen,
                                    indicatorColor = NeonGreen,
                                    unselectedIconColor = TextSecondaryDark,
                                    unselectedTextColor = TextSecondaryDark
                                )
                            )

                            NavigationBarItem(
                                selected = currentRoute == "settings",
                                onClick = { navController.navigate("settings") { popUpTo("home") { saveState = true } } },
                                icon = { Icon(Icons.Default.SettingsIcon, contentDescription = "Settings") },
                                label = { Text("Settings") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AmoledBlack,
                                    selectedTextColor = NeonGreen,
                                    indicatorColor = NeonGreen,
                                    unselectedIconColor = TextSecondaryDark,
                                    unselectedTextColor = TextSecondaryDark
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") {
                            HomeScreen(
                                repository = repository,
                                onNavigateSimulator = { navController.navigate("simulator") },
                                onNavigateHistory = { navController.navigate("history") },
                                onStartOverlayService = { startOverlayService() }
                            )
                        }

                        composable("simulator") {
                            InteractiveSimulatorScreen(
                                repository = repository,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("history") {
                            HistoryAuditScreen(
                                repository = repository,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun startOverlayService() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return
        }

        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        screenCaptureLauncher.launch(projectionManager.createScreenCaptureIntent())
    }
}
