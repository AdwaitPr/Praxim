package com.praxim.engine.analysis

import com.google.mlkit.vision.text.Text
import java.util.regex.Pattern
import kotlin.math.abs

data class ExtractedEntity(
    val type: String,
    val value: String,
    val boundingBox: android.graphics.Rect?
)

class SpatialEntityExtractor {
    companion object {
        private val VPA_PATTERN = Pattern.compile("[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64}")
        private val IFSC_PATTERN = Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$")
        private val PHONE_PATTERN = Pattern.compile("\\+?\\d{10,13}")
        private val AMOUNT_PATTERN = Pattern.compile("(?:Rs\\.?|INR|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)")

        fun extractEntities(mlkitText: Text): List<ExtractedEntity> {
            val entities = mutableListOf<ExtractedEntity>()
            val lines = clusterLines(mlkitText)

            for (line in lines) {
                val text = line.text

                // Extract VPAs
                val vpaMatcher = VPA_PATTERN.matcher(text)
                while (vpaMatcher.find()) {
                    entities.add(ExtractedEntity("VPA", vpaMatcher.group(), line.boundingBox))
                }

                // Extract IFSC
                val ifscMatcher = IFSC_PATTERN.matcher(text)
                while (ifscMatcher.find()) {
                    entities.add(ExtractedEntity("IFSC", ifscMatcher.group(), line.boundingBox))
                }

                // Extract Phone
                val phoneMatcher = PHONE_PATTERN.matcher(text)
                while (phoneMatcher.find()) {
                    entities.add(ExtractedEntity("PHONE", phoneMatcher.group(), line.boundingBox))
                }

                // Extract Amount
                val amountMatcher = AMOUNT_PATTERN.matcher(text)
                while (amountMatcher.find()) {
                    val amountStr = amountMatcher.group(1)
                    if (amountStr != null) {
                         entities.add(ExtractedEntity("AMOUNT", amountStr, line.boundingBox))
                    }
                }
            }

            return entities
        }

        private fun clusterLines(mlkitText: Text): List<Text.Line> {
            val allLines = mutableListOf<Text.Line>()
            for (block in mlkitText.textBlocks) {
                allLines.addAll(block.lines)
            }

            // Basic sorting by Y, then X
            allLines.sortBy { it.boundingBox?.top ?: 0 }

            val clusteredLines = mutableListOf<Text.Line>()

            // Simple clustering based on vertical delta
            var currentCluster = mutableListOf<Text.Line>()

            for (line in allLines) {
                if (currentCluster.isEmpty()) {
                    currentCluster.add(line)
                    continue
                }

                val lastLine = currentCluster.last()
                val lastBox = lastLine.boundingBox
                val currentBox = line.boundingBox

                if (lastBox != null && currentBox != null) {
                    val y1Mid = lastBox.top + (lastBox.height() / 2.0f)
                    val y2Mid = currentBox.top + (currentBox.height() / 2.0f)
                    val hAvg = (lastBox.height() + currentBox.height()) / 2.0f

                    if (abs(y1Mid - y2Mid) <= 0.5f * hAvg) {
                        currentCluster.add(line)
                    } else {
                        // Start new cluster (in a real scenario we'd merge text here, but keeping it simple for now)
                        clusteredLines.addAll(currentCluster)
                        currentCluster = mutableListOf(line)
                    }
                } else {
                     currentCluster.add(line)
                }
            }

            if (currentCluster.isNotEmpty()) {
                clusteredLines.addAll(currentCluster)
            }

            return clusteredLines
        }
    }
}
