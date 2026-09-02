package com.example.praxim.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.example.praxim.MainActivity
import com.example.praxim.data.PraximDatabase
import com.example.praxim.data.ScanHistoryEntity
import com.example.praxim.data.ScanHistoryRepository
import com.example.praxim.engine.EntityRecognizerEngine
import com.example.praxim.model.RecognizedEntity
import com.example.praxim.ui.hud.EdgePillView
import com.example.praxim.ui.hud.ExpandedActionHudView
import com.example.praxim.ui.hud.HudDisplayMode
import com.example.praxim.ui.hud.HudSettings
import com.example.praxim.ui.hud.ProcessingShimmerView
import com.example.praxim.ui.theme.PraximTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OverlayHUDService : LifecycleService(), SavedStateRegistryOwner, ViewModelStoreOwner {

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var windowManager: OverlayWindowManager? = null
    private var screenCaptureManager: ScreenCaptureManager? = null

    private val _displayMode = MutableStateFlow<HudDisplayMode>(HudDisplayMode.Collapsed)
    val displayMode = _displayMode.asStateFlow()

    private val _hudSettings = MutableStateFlow(HudSettings())
    val hudSettings = _hudSettings.asStateFlow()

    private lateinit var repository: ScanHistoryRepository

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)

        val database = PraximDatabase.getDatabase(this)
        repository = ScanHistoryRepository(database.scanHistoryDao())

        startForegroundServiceNotification()

        windowManager = OverlayWindowManager(this, this, this, this)
        windowManager?.mount {
            val mode by displayMode.collectAsState()
            val settings by hudSettings.collectAsState()

            PraximTheme {
                when (val currentMode = mode) {
                    is HudDisplayMode.Collapsed -> {
                        EdgePillView(
                            settings = settings,
                            onTriggerScan = { triggerOnDeviceScan() }
                        )
                    }
                    is HudDisplayMode.Processing -> {
                        ProcessingShimmerView()
                    }
                    is HudDisplayMode.Expanded -> {
                        ExpandedActionHudView(
                            entities = currentMode.entities,
                            onDismiss = {
                                _displayMode.value = HudDisplayMode.Collapsed
                                windowManager?.updateLayout(expanded = false)
                            },
                            onEntityActionExecuted = { entity, action ->
                                recordEntityActionInHistory(entity, action)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
                mediaProjection?.let { screenCaptureManager = ScreenCaptureManager(this, it) }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun triggerOnDeviceScan() {
        serviceScope.launch {
            _displayMode.value = HudDisplayMode.Processing
            windowManager?.updateLayout(expanded = true)

            screenCaptureManager?.captureSingleFrame(
                onFrameReady = { buffer, width, height, captureTimestamp ->
                    serviceScope.launch(Dispatchers.Default) {
                        try {
                            val parsed = EntityRecognizerEngine.processFrame(buffer, width, height)

                            val endTimestamp = System.currentTimeMillis()
                            val latency = endTimestamp - captureTimestamp
                            Log.i("PraximPerformance", "End-to-End Trigger-to-Render Latency: ${latency}ms")

                            withContext(Dispatchers.Main) {
                                if (parsed.isNotEmpty()) {
                                    _displayMode.value = HudDisplayMode.Expanded(parsed)

                                    // Update layout must be on main thread
                                    windowManager?.updateLayout(expanded = true)

                                    parsed.forEach { entity ->
                                        repository.insert(
                                            ScanHistoryEntity(
                                                rawText = entity.rawText,
                                                formattedValue = entity.formattedValue,
                                                entityType = entity.type.name,
                                                primaryActionLabel = entity.primaryActionLabel,
                                                timestamp = entity.timestamp
                                            )
                                        )
                                    }
                                } else {
                                    // Briefly show processing then collapse if nothing found
                                    delay(300)
                                    _displayMode.value = HudDisplayMode.Collapsed
                                    windowManager?.updateLayout(expanded = false)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            withContext(Dispatchers.Main) {
                                _displayMode.value = HudDisplayMode.Collapsed
                                windowManager?.updateLayout(expanded = false)
                            }
                        }
                    }
                },
                onError = {
                    it.printStackTrace()
                    serviceScope.launch(Dispatchers.Main) {
                        _displayMode.value = HudDisplayMode.Collapsed
                        windowManager?.updateLayout(expanded = false)
                    }
                }
            ) ?: run {
                // Fallback if ScreenCaptureManager is not initialized
                _displayMode.value = HudDisplayMode.Collapsed
                windowManager?.updateLayout(expanded = false)
            }
        }
    }

    private fun recordEntityActionInHistory(entity: RecognizedEntity, actionTaken: String) {
        serviceScope.launch {
            repository.insert(
                ScanHistoryEntity(
                    rawText = entity.rawText,
                    formattedValue = entity.formattedValue,
                    entityType = entity.type.name,
                    primaryActionLabel = entity.primaryActionLabel,
                    actionExecuted = actionTaken,
                    timestamp = System.currentTimeMillis()
                )
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
            manager?.createNotificationChannel(channel)
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1001, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        screenCaptureManager?.destroy()
        screenCaptureManager = null
        windowManager?.destroy()
        store.clear()
        windowManager = null
    }

    companion object {
        fun start(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                return
            }

            val intent = Intent(context, OverlayHUDService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, OverlayHUDService::class.java)
            context.stopService(intent)
        }
    }
}
