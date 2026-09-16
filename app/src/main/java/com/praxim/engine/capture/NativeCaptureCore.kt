package com.praxim.engine.capture

import android.hardware.HardwareBuffer

class NativeCaptureCore {
    companion object {
        init {
            System.loadLibrary("praxim_capture")
        }
    }

    private external fun nativeValidateAndAuditBuffer(
        hardwareBuffer: HardwareBuffer,
        probePixels: Boolean,
        outMetadata: IntArray
    ): Long

    external fun nativeResetCircuitBreaker()

    fun auditBuffer(hardwareBuffer: HardwareBuffer, probePixels: Boolean): AuditResult {
        val outMetadata = IntArray(4)
        val packedResult = nativeValidateAndAuditBuffer(hardwareBuffer, probePixels, outMetadata)

        val statusCode = (packedResult shr 32).toInt()
        val latencyUs = (packedResult and 0xFFFFFFFFL).toLong()

        return AuditResult(
            status = BufferSecurityStatus.fromCode(statusCode),
            latencyUs = latencyUs,
            width = outMetadata[0],
            height = outMetadata[1],
            stride = outMetadata[2],
            format = outMetadata[3]
        )
    }

    data class AuditResult(
        val status: BufferSecurityStatus,
        val latencyUs: Long,
        val width: Int,
        val height: Int,
        val stride: Int,
        val format: Int
    )
}
