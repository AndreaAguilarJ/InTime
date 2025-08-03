package com.momentum.app.data.manager

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BillingManager(
    private val context: Context
) : PurchasesUpdatedListener, BillingClientStateListener {
    
    companion object {
        const val PREMIUM_MONTHLY_SKU = "premium_monthly_subscription"
        const val PREMIUM_YEARLY_SKU = "premium_yearly_subscription"
    }
    
    private val _billingConnectionState = MutableStateFlow(BillingConnectionState.DISCONNECTED)
    val billingConnectionState: StateFlow<BillingConnectionState> = _billingConnectionState.asStateFlow()
    
    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()
    
    private val _subscriptionStatus = MutableStateFlow<SubscriptionStatus>(SubscriptionStatus.NOT_SUBSCRIBED)
    val subscriptionStatus: StateFlow<SubscriptionStatus> = _subscriptionStatus.asStateFlow()
    
    private val _availableProducts = MutableStateFlow<List<ProductDetails>>(emptyList())
    val availableProducts: StateFlow<List<ProductDetails>> = _availableProducts.asStateFlow()
    
    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()
    
    enum class BillingConnectionState {
        CONNECTING, CONNECTED, DISCONNECTED, FAILED
    }
    
    enum class PurchaseState {
        Idle, Purchasing, Purchased, Failed, Cancelled
    }
    
    enum class SubscriptionStatus {
        NOT_SUBSCRIBED, MONTHLY_SUBSCRIBED, YEARLY_SUBSCRIBED, TRIAL
    }
    
    fun startConnection() {
        if (billingClient.isReady) {
            _billingConnectionState.value = BillingConnectionState.CONNECTED
            return
        }
        
        _billingConnectionState.value = BillingConnectionState.CONNECTING
        billingClient.startConnection(this)
    }
    
    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _billingConnectionState.value = BillingConnectionState.CONNECTED
            queryAvailableProducts()
            queryExistingPurchases()
        } else {
            _billingConnectionState.value = BillingConnectionState.FAILED
        }
    }
    
    override fun onBillingServiceDisconnected() {
        _billingConnectionState.value = BillingConnectionState.DISCONNECTED
    }
    
    private fun queryAvailableProducts() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_MONTHLY_SKU)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_YEARLY_SKU)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        
        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _availableProducts.value = productDetailsList
            }
        }
    }
    
    private fun queryExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        
        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchases(purchases)
            }
        }
    }
    
    suspend fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails): Boolean {
        return suspendCancellableCoroutine { continuation ->
            if (!billingClient.isReady) {
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }
            
            val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (offerToken == null) {
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }
            
            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            )
            
            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()
            
            _purchaseState.value = PurchaseState.Purchasing
            
            val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
            continuation.resume(billingResult.responseCode == BillingClient.BillingResponseCode.OK)
        }
    }
    
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.let { handlePurchases(it) }
                _purchaseState.value = PurchaseState.Purchased
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _purchaseState.value = PurchaseState.Cancelled
            }
            else -> {
                _purchaseState.value = PurchaseState.Failed
            }
        }
    }
    
    private fun handlePurchases(purchases: List<Purchase>) {
        for (purchase in purchases) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                // Verify the purchase on your server before granting entitlement
                when {
                    purchase.products.contains(PREMIUM_MONTHLY_SKU) -> {
                        _subscriptionStatus.value = SubscriptionStatus.MONTHLY_SUBSCRIBED
                    }
                    purchase.products.contains(PREMIUM_YEARLY_SKU) -> {
                        _subscriptionStatus.value = SubscriptionStatus.YEARLY_SUBSCRIBED
                    }
                }
                
                // Acknowledge the purchase if it hasn't been acknowledged yet
                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                }
            }
        }
        
        if (purchases.isEmpty()) {
            _subscriptionStatus.value = SubscriptionStatus.NOT_SUBSCRIBED
        }
    }
    
    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        
        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            // Handle acknowledgment result
        }
    }
    
    fun getProductDetails(productId: String): ProductDetails? {
        return _availableProducts.value.find { it.productId == productId }
    }
    
    fun isPremiumUser(): Boolean {
        return _subscriptionStatus.value != SubscriptionStatus.NOT_SUBSCRIBED
    }
    
    fun endConnection() {
        billingClient.endConnection()
    }
}