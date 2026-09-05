package com.praxim.core.pipeline

import android.graphics.ImageFormat
import android.media.Image
import android.media.ImageReader
import com.praxim.core.capture.NativeSecureDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.Mockito.any
import org.mockito.Mockito.doThrow
import java.nio.ByteBuffer

class ResilientFrameIngestionPipelineTest {

    private lateinit pipeline: ResilientFrameIngestionPipeline
    private lateinit mockUiController: OverlayUiController
    private lateinit mockNativeDetector: NativeSecureDetector
    private lateinit mockImageReader: ImageReader

    @Before
    fun setup() {
        mockUiController = mock(OverlayUiController::class.java)
        mockNativeDetector = mock(NativeSecureDetector::class.java)
        mockImageReader = mock(ImageReader::class.java)

        pipeline = ResilientFrameIngestionPipeline(mockUiController, mockNativeDetector)
    }

    private fun createMockImage(isBlank: Boolean): Image {
        val mockImage = mock(Image::class.java)
        val mockPlane = mock(Image.Plane::class.java)
        val buffer = ByteBuffer.allocateDirect(100)

        `when`(mockImage.format).thenReturn(ImageFormat.JPEG) // Using JPEG to bypass direct buffer requirements in tests
        `when`(mockImage.planes).thenReturn(arrayOf(mockPlane))
        `when`(mockPlane.buffer).thenReturn(buffer)

        // Setup NativeSecureDetector mock to return our desired result when called with this mock image's params
        // For testing we assume JPEG falls back to returning false normally, but we will mock RGBA mapping
        `when`(mockImage.format).thenReturn(ImageFormat.UNKNOWN)
        `when`(mockNativeDetector.nativeIsBlankFrameRgba(any(), any(), any(), any())).thenReturn(isBlank)

        return mockImage
    }

    @Test
    fun testDebounceStateMachine() {
        val blankImage1 = createMockImage(true)
        val blankImage2 = createMockImage(true)
        val blankImage3 = createMockImage(true)
        val validImage = createMockImage(false)

        // 1st blank frame
        `when`(mockImageReader.acquireLatestImage()).thenReturn(blankImage1)
        pipeline.onImageAvailable(mockImageReader)
        assertTrue(pipeline.getCurrentState() is FrameProcessingState.TransientDebouncing)

        // 2nd blank frame
        `when`(mockImageReader.acquireLatestImage()).thenReturn(blankImage2)
        pipeline.onImageAvailable(mockImageReader)
        assertTrue(pipeline.getCurrentState() is FrameProcessingState.TransientDebouncing)

        // 3rd blank frame
        `when`(mockImageReader.acquireLatestImage()).thenReturn(blankImage3)
        pipeline.onImageAvailable(mockImageReader)
        assertTrue(pipeline.getCurrentState() is FrameProcessingState.SecureContentLatched)
        verify(mockUiController).showSecureScreenFallback()

        // 1 valid frame
        `when`(mockImageReader.acquireLatestImage()).thenReturn(validImage)
        pipeline.onImageAvailable(mockImageReader)
        assertTrue(pipeline.getCurrentState() is FrameProcessingState.ActiveStreaming)
    }

    @Test
    fun testBufferQueueStarvationRecovery() {
        val mockImage = mock(Image::class.java)
        `when`(mockImageReader.acquireLatestImage()).thenReturn(mockImage)

        // Make the format unsupported so it tries to process but might fail
        `when`(mockImage.format).thenReturn(ImageFormat.UNKNOWN)

        // Force an exception during processing
        `when`(mockImage.planes).thenThrow(RuntimeException("Simulated downstream exception"))

        try {
            pipeline.onImageAvailable(mockImageReader)
        } catch (e: Exception) {
            // Expected
        }

        // Verify that even with the exception, image.close() was called
        verify(mockImage).close()
    }
}
