package com.example.praxim.engine

import com.example.praxim.model.EntityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EntityRecognizerEngineTest {

    @Test
    fun `test UPI VPA regex recognition`() {
        val text = "Please send money to test@okaxis or merchant.pay@upi for the order."
        val entities = EntityRecognizerEngine.parseTextEntities(text)

        val upiEntities = entities.filter { it.type == EntityType.UPI_ID }
        assertEquals(2, upiEntities.size)
        assertTrue(upiEntities.any { it.rawText == "test@okaxis" })
        assertTrue(upiEntities.any { it.rawText == "merchant.pay@upi" })
    }

    @Test
    fun `test IFSC code format verification`() {
        val text = "Bank details: HDFC0001234 and SBIN0000456"
        val entities = EntityRecognizerEngine.parseTextEntities(text)

        val ifscEntities = entities.filter { it.type == EntityType.IFSC_CODE }
        assertEquals(2, ifscEntities.size)
        assertTrue(ifscEntities.any { it.rawText == "HDFC0001234" })
        assertTrue(ifscEntities.any { it.rawText == "SBIN0000456" })
    }

    @Test
    fun `test Phone number extraction`() {
        val text = "Call me at +919876543210 or 9876543210."
        val entities = EntityRecognizerEngine.parseTextEntities(text)

        val phoneEntities = entities.filter { it.type == EntityType.PHONE_NUMBER }
        // The engine deduplicates phones by formatting, so it might only find one unique
        assertTrue(phoneEntities.isNotEmpty())
        assertTrue(phoneEntities.any { it.rawText == "+919876543210" || it.rawText == "9876543210" || it.formattedValue == "+919876543210" })
    }

    @Test
    fun `test Web URL extraction`() {
        val text = "Visit https://example.com for more info."
        val entities = EntityRecognizerEngine.parseTextEntities(text)

        val urlEntities = entities.filter { it.type == EntityType.URL_LINK }
        assertEquals(1, urlEntities.size)
        assertEquals("https://example.com", urlEntities[0].rawText)
    }
}
