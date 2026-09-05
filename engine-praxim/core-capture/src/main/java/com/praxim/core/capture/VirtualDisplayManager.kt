package com.praxim.core.capture

import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.view.Surface

class VirtualDisplayManager(private val mediaProjection: MediaProjection) {
    private var virtualDisplay: VirtualDisplay? = null

    fun createVirtualDisplay(width: Int, height: Int, densityDpi: Int, surface: Surface): VirtualDisplay {
        release()

        virtualDisplay = mediaProjection.createVirtualDisplay(
            "PraximScreenCapture",
            width,
            height,
            densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            surface,
            null,
            null
        )
        return virtualDisplay!!
    }

    fun resize(width: Int, height: Int, densityDpi: Int) {
        virtualDisplay?.resize(width, height, densityDpi)
    }

    fun release() {
        virtualDisplay?.release()
        virtualDisplay = null
    }
}
