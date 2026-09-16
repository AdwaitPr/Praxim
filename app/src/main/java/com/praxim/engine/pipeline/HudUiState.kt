package com.praxim.engine.pipeline

import com.praxim.engine.analysis.ExtractedEntity
import com.praxim.engine.slm.parser.TriageResult

data class AnalysisOutcome(
    val result: TriageResult,
    val entities: List<ExtractedEntity>
)

sealed interface HudUiState {
    object Idle : HudUiState
    object Analyzing : HudUiState
    object SecureScreenDetected : HudUiState
    data class Throttled(val reason: String) : HudUiState
    data class ActionReady(val outcome: AnalysisOutcome) : HudUiState
    data class ThreatDetected(val outcome: AnalysisOutcome) : HudUiState
}
