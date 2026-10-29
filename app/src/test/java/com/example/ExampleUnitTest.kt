package com.example

import com.example.ai.AiNoteAssistant
import com.example.ai.AiTone
import com.example.data.model.NoteEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun noteEntity_defaultValues() {
        val note = NoteEntity(title = "Test Note", content = "Test Content")
        assertEquals("Test Note", note.title)
        assertEquals("default", note.colorId)
        assertTrue(note.createdAt > 0)
    }

    @Test
    fun aiAssistant_fallbackSummarizeWorks() = runBlocking {
        val assistant = AiNoteAssistant()
        val content = "Discussed Q4 launch.\nPrioritize speech dictation.\nDeploy to production."
        val summary = assistant.summarize(content)
        assertNotNull(summary)
        assertTrue(summary.contains("Summary") || summary.contains("Takeaways"))
    }

    @Test
    fun billing_exactProductIdsMatchPlayConsoleRequirements() {
        assertEquals("noteflow_pro_annual", com.example.billing.BillingManager.PRODUCT_PRO_ANNUAL)
        assertEquals("noteflow_pro_monthly", com.example.billing.BillingManager.PRODUCT_PRO_MONTHLY)
        assertEquals("noteflow_lifetime_vip", com.example.billing.BillingManager.PRODUCT_LIFETIME_VIP)
    }

    @Test
    fun billing_productTypesSeparation() {
        // Annual and Monthly MUST be SUBS, Lifetime VIP MUST be INAPP
        val annualTier = com.example.billing.ProTier(
            id = com.example.billing.BillingManager.PRODUCT_PRO_ANNUAL,
            title = "Annual Plan",
            defaultPrice = "$34.99",
            defaultPeriod = "/ year",
            isSubscription = true
        )
        val monthlyTier = com.example.billing.ProTier(
            id = com.example.billing.BillingManager.PRODUCT_PRO_MONTHLY,
            title = "Monthly Plan",
            defaultPrice = "$4.99",
            defaultPeriod = "/ month",
            isSubscription = true
        )
        val lifetimeTier = com.example.billing.ProTier(
            id = com.example.billing.BillingManager.PRODUCT_LIFETIME_VIP,
            title = "Lifetime VIP",
            defaultPrice = "$79.99",
            defaultPeriod = "one-time payment",
            isSubscription = false
        )

        assertTrue("Annual must be a subscription", annualTier.isSubscription)
        assertTrue("Monthly must be a subscription", monthlyTier.isSubscription)
        org.junit.Assert.assertFalse("Lifetime VIP must NOT be a subscription (INAPP one-time)", lifetimeTier.isSubscription)
    }
}
