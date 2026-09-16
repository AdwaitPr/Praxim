package com.praxim.engine.slm

import android.content.Context
import com.praxim.engine.slm.parser.TriageResult
import com.praxim.engine.slm.parser.ZeroAllocStreamingParser

class ScamTriageClassifier(private val context: Context) {
    private val engine = OnDeviceIntelligenceEngine.getInstance(context)
    private val parser = ZeroAllocStreamingParser()

    suspend fun classifyTransaction(extractedText: String, entities: List<String>): TriageResult {
        // Construct a constrained prompt for greedy generation (temperature=0 implied by model config usually,
        // or set via options when supported).
        val prompt = """
            Analyze the following text and extracted entities for potential fraud.
            Respond ONLY with a JSON object containing the keys: isFraud (boolean), confidence (float), payee (string), amount (string), risk (string).
            Limit output to 64 tokens.

            Text: $extractedText
            Entities: ${entities.joinToString(", ")}
        """.trimIndent()

        val jsonResponse = engine.executeInference(prompt)

        val result = TriageResult()
        parser.parse(jsonResponse, result)

        return result
    }
}
