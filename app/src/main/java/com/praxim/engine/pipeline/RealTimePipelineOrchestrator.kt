package com.praxim.engine.pipeline

import android.content.Context
import android.graphics.PixelFormat
import android.hardware.HardwareBuffer
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.PowerManager
import android.util.Log
import com.praxim.core.data.local.entity.EntityType
import com.praxim.core.data.local.entity.ScanAuditEntity
import com.praxim.core.data.local.database.PraximDatabase
import com.praxim.core.data.local.database.ResilientDatabaseErrorHandler
import com.praxim.engine.analysis.ExtractedEntity
import com.praxim.engine.analysis.SpatialEntityExtractor
import com.praxim.engine.capture.BufferSecurityStatus
import com.praxim.engine.capture.NativeCaptureCore
import com.praxim.engine.slm.OnDeviceIntelligenceEngine
import com.praxim.engine.slm.ScamTriageClassifier
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

class RealTimePipelineOrchestrator(
    private val context: Context,
    private val mediaProjection: MediaProjection,
    private val width: Int,
    private val height: Int,
    private val densityDpi: Int
) {
    private val _hudState = MutableStateFlow<HudUiState>(HudUiState.Idle)
    val hudState: StateFlow<HudUiState> = _hudState.asStateFlow()

    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private val pipelineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val frameChannel = Channel<ScopedFrame>(
        capacity = Channel.CONFLATED,
        onUndeliveredElement = { frame -> frame.close() }
    )

    private val currentJob = AtomicReference<Job?>(null)
    private val nativeCaptureCore = NativeCaptureCore()
    private val scamTriageClassifier = ScamTriageClassifier(context)
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val intelligenceEngine = OnDeviceIntelligenceEngine.getInstance(context)

    private var frameCounter = 0L

    // For perceptual diffing
    private var lastLumaSummary: IntArray? = null

    init {
        startBackgroundThread()
        setupImageReader()
        setupVirtualDisplay()
        startProcessingLoop()
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("RealTimePipelineThread").apply { start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun setupImageReader() {
        // Usage flags as per requirements
        val usage = HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_CPU_READ_OFTEN
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2, usage).apply {
            setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                if (image != null) {
                    val hardwareBuffer = image.hardwareBuffer
                    if (hardwareBuffer != null) {
                        val scopedFrame = ScopedFrame(
                            image = image,
                            hardwareBuffer = hardwareBuffer,
                            frameIndex = frameCounter++,
                            timestampNs = System.nanoTime()
                        )
                        val offered = frameChannel.trySend(scopedFrame).isSuccess
                        if (!offered) {
                            scopedFrame.close()
                        }
                    } else {
                        image.close()
                    }
                }
            }, backgroundHandler)
        }
    }

    private fun setupVirtualDisplay() {
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "RealTimePipelineDisplay",
            width, height, densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, backgroundHandler
        )
    }

    private var lastFrameTimeMs = 0L

    private fun startProcessingLoop() {
        pipelineScope.launch {
            for (frame in frameChannel) {
                // Cancel previous job if it's still running (single-flight atomic cancellation)
                currentJob.getAndSet(null)?.cancel()

                val newJob = launch {
                    try {
                        processFrame(frame)
                    } finally {
                        frame.close()
                    }
                }
                currentJob.set(newJob)
            }
        }
    }

    private suspend fun processFrame(frame: ScopedFrame) {
        try {
            // Check thermal throttling
            var fpsLimit = 60
            var bypassGenerative = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val headroom = powerManager.getThermalHeadroom(5)
                if (headroom != Float.NaN) {
                    if (headroom < 0.60f) {
                        fpsLimit = 2
                        bypassGenerative = true
                    } else if (headroom < 0.85f) {
                        fpsLimit = 10
                    }
                }
            }

            val frameDelay = 1000L / fpsLimit
            val now = System.currentTimeMillis()
            val timeSinceLastFrame = now - lastFrameTimeMs
            if (timeSinceLastFrame < frameDelay) {
                delay(frameDelay - timeSinceLastFrame)
            }
            lastFrameTimeMs = System.currentTimeMillis()

            _hudState.value = HudUiState.Analyzing

            // Stage 1: Native audit
            val auditResult = nativeCaptureCore.auditBuffer(frame.hardwareBuffer, probePixels = true)
            when (auditResult.status) {
                BufferSecurityStatus.Permitted -> {
                    // Proceed
                }
                BufferSecurityStatus.ProtectedSurfaceViolation -> {
                    _hudState.value = HudUiState.SecureScreenDetected
                    return
                }
                BufferSecurityStatus.CircuitBreakerTripped -> {
                    _hudState.value = HudUiState.Throttled("CircuitBreakerTripped")
                    delay(1000) // Backoff
                    return
                }
                BufferSecurityStatus.PosixSignalTrapped, BufferSecurityStatus.InvalidGeometryOrStride -> {
                    // Discard silently
                    _hudState.value = HudUiState.Idle
                    return
                }
                else -> {
                    _hudState.value = HudUiState.Idle
                    return
                }
            }

            // Stage 1b: Perceptual differencing (simplified to skip downstream if nothing changes)
            val lumaChanged = hasLumaChanged(frame.image)
            if (!lumaChanged) {
                _hudState.value = HudUiState.Idle
                return
            }

            // Stage 2: Spatial OCR
            val (recognizedTextStr, extractedEntities) = SpatialEntityExtractor.extractFromMediaImage(frame.image)

            if (extractedEntities.isEmpty()) {
                _hudState.value = HudUiState.Idle
                return
            }

            // Stage 3 & 4: SLM Triage
            val triageResult = if (!bypassGenerative) {
                scamTriageClassifier.classifyTransaction(
                    recognizedTextStr,
                    extractedEntities.map { it.value }
                )
            } else {
                com.praxim.engine.slm.parser.TriageResult(isFraud = false, confidence = 0.5f, payee = "Unknown", amount = "Unknown", risk = "Unknown")
            }

            val outcome = AnalysisOutcome(triageResult, extractedEntities)
            if (triageResult.isFraud) {
                _hudState.value = HudUiState.ThreatDetected(outcome)
            } else {
                _hudState.value = HudUiState.ActionReady(outcome)
            }

            // Stage 5: Async audit logging (Fire and forget on IO)
            pipelineScope.launch(Dispatchers.IO) {
                logToDatabase(outcome)
            }

            // Artificial delay to mimic cognitive latency budget
            delay(16) // roughly 1 frame

        } catch (e: CancellationException) {
            // Job was cancelled by a newer frame, this is expected
        } catch (e: Exception) {
            Log.e("Pipeline", "Error processing frame", e)
            _hudState.value = HudUiState.Idle
        }
    }

    private fun hasLumaChanged(image: Image): Boolean {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val width = image.width
        val height = image.height

        // Calculate 8x8 downsampled luminance vector
        val currentLumaSummary = IntArray(64)
        val blockWidth = width / 8
        val blockHeight = height / 8
        if (blockWidth == 0 || blockHeight == 0) return true

        buffer.rewind()
        for (r in 0 until 8) {
            for (c in 0 until 8) {
                val startX = c * blockWidth
                val startY = r * blockHeight
                var sumLuma = 0
                for (y in startY until startY + blockHeight) {
                    for (x in startX until startX + blockWidth) {
                        val index = y * rowStride + x * pixelStride
                        if (index + 2 < buffer.capacity()) {
                            val rVal = buffer.get(index).toInt() and 0xFF
                            val gVal = buffer.get(index + 1).toInt() and 0xFF
                            val bVal = buffer.get(index + 2).toInt() and 0xFF
                            // standard luma formula approximation
                            sumLuma += (rVal * 299 + gVal * 587 + bVal * 114) / 1000
                        }
                    }
                }
                currentLumaSummary[r * 8 + c] = sumLuma / (blockWidth * blockHeight)
            }
        }

        var l1Distance = 0f
        lastLumaSummary?.let { last ->
            for (i in 0 until 64) {
                l1Distance += abs(currentLumaSummary[i] - last[i])
            }
            l1Distance /= (64 * 255f) // Normalize to 0..1
        }

        lastLumaSummary = currentLumaSummary

        // If it's the first frame (l1Distance == 0f because last was null), or if diff > 5%
        return lastLumaSummary == null || l1Distance > 0.05f
    }

    private suspend fun logToDatabase(outcome: AnalysisOutcome) {
        try {
            val pass = ByteArray(32) { 0 } // dummy pass
            val db = PraximDatabase.getDatabase(context, pass, ResilientDatabaseErrorHandler(context))
            val dao = db.scanAuditDao()

            for (entity in outcome.entities) {
                val entityType = when(entity.type) {
                    "VPA" -> EntityType.UPI
                    "IFSC" -> EntityType.IFSC
                    "PHONE" -> EntityType.PHONE
                    else -> continue
                }
                dao.insert(
                    ScanAuditEntity(
                        timestamp = System.currentTimeMillis(),
                        rawPayload = outcome.result.payee,
                        entityType = entityType,
                        targetAppPackage = null,
                        upiVpa = if (entityType == EntityType.UPI) entity.value else null,
                        ifscCode = if (entityType == EntityType.IFSC) entity.value else null,
                        phoneRawNumber = if (entityType == EntityType.PHONE) entity.value else null
                    )
                )
            }
        } catch(e: Exception) {
            Log.e("Pipeline", "Error saving to DB", e)
        }
    }

    fun stopPipeline() {
        pipelineScope.cancel()
        currentJob.getAndSet(null)?.cancel()

        // Use Handler to close to ensure we don't block Main
        backgroundHandler?.post {
            virtualDisplay?.release()
            virtualDisplay = null
            imageReader?.close()
            imageReader = null
            backgroundThread?.quitSafely()
        }
    }
}
