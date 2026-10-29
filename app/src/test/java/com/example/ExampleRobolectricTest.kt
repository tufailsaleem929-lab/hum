package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("NoteFlow", appName)
  }

  @Test
  fun `billingManager default state and test mode isolation`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val billingManager = com.example.billing.BillingManager(context)

    // Clear any leftover prefs from other runs
    billingManager.isTestModeEnabled = false
    billingManager.clearTestEntitlements()

    // 1. Initial state should NOT be pro
    org.junit.Assert.assertFalse("User should not be Pro by default", billingManager.isProUser.value)

    // 2. Simulated purchase while test mode is disabled MUST NOT grant entitlements (Requirement 7)
    billingManager.simulateTestPurchase(com.example.billing.BillingManager.PRODUCT_PRO_ANNUAL)
    org.junit.Assert.assertFalse("Simulated purchase must fail when test mode is disabled", billingManager.isProUser.value)

    // 3. Enable test mode explicitly
    billingManager.isTestModeEnabled = true
    billingManager.simulateTestPurchase(com.example.billing.BillingManager.PRODUCT_PRO_ANNUAL)
    org.junit.Assert.assertTrue("Simulated purchase should grant Pro in explicit test mode", billingManager.isProUser.value)

    // 4. Test clearing entitlements
    billingManager.clearTestEntitlements()
    org.junit.Assert.assertFalse("Clearing entitlements should revoke Pro", billingManager.isProUser.value)

    // 5. Verify price fallbacks when offline
    assertEquals("$34.99", billingManager.getFormattedPrice(com.example.billing.BillingManager.PRODUCT_PRO_ANNUAL))
    assertEquals("$4.99", billingManager.getFormattedPrice(com.example.billing.BillingManager.PRODUCT_PRO_MONTHLY))
    assertEquals("$79.99", billingManager.getFormattedPrice(com.example.billing.BillingManager.PRODUCT_LIFETIME_VIP))
  }
}
