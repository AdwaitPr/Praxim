package com.example.praxim.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.WindowMetrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

class ScreenCaptureManager(
    private val context: Context,
    private val mediaProjection: MediaProjection
) {
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var screenWidth = 0
    private var screenHeight = 0
    private var screenDensity = 0

    private var nv21Buffer: ByteBuffer? = null
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null

    private var onFrameCallback: ((ByteBuffer, Int, Int, Long) -> Unit)? = null
    private var onErrorCallback: ((Throwable) -> Unit)? = null

    private var isCapturing = false
    private val captureScope = CoroutineScope(Dispatchers.Default)

    init {
        setupDisplayMetrics()
        setupBuffers()
        setupHandlerThread()
        setupImageReader()
        setupVirtualDisplay()
    }

    private fun setupBuffers() {
        // NV21 requires width * height * 1.5 bytes
        val bufferSize = screenWidth * screenHeight * 3 / 2
        nv21Buffer = ByteBuffer.allocateDirect(bufferSize)
    }

    private fun setupHandlerThread() {
        handlerThread = HandlerThread("ImageReaderThread").apply { start() }
        handler = Handler(handlerThread!!.looper)
    }

    private fun setupDisplayMetrics() {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics: WindowMetrics = windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds
            screenWidth = bounds.width()
            screenHeight = bounds.height()
            screenDensity = context.resources.configuration.densityDpi
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            screenWidth = metrics.widthPixels
            screenHeight = metrics.heightPixels
            screenDensity = metrics.densityDpi
        }
    }

    @SuppressLint("WrongConstant")
    private fun setupImageReader() {
        imageReader = ImageReader.newInstance(
            screenWidth,
            screenHeight,
            PixelFormat.RGBA_8888,
            2
        ).apply {
            setOnImageAvailableListener({ reader ->
                val image: Image? = reader.acquireLatestImage()
                if (image == null) return@setOnImageAvailableListener

                if (!isCapturing) {
                    image.close()
                    return@setOnImageAvailableListener
                }

                // We only want a single frame per capture request
                isCapturing = false
                val captureTimestamp = System.currentTimeMillis()

                captureScope.launch {
                    try {
                        val planes = image.planes
                        val buffer = planes[0].buffer
                        val pixelStride = planes[0].pixelStride
                        val rowStride = planes[0].rowStride

                        val targetBuffer = nv21Buffer ?: return@launch
                        targetBuffer.rewind()

                        // Extract luminance (Y) from RGBA to NV21 format directly without intermediate arrays/bitmaps
                        // NV21 layout:
                        // Y (Luminance) block first: size = width * height
                        // VU (Chroma) block next: size = width * height / 2
                        // For OCR, grayscale (Y) is sufficient. ML Kit ignores VU if Y is valid or might need valid VU for some processing depending on strictness.
                        // We will set UV to 128 (neutral chroma).

                        for (y in 0 until screenHeight) {
                            var bufferPos = y * rowStride
                            var targetPos = y * screenWidth

                            for (x in 0 until screenWidth) {
                                // RGBA_8888 layout: R, G, B, A
                                val r = buffer.get(bufferPos).toInt() and 0xFF
                                val g = buffer.get(bufferPos + 1).toInt() and 0xFF
                                val b = buffer.get(bufferPos + 2).toInt() and 0xFF

                                // Standard RGB to Grayscale (Luminance) conversion
                                val yVal = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                                targetBuffer.put(targetPos, yVal.toByte())

                                bufferPos += pixelStride
                                targetPos++
                            }
                        }

                        // Fill UV with neutral values (128)
                        val ySize = screenWidth * screenHeight
                        val uvSize = ySize / 2
                        for (i in 0 until uvSize) {
                            targetBuffer.put(ySize + i, 128.toByte())
                        }

                        targetBuffer.rewind()
                        onFrameCallback?.invoke(targetBuffer, screenWidth, screenHeight, captureTimestamp)

                    } catch (e: Exception) {
                        onErrorCallback?.invoke(e)
                    } finally {
                        image.close()
                    }
                }
            }, handler)
        }
    }

    private fun setupVirtualDisplay() {
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "ScreenCapture",
            screenWidth,
            screenHeight,
            screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )
    }

    fun captureSingleFrame(onFrameReady: (ByteBuffer, Int, Int, Long) -> Unit, onError: (Throwable) -> Unit) {
        this.onFrameCallback = onFrameReady
        this.onErrorCallback = onError
        this.isCapturing = true
    }

    fun destroy() {
        virtualDisplay?.release()
        virtualDisplay = null

        imageReader?.setOnImageAvailableListener(null, null)
        imageReader?.close()
        imageReader = null

        handlerThread?.quitSafely()
        try {
            handlerThread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        handlerThread = null
        handler = null
        nv21Buffer = null

        mediaProjection.stop()
    }
}
