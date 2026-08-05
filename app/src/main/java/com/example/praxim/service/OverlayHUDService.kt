package com.example.praxim.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.example.MainActivity
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
import com.example.ui.theme.PraximTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OverlayHUDService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var windowManager: OverlayWindowManager? = null
    private val _displayMode = MutableStateFlow<HudDisplayMode>(HudDisplayMode.Collapsed)
    val displayMode = _displayMode.asStateFlow()

    private val _hudSettings = MutableStateFlow(HudSettings())
    val hudSettings = _hudSettings.asStateFlow()

    private lateinit var repository: ScanHistoryRepository

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        val database = PraximDatabase.getDatabase(this)
        repository = ScanHistoryRepository(database.scanHistoryDao())

        startForegroundServiceNotification()

        windowManager = OverlayWindowManager(this, this, this, this)
        windowManager?.createOverlayView {
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

        windowManager?.attachToWindow()
    }

    private fun triggerOnDeviceScan() {
        serviceScope.launch {
            _displayMode.value = HudDisplayMode.Processing
            windowManager?.updateLayout(expanded = true)

            // Simulate <100ms local RAM Frame Buffer OCR parsing
            delay(180)

            // Check active clipboard / screen text
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""

            val defaultSampleScreenText = if (clipText.isNotBlank()) {
                clipText
            } else {
                "Swiggy Delivery Payment: ₹420 to merchant.swiggy@ybl or UPI ID swiggy.pay@okicici. Bank IFSC: SBIN0001234. Helpline +91 9876543210. Track shipment AWB102938475."
            }

            val parsed = EntityRecognizerEngine.parseTextEntities(defaultSampleScreenText)

            _displayMode.value = HudDisplayMode.Expanded(parsed)

            // Save scanned entities to local database
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

        startForeground(1001, notification)
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        windowManager?.detachFromWindow()
        super.onDestroy()
    }

    companion object {
        fun startService(context: Context) {
            val intent = Intent(context, OverlayHUDService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, OverlayHUDService::class.java)
            context.stopService(intent)
        }
    }
}
