package com.praxim.engine.capture

sealed interface BufferSecurityStatus {
    object Permitted : BufferSecurityStatus
    object ProtectedSurfaceViolation : BufferSecurityStatus
    object MissingCpuReadPermission : BufferSecurityStatus
    object UnsupportedPixelFormat : BufferSecurityStatus
    object InvalidGeometryOrStride : BufferSecurityStatus
    object NativeLockFailed : BufferSecurityStatus
    object ExecutionTimeout : BufferSecurityStatus
    object CircuitBreakerTripped : BufferSecurityStatus
    object PosixSignalTrapped : BufferSecurityStatus
    object InvalidHardwareBufferRef : BufferSecurityStatus
    object UnknownFailure : BufferSecurityStatus

    companion object {
        fun fromCode(code: Int): BufferSecurityStatus {
            return when (code) {
                0 -> Permitted
                1 -> ProtectedSurfaceViolation
                2 -> MissingCpuReadPermission
                3 -> UnsupportedPixelFormat
                4 -> InvalidGeometryOrStride
                5 -> NativeLockFailed
                6 -> ExecutionTimeout
                7 -> CircuitBreakerTripped
                8 -> PosixSignalTrapped
                9 -> InvalidHardwareBufferRef
                else -> UnknownFailure
            }
        }
    }
}
