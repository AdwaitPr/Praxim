package com.praxim.core.pipeline

sealed class FrameProcessingState {
    object ActiveStreaming : FrameProcessingState()
    data class TransientDebouncing(val blankCount: Int) : FrameProcessingState()
    object SecureContentLatched : FrameProcessingState()
    data class OrientationReconfiguring(val resumeTimeMs: Long) : FrameProcessingState()
}
