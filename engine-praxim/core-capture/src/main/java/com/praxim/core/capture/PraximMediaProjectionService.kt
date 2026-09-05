package com.praxim.core.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.ImageFormat
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat

class PraximMediaProjectionService : Service() {

    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplayManager: VirtualDisplayManager? = null
    private var imageReader: ImageReader? = null
    private var wakeLock: PowerManager.WakeLock? = null

    // In a real implementation this would be passed in or injected
    // private var pipeline: ResilientFrameIngestionPipeline? = null

    override fun onCreate() {
        super.onCreate()
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        val resultCode = intent.getIntExtra("RESULT_CODE", -1)
        val data = intent.getParcelableExtra<Intent>("DATA")

        if (resultCode == -1 || data == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundService()

        mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, data)
        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                stopScreenCapture()
                stopSelf()
            }

            override fun onCapturedContentResize(width: Int, height: Int) {
                virtualDisplayManager?.resize(width, height, resources.displayMetrics.densityDpi)
            }
        }, null)

        startScreenCapture()

        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "praxim_capture_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Screen Capture", NotificationManager.IMPORTANCE_LOW)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Praxim Engine")
            .setContentText("Capturing screen...")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            notificationBuilder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }

        val notification = notificationBuilder.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(1, notification)
        }

        acquireWakeLock()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Praxim::CaptureWakeLock").apply {
            acquire()
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    private fun startScreenCapture() {
        if (mediaProjection == null) return

        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 2)
        // pipeline?.let { imageReader?.setOnImageAvailableListener(it, null) }

        virtualDisplayManager = VirtualDisplayManager(mediaProjection!!)
        virtualDisplayManager?.createVirtualDisplay(width, height, density, imageReader!!.surface)
    }

    private fun stopScreenCapture() {
        virtualDisplayManager?.release()
        imageReader?.close()
        mediaProjection?.stop()
        mediaProjection = null
        releaseWakeLock()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScreenCapture()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
