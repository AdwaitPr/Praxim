package com.example.praxim.engine

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import com.example.praxim.model.EntityType
import com.example.praxim.model.NormalizedRect
import com.example.praxim.model.RecognizedEntity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import java.util.regex.Pattern
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object EntityRecognizerEngine {

    // Regex Patterns for Indian digital entity ecosystem
    private val UPI_PATTERN = Pattern.compile(
        "\\b[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64}\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val IFSC_PATTERN = Pattern.compile(
        "\\b[A-Z]{4}0[A-Z0-9]{6}\\b"
    )

    private val PHONE_PATTERN = Pattern.compile(
        "\\b(?:\\+91[\\-\\s]?)?[6-9]\\d{9}\\b"
    )

    private val URL_PATTERN = Pattern.compile(
        "\\bhttps?://[^\\s]+\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val TRACKING_PATTERN = Pattern.compile(
        "\\b(?:AWB|IN|EK|DEL|DTDC|TRACK)?[0-9]{10,14}\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    suspend fun processFrame(bitmap: Bitmap): List<RecognizedEntity> = suspendCancellableCoroutine { continuation ->
        val image = InputImage.fromBitmap(bitmap, 0)
        val imageWidth = bitmap.width.toFloat()
        val imageHeight = bitmap.height.toFloat()

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val entities = mutableListOf<RecognizedEntity>()
                val seenValues = mutableSetOf<String>()

                for (block in visionText.textBlocks) {
                    val rawText = block.text
                    val bbox = block.boundingBox
                    val normalizedRect = if (bbox != null && imageWidth > 0 && imageHeight > 0) {
                        NormalizedRect(
                            left = bbox.left / imageWidth,
                            top = bbox.top / imageHeight,
                            right = bbox.right / imageWidth,
                            bottom = bbox.bottom / imageHeight
                        )
                    } else {
                        NormalizedRect(0f, 0f, 0f, 0f)
                    }

                    // UPI
                    val upiMatcher = UPI_PATTERN.matcher(rawText)
                    while (upiMatcher.find()) {
                        val upi = upiMatcher.group().trim().lowercase()
                        if (upi !in seenValues) {
                            seenValues.add(upi)
                            entities.add(
                                RecognizedEntity(
                                    id = UUID.randomUUID().toString(),
                                    rawText = upi,
                                    formattedValue = upi,
                                    type = EntityType.UPI_ID,
                                    primaryActionLabel = "Pay via UPI",
                                    secondaryActionLabel = "Copy UPI ID",
                                    boundingBox = normalizedRect
                                )
                            )
                        }
                    }

                    // IFSC
                    val ifscMatcher = IFSC_PATTERN.matcher(rawText)
                    while (ifscMatcher.find()) {
                        val ifsc = ifscMatcher.group().trim().uppercase()
                        if (ifsc !in seenValues) {
                            seenValues.add(ifsc)
                            val bankName = getBankNameFromIfsc(ifsc)
                            entities.add(
                                RecognizedEntity(
                                    id = UUID.randomUUID().toString(),
                                    rawText = ifsc,
                                    formattedValue = "$ifsc ($bankName)",
                                    type = EntityType.IFSC_CODE,
                                    primaryActionLabel = "Copy IFSC & Bank Details",
                                    secondaryActionLabel = "Verify Bank & Branch",
                                    boundingBox = normalizedRect
                                )
                            )
                        }
                    }

                    // Phone
                    val phoneMatcher = PHONE_PATTERN.matcher(rawText)
                    while (phoneMatcher.find()) {
                        val phone = phoneMatcher.group().trim().replace(" ", "").replace("-", "")
                        val cleanPhone = if (!phone.startsWith("+91")) "+91$phone" else phone
                        if (cleanPhone !in seenValues) {
                            seenValues.add(cleanPhone)
                            entities.add(
                                RecognizedEntity(
                                    id = UUID.randomUUID().toString(),
                                    rawText = phoneMatcher.group().trim(),
                                    formattedValue = cleanPhone,
                                    type = EntityType.PHONE_NUMBER,
                                    primaryActionLabel = "Call / WhatsApp",
                                    secondaryActionLabel = "Copy Number",
                                    boundingBox = normalizedRect
                                )
                            )
                        }
                    }

                    // URL
                    val urlMatcher = URL_PATTERN.matcher(rawText)
                    while (urlMatcher.find()) {
                        val url = urlMatcher.group().trim()
                        if (url.lowercase() !in seenValues) {
                            seenValues.add(url.lowercase())
                            entities.add(
                                RecognizedEntity(
                                    id = UUID.randomUUID().toString(),
                                    rawText = url,
                                    formattedValue = url,
                                    type = EntityType.URL_LINK,
                                    primaryActionLabel = "Open Link",
                                    secondaryActionLabel = "Copy Link",
                                    boundingBox = normalizedRect
                                )
                            )
                        }
                    }

                    // Add generic fallback if no match
                    if (entities.isEmpty() && rawText.isNotBlank()) {
                         val snippet = rawText.take(80)
                         entities.add(
                             RecognizedEntity(
                                 id = UUID.randomUUID().toString(),
                                 rawText = rawText,
                                 formattedValue = "\"$snippet\"",
                                 type = EntityType.GENERIC_TEXT,
                                 primaryActionLabel = "Copy Full Screen Text",
                                 secondaryActionLabel = "Share Snippet",
                                 boundingBox = normalizedRect
                             )
                         )
                    }
                }

                // Fallback for completely empty parse but text exists
                if (entities.isEmpty() && visionText.text.isNotBlank()) {
                    val rawText = visionText.text
                    val snippet = rawText.take(80)
                    entities.add(
                        RecognizedEntity(
                            id = UUID.randomUUID().toString(),
                            rawText = rawText,
                            formattedValue = "\"$snippet\"",
                            type = EntityType.GENERIC_TEXT,
                            primaryActionLabel = "Copy Full Screen Text",
                            secondaryActionLabel = "Share Snippet",
                            boundingBox = NormalizedRect(0.05f, 0.30f, 0.95f, 0.60f)
                        )
                    )
                }

                if (continuation.isActive) {
                    continuation.resume(entities)
                }
            }
            .addOnFailureListener { e ->
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }
    }

    // Keep existing parseTextEntities for any legacy callers that just pass strings
    fun parseTextEntities(rawScreenText: String): List<RecognizedEntity> {
        val results = mutableListOf<RecognizedEntity>()
        val seenValues = mutableSetOf<String>()

        // 1. UPI IDs
        val upiMatcher = UPI_PATTERN.matcher(rawScreenText)
        while (upiMatcher.find()) {
            val upi = upiMatcher.group().trim().lowercase()
            if (upi !in seenValues) {
                seenValues.add(upi)
                results.add(
                    RecognizedEntity(
                        id = UUID.randomUUID().toString(),
                        rawText = upi,
                        formattedValue = upi,
                        type = EntityType.UPI_ID,
                        primaryActionLabel = "Pay via UPI",
                        secondaryActionLabel = "Copy UPI ID",
                        boundingBox = NormalizedRect(0.1f, 0.2f, 0.9f, 0.28f)
                    )
                )
            }
        }

        // 2. Bank IFSC
        val ifscMatcher = IFSC_PATTERN.matcher(rawScreenText)
        while (ifscMatcher.find()) {
            val ifsc = ifscMatcher.group().trim().uppercase()
            if (ifsc !in seenValues) {
                seenValues.add(ifsc)
                val bankName = getBankNameFromIfsc(ifsc)
                results.add(
                    RecognizedEntity(
                        id = UUID.randomUUID().toString(),
                        rawText = ifsc,
                        formattedValue = "$ifsc ($bankName)",
                        type = EntityType.IFSC_CODE,
                        primaryActionLabel = "Copy IFSC & Bank Details",
                        secondaryActionLabel = "Verify Bank & Branch",
                        boundingBox = NormalizedRect(0.15f, 0.38f, 0.85f, 0.44f)
                    )
                )
            }
        }

        // 3. Phone Numbers
        val phoneMatcher = PHONE_PATTERN.matcher(rawScreenText)
        while (phoneMatcher.find()) {
            val phone = phoneMatcher.group().trim().replace(" ", "").replace("-", "")
            val cleanPhone = if (!phone.startsWith("+91")) "+91$phone" else phone
            if (cleanPhone !in seenValues) {
                seenValues.add(cleanPhone)
                results.add(
                    RecognizedEntity(
                        id = UUID.randomUUID().toString(),
                        rawText = phoneMatcher.group().trim(),
                        formattedValue = cleanPhone,
                        type = EntityType.PHONE_NUMBER,
                        primaryActionLabel = "Call / WhatsApp",
                        secondaryActionLabel = "Copy Number",
                        boundingBox = NormalizedRect(0.12f, 0.50f, 0.88f, 0.56f)
                    )
                )
            }
        }

        // 4. Web URLs
        val urlMatcher = URL_PATTERN.matcher(rawScreenText)
        while (urlMatcher.find()) {
            val url = urlMatcher.group().trim()
            if (url.lowercase() !in seenValues) {
                seenValues.add(url.lowercase())
                results.add(
                    RecognizedEntity(
                        id = UUID.randomUUID().toString(),
                        rawText = url,
                        formattedValue = url,
                        type = EntityType.URL_LINK,
                        primaryActionLabel = "Open Link",
                        secondaryActionLabel = "Copy Link",
                        boundingBox = NormalizedRect(0.08f, 0.62f, 0.92f, 0.68f)
                    )
                )
            }
        }

        // 5. Courier / Tracking IDs
        val trackMatcher = TRACKING_PATTERN.matcher(rawScreenText)
        while (trackMatcher.find()) {
            val trk = trackMatcher.group().trim()
            if (trk.length >= 10 && trk !in seenValues) {
                seenValues.add(trk)
                results.add(
                    RecognizedEntity(
                        id = UUID.randomUUID().toString(),
                        rawText = trk,
                        formattedValue = "Ref: $trk",
                        type = EntityType.TRACKING_ID,
                        primaryActionLabel = "Track Order / Delivery",
                        secondaryActionLabel = "Copy Reference",
                        boundingBox = NormalizedRect(0.14f, 0.72f, 0.86f, 0.78f)
                    )
                )
            }
        }

        // Fallback generic text snippet if nothing structured was parsed
        if (results.isEmpty() && rawScreenText.isNotBlank()) {
            val snippet = rawScreenText.lines().firstOrNull { it.isNotBlank() }?.take(80) ?: rawScreenText.take(80)
            results.add(
                RecognizedEntity(
                    id = UUID.randomUUID().toString(),
                    rawText = rawScreenText,
                    formattedValue = "\"$snippet\"",
                    type = EntityType.GENERIC_TEXT,
                    primaryActionLabel = "Copy Full Screen Text",
                    secondaryActionLabel = "Share Snippet",
                    boundingBox = NormalizedRect(0.05f, 0.30f, 0.95f, 0.60f)
                )
            )
        }

        return results
    }

    private fun getBankNameFromIfsc(ifsc: String): String {
        return when {
            ifsc.startsWith("SBIN") -> "State Bank of India"
            ifsc.startsWith("HDFC") -> "HDFC Bank"
            ifsc.startsWith("ICIC") -> "ICICI Bank"
            ifsc.startsWith("UTIB") -> "Axis Bank"
            ifsc.startsWith("PUNB") -> "Punjab National Bank"
            ifsc.startsWith("BARB") -> "Bank of Baroda"
            ifsc.startsWith("CNRB") -> "Canara Bank"
            ifsc.startsWith("KKBK") -> "Kotak Mahindra Bank"
            ifsc.startsWith("IDIB") -> "Indian Bank"
            ifsc.startsWith("MAHB") -> "Bank of Maharashtra"
            else -> "Indian Scheduled Bank"
        }
    }

    /**
     * Executes the single-tap native system action for a recognized entity.
     */
    fun executeEntityAction(context: Context, entity: RecognizedEntity, isSecondaryAction: Boolean = false) {
        when (entity.type) {
            EntityType.UPI_ID -> {
                if (isSecondaryAction) {
                    copyToClipboard(context, "UPI ID", entity.rawText)
                } else {
                    // Launch UPI Intent
                    val uri = Uri.parse("upi://pay?pa=${Uri.encode(entity.rawText)}&pn=${Uri.encode("Recipient")}&cu=INR")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    val chooser = Intent.createChooser(intent, "Pay using UPI")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(chooser)
                    } catch (e: Exception) {
                        copyToClipboard(context, "UPI ID", entity.rawText)
                        Toast.makeText(context, "Copied UPI ID: ${entity.rawText}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            EntityType.PHONE_NUMBER -> {
                if (isSecondaryAction) {
                    copyToClipboard(context, "Phone", entity.rawText)
                } else {
                    // Call / WhatsApp
                    val cleanNum = entity.rawText.replace(" ", "").replace("+", "").replace("-", "")
                    val uri = Uri.parse("https://wa.me/91${cleanNum.removePrefix("91")}")
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        copyToClipboard(context, "Phone", entity.rawText)
                    }
                }
            }
            EntityType.IFSC_CODE -> {
                if (isSecondaryAction) {
                    copyToClipboard(context, "IFSC Code", entity.rawText)
                } else {
                    copyToClipboard(context, "IFSC & Bank Details", entity.rawText)
                }
            }
            EntityType.URL_LINK -> {
                if (isSecondaryAction) {
                    copyToClipboard(context, "URL", entity.rawText)
                } else {
                    val fullUrl = if (!entity.rawText.startsWith("http")) "https://${entity.rawText}" else entity.rawText
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        copyToClipboard(context, "Link", entity.rawText)
                    }
                }
            }
            EntityType.TRACKING_ID -> {
                if (isSecondaryAction) {
                    copyToClipboard(context, "Tracking ID", entity.rawText)
                } else {
                    val uri = Uri.parse("https://www.google.com/search?q=track+package+${entity.rawText}")
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        copyToClipboard(context, "Tracking ID", entity.rawText)
                    }
                }
            }
            else -> {
                copyToClipboard(context, "Extracted Text", entity.rawText)
            }
        }
    }

    private fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "Copied $label to clipboard", Toast.LENGTH_SHORT).show()
    }
}
