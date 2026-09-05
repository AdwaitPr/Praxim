package com.praxim.core.capture

import java.nio.ByteBuffer

class NativeSecureDetector {
    companion object {
        init {
            System.loadLibrary("praxim_security_engine")
        }
    }

    external fun nativeIsBlankFrameRgba(
        buffer: ByteBuffer,
        width: Int,
        height: Int,
        rowStrideBytes: Int
    ): Boolean

    external fun nativeIsBlankFrameYuv(
        buffer: ByteBuffer,
        width: Int,
        height: Int,
        rowStrideBytes: Int,
        pixelStrideBytes: Int
    ): Boolean
}
