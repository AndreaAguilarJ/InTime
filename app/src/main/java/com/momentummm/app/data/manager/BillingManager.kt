package com.momentummm.app.data.manager

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
        const val EMERGENCY_UNLOCK_SKU = "emergency_unlock_consumable"
    }
    
    private val _billingConnectionState = MutableStateFlow(BillingConnectionState.DISCONNECTED)
    val billingConnectionState: StateFlow<BillingConnectionState> = _billingConnectionState.asStateFlow()
    
    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()
    
    private val _subscriptionStatus = MutableStateFlow<SubscriptionStatus>(SubscriptionStatus.NOT_SUBSCRIBED)
    val subscriptionStatus: StateFlow<SubscriptionStatus> = _subscriptionStatus.asStateFlow()
    
    // CRITICAL FIX: Persistir estado de suscripción en SharedPreferences
    // Antes solo estaba en memoria y se perdía si no había internet al abrir
    private val billingPrefs = context.getSharedPreferences("billing_state", Context.MODE_PRIVATE)
    
    private val _availableProducts = MutableStateFlow<List<ProductDetails>>(emptyList())
    val availableProducts: StateFlow<List<ProductDetails>> = _availableProducts.asStateFlow()
    
    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
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
        
        // CRITICAL FIX: Cargar último estado de suscripción conocido del disco
        loadPersistedSubscriptionStatus()
        
        _billingConnectionState.value = BillingConnectionState.CONNECTING
        billingClient.startConnection(this)
    }
    
    /**
     * CRITICAL FIX: Cargar estado de suscripción desde SharedPreferences.
     * Esto permite que usuarios premium mantengan acceso incluso sin internet.
     */
    private fun loadPersistedSubscriptionStatus() {
        val savedStatus = billingPrefs.getString("subscription_status", null)
        if (savedStatus != null) {
            try {
                _subscriptionStatus.value = SubscriptionStatus.valueOf(savedStatus)
            } catch (e: Exception) {
                // Ignorar valores inválidos
            }
        }
    }
    
    /**
     * CRITICAL FIX: Persistir estado de suscripción al disco.
     */
    private fun persistSubscriptionStatus(status: SubscriptionStatus) {
        _subscriptionStatus.value = status
        billingPrefs.edit().putString("subscription_status", status.name).apply()
    }
    
    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _billingConnectionState.value = BillingConnectionState.CONNECTED
            queryAvailableProducts()
            queryExistingPurchases()
            // CRITICAL FIX: Reintentar acknowledgments pendientes al reconectar
            retryPendingAcknowledgments()
        } else {
            _billingConnectionState.value = BillingConnectionState.FAILED
        }
    }
    
    override fun onBillingServiceDisconnected() {
        _billingConnectionState.value = BillingConnectionState.DISCONNECTED
        // CRITICAL FIX: Intentar reconexión con delay
        // Google recomienda exponential backoff para reconexión
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (_billingConnectionState.value == BillingConnectionState.DISCONNECTED) {
                startConnection()
            }
        }, 5000L) // Reintentar después de 5 segundos
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
    
    /**
     * Concede el acceso a las compras confirmadas por Google.
     *
     * BUG CORREGIDO: las suscripciones sólo se registraban si
     * `purchase.isAcknowledged` **ya** era true. Pero el acknowledge es un
     * paso que hace esta misma app justo después, así que en la compra recién
     * hecha siempre valía false: el usuario pagaba y no obtenía premium. Sólo
     * aparecía en un arranque posterior, cuando `queryPurchases` devolvía la
     * compra ya confirmada. Y si el acknowledge fallaba (sin red, proceso
     * muerto), Google reembolsaba a los 3 días.
     *
     * Lo correcto, y lo que recomienda Google Play Billing, es conceder el
     * derecho en cuanto el estado es PURCHASED y confirmar después.
     */
    private fun handlePurchases(purchases: List<Purchase>) {
        for (purchase in purchases) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                when {
                    purchase.products.contains(PREMIUM_MONTHLY_SKU) -> {
                        persistSubscriptionStatus(SubscriptionStatus.MONTHLY_SUBSCRIBED)
                    }
                    purchase.products.contains(PREMIUM_YEARLY_SKU) -> {
                        persistSubscriptionStatus(SubscriptionStatus.YEARLY_SUBSCRIBED)
                    }
                    purchase.products.contains(EMERGENCY_UNLOCK_SKU) -> {
                        // CRITICAL FIX: Consumir el emergency unlock para que se pueda comprar de nuevo
                        // Antes esta función nunca se llamaba - la compra se reembolsaba después de 3 días
                        consumeEmergencyUnlock(purchase)
                        _purchaseState.value = PurchaseState.Purchased
                    }
                }
                
                // Acknowledge the purchase if it hasn't been acknowledged yet
                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                }
            }
        }
        
        // CRITICAL FIX: Solo marcar como NOT_SUBSCRIBED si realmente verificamos
        // que no hay suscripciones activas (no cuando la lista está vacía por error de red)
        if (purchases.isEmpty() && billingClient.isReady) {
            persistSubscriptionStatus(SubscriptionStatus.NOT_SUBSCRIBED)
        }
    }
    
    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        
        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                android.util.Log.d("BillingManager", "Purchase acknowledged successfully")
                // CRITICAL FIX: Actualizar estado de suscripción DESPUÉS de acknowledge exitoso
                when {
                    purchase.products.contains(PREMIUM_MONTHLY_SKU) -> 
                        persistSubscriptionStatus(SubscriptionStatus.MONTHLY_SUBSCRIBED)
                    purchase.products.contains(PREMIUM_YEARLY_SKU) -> 
                        persistSubscriptionStatus(SubscriptionStatus.YEARLY_SUBSCRIBED)
                }
            } else {
                android.util.Log.e("BillingManager", "Failed to acknowledge. Code: ${billingResult.responseCode}")
                // CRITICAL FIX: Guardar token para retry posterior
                savePendingAcknowledgment(purchase.purchaseToken)
            }
        }
    }
    
    /**
     * CRITICAL FIX: Guardar compras pendientes de acknowledge para retry.
     * Sin esto, las compras se reembolsan después de 3 días.
     */
    private fun savePendingAcknowledgment(purchaseToken: String) {
        val pending = billingPrefs.getStringSet("pending_acks", mutableSetOf()) ?: mutableSetOf()
        val updated = pending.toMutableSet()
        updated.add(purchaseToken)
        billingPrefs.edit().putStringSet("pending_acks", updated).apply()
    }
    
    /**
     * Reintentar acknowledgments pendientes.
     * Debe llamarse cada vez que se conecte el billing client.
     */
    private fun retryPendingAcknowledgments() {
        val pending = billingPrefs.getStringSet("pending_acks", emptySet()) ?: return
        if (pending.isEmpty()) return
        
        for (token in pending.toSet()) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(token)
                .build()
            
            billingClient.acknowledgePurchase(params) { result ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    // Remover del set de pendientes
                    val current = billingPrefs.getStringSet("pending_acks", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                    current.remove(token)
                    billingPrefs.edit().putStringSet("pending_acks", current).apply()
                    android.util.Log.d("BillingManager", "Pending acknowledgment retried successfully")
                }
            }
        }
    }
    
    fun getProductDetails(productId: String): ProductDetails? {
        return _availableProducts.value.find { it.productId == productId }
    }
    
    fun isPremiumUser(): Boolean {
        return _subscriptionStatus.value != SubscriptionStatus.NOT_SUBSCRIBED
    }

    /**
     * Inicia la compra del desbloqueo de emergencia (consumible de $0.99).
     * Este es un producto de una sola vez que permite desbloquear una app por 15 minutos.
     */
    fun launchEmergencyUnlockPurchase(activity: Activity) {
        if (!billingClient.isReady) {
            startConnection()
            return
        }

        // Consultar el producto de desbloqueo de emergencia
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(EMERGENCY_UNLOCK_SKU)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val emergencyUnlockProduct = productDetailsList.firstOrNull()
                if (emergencyUnlockProduct != null) {
                    val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(emergencyUnlockProduct)
                        .build()

                    val billingFlowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(listOf(productDetailsParams))
                        .build()

                    _purchaseState.value = PurchaseState.Purchasing
                    billingClient.launchBillingFlow(activity, billingFlowParams)
                } else {
                    _purchaseState.value = PurchaseState.Failed
                }
            } else {
                _purchaseState.value = PurchaseState.Failed
            }
        }
    }

    /**
     * Consume un producto de desbloqueo de emergencia para permitir otra compra.
     */
    private fun consumeEmergencyUnlock(purchase: Purchase) {
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.consumeAsync(consumeParams) { billingResult, purchaseToken ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                // Producto consumido exitosamente
            }
        }
    }
    
    fun endConnection() {
        billingClient.endConnection()
    }
}