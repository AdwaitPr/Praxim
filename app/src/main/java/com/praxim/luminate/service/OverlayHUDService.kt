package com.praxim.luminate.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.praxim.engine.pipeline.HudUiState
import com.praxim.engine.pipeline.RealTimePipelineOrchestrator
import com.praxim.luminate.ui.PraximHudRootView
import kotlinx.coroutines.launch

class OverlayHUDService : LifecycleService(), SavedStateRegistryOwner, ViewModelStoreOwner {

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var pipelineOrchestrator: RealTimePipelineOrchestrator? = null

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    // Hide overlay, pause capture by stopping pipeline
                    composeView?.visibility = android.view.View.GONE
                    pipelineOrchestrator?.stopPipeline()
                    pipelineOrchestrator = null
                }
                Intent.ACTION_USER_PRESENT -> {
                    // Restore overlay
                    composeView?.visibility = android.view.View.VISIBLE
                    // In a real app we'd need to re-acquire MediaProjection token here if it was revoked,
                    // but for this assignment we rely on the single-use token from the initial launch.
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenStateReceiver, filter)

        setupWindowManager()
    }

    private fun setupWindowManager() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
            // Clamp alpha to bypass Android 12+ untrusted touch limits
            alpha = 0.8f
        }

        composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setViewTreeLifecycleOwner(this@OverlayHUDService)
            setViewTreeSavedStateRegistryOwner(this@OverlayHUDService)
            setViewTreeViewModelStoreOwner(this@OverlayHUDService)
        }

        windowManager?.addView(composeView, layoutParams)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForegroundServiceNotification()

        if (intent?.action == "ACTION_START_WITH_PROJECTION") {
            val resultCode = intent.getIntExtra("EXTRA_RESULT_CODE", 0)
            val resultData: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra("EXTRA_RESULT_DATA", Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra("EXTRA_RESULT_DATA")
            }

            if (resultCode != 0 && resultData != null) {
                val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                val mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)
                if (mediaProjection != null) {
                    startPipeline(mediaProjection)
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun startPipeline(mediaProjection: MediaProjection) {
        val metrics = resources.displayMetrics
        pipelineOrchestrator = RealTimePipelineOrchestrator(
            context = this,
            mediaProjection = mediaProjection,
            width = metrics.widthPixels,
            height = metrics.heightPixels,
            densityDpi = metrics.densityDpi
        )

        composeView?.setContent {
            val hudState by pipelineOrchestrator!!.hudState.collectAsState()

            // Dynamic focus switching
            androidx.compose.runtime.LaunchedEffect(hudState) {
                val requiresFocus = hudState is HudUiState.ActionReady || hudState is HudUiState.ThreatDetected
                if (requiresFocus) {
                    layoutParams?.flags = layoutParams!!.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                } else {
                    layoutParams?.flags = layoutParams!!.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                }
                windowManager?.updateViewLayout(composeView, layoutParams)
            }

            PraximHudRootView(
                hudState = hudState,
                onDismiss = {
                    // For now just manually setting layout params back to not focusable
                    layoutParams?.flags = layoutParams!!.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    windowManager?.updateViewLayout(composeView, layoutParams)
                }
            )
        }
    }

    private fun startForegroundServiceNotification() {
        val channelId = "praxim_hud_channel"
        val channelName = "Praxim Micro-HUD Intelligence Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Luminate On-Device Intelligence")
            .setContentText("Edge Handle active • 100% On-Device Screen recognition")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                1001,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(1001, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pipelineOrchestrator?.stopPipeline()
        if (composeView != null) {
            windowManager?.removeView(composeView)
        }
        unregisterReceiver(screenStateReceiver)
        store.clear()
    }
}
