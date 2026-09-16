package com.praxim.engine.capture

import android.hardware.HardwareBuffer

class NativeCaptureCore {
    companion object {
        init {
            System.loadLibrary("praxim_security_engine")
        }
    }

    external fun validateBufferSecurity(hardwareBuffer: HardwareBuffer): Int
}
