package com.example.billing

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProTier(
    val id: String,
    val title: String,
    val defaultPrice: String,
    val defaultPeriod: String,
    val badge: String? = null,
    val isSubscription: Boolean = true
)

class BillingManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : PurchasesUpdatedListener {

    private val TAG = "BillingManager"
    private val prefs: SharedPreferences = context.getSharedPreferences("noteflow_billing_prefs", Context.MODE_PRIVATE)

    companion object {
        const val PRODUCT_PRO_ANNUAL = "noteflow_pro_annual"
        const val PRODUCT_PRO_MONTHLY = "noteflow_pro_monthly"
        const val PRODUCT_LIFETIME_VIP = "noteflow_lifetime_vip"
    }

    val proTiers = listOf(
        ProTier(
            id = PRODUCT_PRO_ANNUAL,
            title = "Annual Plan",
            defaultPrice = "$34.99",
            defaultPeriod = "/ year (Save 42%)",
            badge = "BEST VALUE",
            isSubscription = true
        ),
        ProTier(
            id = PRODUCT_PRO_MONTHLY,
            title = "Monthly Plan",
            defaultPrice = "$4.99",
            defaultPeriod = "/ month",
            badge = null,
            isSubscription = true
        ),
        ProTier(
            id = PRODUCT_LIFETIME_VIP,
            title = "Lifetime VIP",
            defaultPrice = "$79.99",
            defaultPeriod = "one-time payment",
            badge = "LIFETIME",
            isSubscription = false
        )
    )

    // Entitlement State
    private var hasLifetimeVip: Boolean
        get() = prefs.getBoolean("has_lifetime_vip", false)
        set(value) = prefs.edit().putBoolean("has_lifetime_vip", value).apply()

    private var hasActiveSubscription: Boolean
        get() = prefs.getBoolean("has_active_subscription", false)
        set(value) = prefs.edit().putBoolean("has_active_subscription", value).apply()

    // Testing / Dev mode flag (strictly disabled in production by default)
    var isTestModeEnabled: Boolean
        get() = prefs.getBoolean("is_test_mode_enabled", false)
        set(value) {
            prefs.edit().putBoolean("is_test_mode_enabled", value).apply()
            updateProStatus()
        }

    private var isTestProUnlocked: Boolean
        get() = prefs.getBoolean("is_test_pro_unlocked", false)
        set(value) = prefs.edit().putBoolean("is_test_pro_unlocked", value).apply()

    private val _isProUser = MutableStateFlow(hasLifetimeVip || hasActiveSubscription || (isTestModeEnabled && isTestProUnlocked))
    val isProUser: StateFlow<Boolean> = _isProUser.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _productDetailsMap = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetailsMap: StateFlow<Map<String, ProductDetails>> = _productDetailsMap.asStateFlow()

    private val _billingStatusMessage = MutableStateFlow<String?>(null)
    val billingStatusMessage: StateFlow<String?> = _billingStatusMessage.asStateFlow()

    private val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
        .enableOneTimeProducts()
        .build()

    val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(pendingPurchasesParams)
        .build()

    init {
        startBillingConnection()
    }

    fun startBillingConnection(onConnected: () -> Unit = {}) {
        _isLoading.value = true
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                _isLoading.value = false
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Google Play Billing connected successfully.")
                    _isConnected.value = true
                    queryPurchases()
                    queryProductDetails()
                    onConnected()
                } else {
                    Log.w(TAG, "Billing setup failed: code ${billingResult.responseCode} - ${billingResult.debugMessage}")
                    _isConnected.value = false
                    _billingStatusMessage.value = "Google Play disconnected: ${billingResult.debugMessage}"
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected.")
                _isConnected.value = false
                _isLoading.value = false
            }
        })
    }

    fun queryProductDetails() {
        if (!_isConnected.value) return

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_PRO_ANNUAL)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_PRO_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_LIFETIME_VIP)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val list = queryProductDetailsResult.productDetailsList ?: emptyList()
                val map = list.associateBy { it.productId }
                _productDetailsMap.value = map
                Log.d(TAG, "Successfully loaded ${map.size} products from Google Play.")
            } else {
                Log.w(TAG, "queryProductDetails failed: ${billingResult.debugMessage}")
            }
        }
    }

    /**
     * Resolves the real formatted price from Google Play ProductDetails.
     * Falls back to default estimate if product details haven't returned yet.
     */
    fun getFormattedPrice(productId: String): String {
        val details = _productDetailsMap.value[productId]
        if (details != null) {
            when (details.productType) {
                BillingClient.ProductType.SUBS -> {
                    val pricingPhase = details.subscriptionOfferDetails?.firstOrNull()
                        ?.pricingPhases?.pricingPhaseList?.firstOrNull()
                    if (pricingPhase != null) {
                        return pricingPhase.formattedPrice
                    }
                }
                BillingClient.ProductType.INAPP -> {
                    val oneTimePrice = details.oneTimePurchaseOfferDetails?.formattedPrice
                    if (oneTimePrice != null) {
                        return oneTimePrice
                    }
                }
            }
        }
        val defaultTier = proTiers.find { it.id == productId }
        return defaultTier?.defaultPrice ?: ""
    }

    fun queryPurchases(onFinished: (Boolean) -> Unit = {}) {
        if (!_isConnected.value) {
            onFinished(_isProUser.value)
            return
        }

        _isLoading.value = true

        // 1. Query subscriptions
        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(subsParams) { subsResult, subsPurchases ->
            val hasSubs = if (subsResult.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchases(subsPurchases, isSubscription = true)
            } else {
                false
            }

            // 2. Query in-app purchases (Lifetime VIP)
            val inAppParams = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()

            billingClient.queryPurchasesAsync(inAppParams) { inAppResult, inAppPurchases ->
                _isLoading.value = false
                val hasLifetime = if (inAppResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    processPurchases(inAppPurchases, isSubscription = false)
                } else {
                    false
                }

                // If Google Play confirms no active subscription, expire subscription entitlement
                if (subsResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    hasActiveSubscription = hasSubs
                }
                // If Google Play returns in-app lifetime, record it
                if (inAppResult.responseCode == BillingClient.BillingResponseCode.OK && hasLifetime) {
                    hasLifetimeVip = true
                }

                updateProStatus()
                onFinished(_isProUser.value)
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        _isLoading.value = false
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (!purchases.isNullOrEmpty()) {
                    var activated = false
                    for (purchase in purchases) {
                        if (purchase.products.contains(PRODUCT_LIFETIME_VIP)) {
                            if (handlePurchaseState(purchase)) {
                                hasLifetimeVip = true
                                activated = true
                            }
                        } else if (purchase.products.contains(PRODUCT_PRO_ANNUAL) || purchase.products.contains(PRODUCT_PRO_MONTHLY)) {
                            if (handlePurchaseState(purchase)) {
                                hasActiveSubscription = true
                                activated = true
                            }
                        }
                    }
                    updateProStatus()
                    if (activated) {
                        _billingStatusMessage.value = "Purchase successful! Welcome to NoteFlow Pro 🎉"
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _billingStatusMessage.value = "Purchase cancelled."
                Log.d(TAG, "User cancelled purchase flow.")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                _billingStatusMessage.value = "Item already owned. Restoring your entitlement..."
                queryPurchases()
            }
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> {
                _billingStatusMessage.value = "Google Play Store service unavailable. Check internet connection."
            }
            BillingClient.BillingResponseCode.NETWORK_ERROR -> {
                _billingStatusMessage.value = "Network error. Please check your connection."
            }
            else -> {
                _billingStatusMessage.value = "Purchase error: ${billingResult.debugMessage.ifBlank { "Error code ${billingResult.responseCode}" }}"
            }
        }
    }

    private fun handlePurchaseState(purchase: Purchase): Boolean {
        return when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                if (!purchase.isAcknowledged) {
                    val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams) { ackResult ->
                        Log.d(TAG, "Acknowledge purchase response: ${ackResult.responseCode}")
                    }
                }
                true
            }
            Purchase.PurchaseState.PENDING -> {
                _billingStatusMessage.value = "Purchase is pending. Entitlement will be activated automatically once payment clears."
                false
            }
            else -> {
                false
            }
        }
    }

    private fun processPurchases(purchases: List<Purchase>, isSubscription: Boolean): Boolean {
        var hasValidPurchase = false
        for (purchase in purchases) {
            if (handlePurchaseState(purchase)) {
                hasValidPurchase = true
            }
        }
        return hasValidPurchase
    }

    fun launchBilling(activity: Activity, productId: String, onError: (String) -> Unit = {}) {
        _billingStatusMessage.value = null
        val details = _productDetailsMap.value[productId]

        if (details == null) {
            // Requirement 7: Simulated purchases are ONLY allowed when explicitly enabled in development mode.
            if (isTestModeEnabled) {
                simulateTestPurchase(productId)
            } else {
                val errorMsg = "Cannot initiate purchase: Product details for $productId are not yet available from Google Play. Please check your internet or Play Store configuration."
                _billingStatusMessage.value = errorMsg
                onError(errorMsg)
            }
            return
        }

        _isLoading.value = true

        val productDetailsParamsList = when (details.productType) {
            BillingClient.ProductType.SUBS -> {
                val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
                if (offerToken.isNullOrBlank()) {
                    _isLoading.value = false
                    val errorMsg = "No eligible subscription offer token found for $productId in Google Play."
                    _billingStatusMessage.value = errorMsg
                    onError(errorMsg)
                    return
                }
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offerToken)
                        .build()
                )
            }
            BillingClient.ProductType.INAPP -> {
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            }
            else -> {
                _isLoading.value = false
                val errorMsg = "Unsupported product type: ${details.productType}"
                _billingStatusMessage.value = errorMsg
                onError(errorMsg)
                return
            }
        }

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val responseCode = billingClient.launchBillingFlow(activity, billingFlowParams).responseCode
        if (responseCode != BillingClient.BillingResponseCode.OK) {
            _isLoading.value = false
            val errorMsg = "Failed to launch Google Play billing flow: code $responseCode"
            _billingStatusMessage.value = errorMsg
            onError(errorMsg)
        }
    }

    /**
     * Requirement 7: Simulated test purchases are gated behind isTestModeEnabled.
     * Never grants production entitlements unless test mode is active.
     */
    fun simulateTestPurchase(productId: String) {
        if (!isTestModeEnabled) return
        isTestProUnlocked = true
        if (productId == PRODUCT_LIFETIME_VIP) {
            hasLifetimeVip = true
        } else {
            hasActiveSubscription = true
        }
        updateProStatus()
        _billingStatusMessage.value = "[TEST SANDBOX] Test purchase successful! Pro unlocked."
    }

    fun restorePurchases(onFinished: (Boolean) -> Unit) {
        _billingStatusMessage.value = "Checking Google Play purchases..."
        if (!_isConnected.value) {
            startBillingConnection {
                queryPurchases { isPro ->
                    onFinished(isPro)
                }
            }
        } else {
            queryPurchases { isPro ->
                onFinished(isPro)
            }
        }
    }

    private fun updateProStatus() {
        val pro = hasLifetimeVip || hasActiveSubscription || (isTestModeEnabled && isTestProUnlocked)
        _isProUser.value = pro
    }

    fun clearTestEntitlements() {
        isTestProUnlocked = false
        hasActiveSubscription = false
        hasLifetimeVip = false
        updateProStatus()
        _billingStatusMessage.value = "Test entitlements cleared."
    }
}
