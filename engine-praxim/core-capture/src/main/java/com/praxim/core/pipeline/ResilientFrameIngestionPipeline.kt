package com.praxim.core.pipeline

import android.graphics.ImageFormat
import android.media.Image
import android.media.ImageReader
import android.os.SystemClock
import com.praxim.core.capture.NativeSecureDetector

class ResilientFrameIngestionPipeline(
    private val overlayUiController: OverlayUiController,
    private val nativeDetector: NativeSecureDetector = NativeSecureDetector()
) : ImageReader.OnImageAvailableListener {

    private var currentState: FrameProcessingState = FrameProcessingState.ActiveStreaming
    private var blankFrameCount = 0

    override fun onImageAvailable(reader: ImageReader) {
        val image: Image? = try {
            reader.acquireLatestImage()
        } catch (e: Exception) {
            null
        }

        if (image == null) return

        try {
            val now = SystemClock.uptimeMillis()

            if (currentState is FrameProcessingState.OrientationReconfiguring) {
                val resumeTime = (currentState as FrameProcessingState.OrientationReconfiguring).resumeTimeMs
                if (now < resumeTime) {
                    return // Drop frame during reconfiguration
                } else {
                    transitionTo(FrameProcessingState.ActiveStreaming)
                }
            }

            val isBlank = isImageBlank(image)

            if (isBlank) {
                blankFrameCount++
                if (blankFrameCount >= 3) {
                    if (currentState !is FrameProcessingState.SecureContentLatched) {
                        transitionTo(FrameProcessingState.SecureContentLatched)
                    }
                } else {
                    transitionTo(FrameProcessingState.TransientDebouncing(blankFrameCount))
                }
            } else {
                blankFrameCount = 0
                transitionTo(FrameProcessingState.ActiveStreaming)
                // Here we would typically forward the frame to ML/OCR inference
            }
        } finally {
            image.close()
        }
    }

    fun notifyConfigurationChanged() {
        transitionTo(FrameProcessingState.OrientationReconfiguring(SystemClock.uptimeMillis() + 350))
    }

    private fun transitionTo(newState: FrameProcessingState) {
        if (currentState == newState) return
        currentState = newState

        when (newState) {
            is FrameProcessingState.ActiveStreaming -> {
                overlayUiController.renderActiveContent()
            }
            is FrameProcessingState.SecureContentLatched -> {
                overlayUiController.showSecureScreenFallback()
                overlayUiController.clearBoundingBoxes()
            }
            is FrameProcessingState.TransientDebouncing -> {
                // Pause inference dispatch, retain existing overlay UI
            }
            is FrameProcessingState.OrientationReconfiguring -> {
                // Freeze state transitions
            }
        }
    }

    private fun isImageBlank(image: Image): Boolean {
        return when (image.format) {
            ImageFormat.YUV_420_888 -> {
                val yPlane = image.planes[0]
                nativeDetector.nativeIsBlankFrameYuv(
                    yPlane.buffer,
                    image.width,
                    image.height,
                    yPlane.rowStride,
                    yPlane.pixelStride
                )
            }
            ImageFormat.JPEG -> {
                // Handle JPEG or other formats if necessary, assuming RGBA/PixelX for now
                false
            }
            else -> {
                // Assume RGBA or similar pixel format mapping to our direct RGBA method
                val plane = image.planes[0]
                nativeDetector.nativeIsBlankFrameRgba(
                    plane.buffer,
                    image.width,
                    image.height,
                    plane.rowStride
                )
            }
        }
    }

    // Visible for testing
    fun getCurrentState(): FrameProcessingState = currentState
}
