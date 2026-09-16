package com.praxim.engine.capture

import android.hardware.HardwareBuffer
import android.media.Image
import android.media.ImageReader
import android.util.Log

sealed interface EngineState {
    data class FramePermitted(val hardwareBuffer: HardwareBuffer) : EngineState
    data class FrameRejected(val reason: BufferSecurityStatus) : EngineState
}

class NativeCaptureEngine {
    private val nativeCore = NativeCaptureCore()

    fun processImage(imageReader: ImageReader): EngineState {
        var image: Image? = null
        var hardwareBuffer: HardwareBuffer? = null

        try {
            image = imageReader.acquireLatestImage()
            if (image == null) {
                return EngineState.FrameRejected(BufferSecurityStatus.UnknownFailure)
            }

            hardwareBuffer = image.hardwareBuffer
            if (hardwareBuffer == null) {
                image.close()
                return EngineState.FrameRejected(BufferSecurityStatus.InvalidHardwareBufferRef)
            }

            val auditResult = nativeCore.auditBuffer(hardwareBuffer, probePixels = true)

            if (auditResult.status == BufferSecurityStatus.Permitted) {
                // Return buffer, caller must close both image and hardwareBuffer
                return EngineState.FramePermitted(hardwareBuffer)
            } else {
                Log.w("NativeCaptureEngine", "Buffer rejected: ${auditResult.status}")
                hardwareBuffer.close()
                image.close()
                return EngineState.FrameRejected(auditResult.status)
            }
        } catch (e: Exception) {
            Log.e("NativeCaptureEngine", "Exception during image processing", e)
            hardwareBuffer?.close()
            image?.close()
            return EngineState.FrameRejected(BufferSecurityStatus.UnknownFailure)
        }
    }

    fun resetCircuitBreaker() {
        nativeCore.nativeResetCircuitBreaker()
    }
}
