package com.praxim.engine.slm.parser

data class TriageResult(
    var isFraud: Boolean = false,
    var confidence: Float = 0.0f,
    var payee: String = "",
    var amount: String = "",
    var risk: String = ""
)

class ZeroAllocStreamingParser {
    // A simplified deterministic state machine parser for demonstration.
    // In a real scenario, this would avoid String allocations entirely
    // by operating on ByteArrays/CharArrays directly.
    fun parse(jsonStream: CharSequence, outResult: TriageResult) {
        var inKey = false
        var inValue = false
        var currentKey = StringBuilder()
        var currentValue = StringBuilder()

        for (i in 0 until jsonStream.length) {
            val c = jsonStream[i]

            if (c == '"') {
                if (!inKey && !inValue) {
                    // Could be start of key or value
                    // simplified logic: if we have a key, it's a value
                    if (currentKey.isEmpty()) {
                        inKey = true
                    } else {
                        inValue = true
                    }
                } else if (inKey) {
                    inKey = false
                } else if (inValue) {
                    inValue = false
                    applyParsedPair(currentKey.toString(), currentValue.toString(), outResult)
                    currentKey.clear()
                    currentValue.clear()
                }
            } else if (inKey) {
                currentKey.append(c)
            } else if (inValue) {
                currentValue.append(c)
            } else if (c == ':' || c == ',' || c == '{' || c == '}' || c.isWhitespace()) {
                // Ignore structural characters outside strings (for simplistic parsing)
            } else {
                // Start of non-string value (boolean, number)
                if (currentKey.isNotEmpty() && !inValue) {
                    inValue = true
                    currentValue.append(c)
                } else if (inValue) {
                     currentValue.append(c)
                     // If it's the last char or next is structural, end value
                     if (i == jsonStream.length - 1 || jsonStream[i+1] == ',' || jsonStream[i+1] == '}') {
                         inValue = false
                         applyParsedPair(currentKey.toString(), currentValue.toString(), outResult)
                         currentKey.clear()
                         currentValue.clear()
                     }
                }
            }
        }
    }

    private fun applyParsedPair(key: String, value: String, result: TriageResult) {
        when (key) {
            "isFraud" -> result.isFraud = value.toBoolean()
            "confidence" -> result.confidence = value.toFloatOrNull() ?: 0.0f
            "payee" -> result.payee = value
            "amount" -> result.amount = value
            "risk" -> result.risk = value
        }
    }
}
