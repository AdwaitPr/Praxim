package com.praxim.engine.pipeline

import android.hardware.HardwareBuffer
import android.media.Image
import java.util.concurrent.atomic.AtomicBoolean

class ScopedFrame(
    val image: Image,
    val hardwareBuffer: HardwareBuffer,
    val frameIndex: Long,
    val timestampNs: Long
) : AutoCloseable {

    private val isClosed = AtomicBoolean(false)

    override fun close() {
        if (isClosed.compareAndSet(false, true)) {
            try {
                hardwareBuffer.close()
            } catch (e: Exception) {
                // Ignore close errors
            } finally {
                try {
                    image.close()
                } catch (e: Exception) {
                    // Ignore close errors
                }
            }
        }
    }
}
