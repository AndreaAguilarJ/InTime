package com.momentummm.app.data.repository

import com.momentummm.app.data.appwrite.AppwriteService
import com.momentummm.app.data.model.SubscriptionStatus
import com.momentummm.app.data.model.UserSubscription
import io.appwrite.models.Document
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SubscriptionRepository(
    private val appwriteService: AppwriteService
) {
    companion object {
        /**
         * Días que dura la prueba gratuita.
         *
         * Única fuente de verdad: la interfaz recibe este número como argumento en lugar
         * de escribirlo en el texto. Antes el "7" vivía duplicado en la copia y en
         * plusDays(7), sin nada que los mantuviera de acuerdo.
         */
        const val TRIAL_DAYS = 7
    }

    private val _userSubscription = MutableStateFlow<UserSubscription?>(null)
    val userSubscription: Flow<UserSubscription?> = _userSubscription.asStateFlow()
    
    suspend fun getUserSubscription(userId: String): UserSubscription? {
        return try {
            val documents = appwriteService.databases.listDocuments(
                databaseId = appwriteService.databaseId,
                collectionId = "subscriptions",
                queries = listOf(io.appwrite.Query.equal("userId", userId))
            )
            
            if (documents.documents.isNotEmpty()) {
                val doc = documents.documents.firstOrNull() ?: return null
                val subscription = UserSubscription(
                    userId = doc.data["userId"] as? String ?: userId,
                    status = try { 
                        SubscriptionStatus.valueOf(doc.data["status"] as? String ?: "FREE") 
                    } catch (e: IllegalArgumentException) { 
                        SubscriptionStatus.FREE 
                    },
                    expiryDate = doc.data["expiryDate"] as? String,
                    trialEndsAt = doc.data["trialEndsAt"] as? String,
                    isTrialUsed = doc.data["isTrialUsed"] as? Boolean ?: false
                )
                _userSubscription.value = subscription
                subscription
            } else {
                // Create default free subscription
                val freeSubscription = UserSubscription(
                    userId = userId,
                    status = SubscriptionStatus.FREE
                )
                createSubscription(freeSubscription)
                _userSubscription.value = freeSubscription
                freeSubscription
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun createSubscription(subscription: UserSubscription): Boolean {
        return try {
            appwriteService.databases.createDocument(
                databaseId = appwriteService.databaseId,
                collectionId = "subscriptions",
                documentId = io.appwrite.ID.unique(),
                data = mapOf(
                    "userId" to subscription.userId,
                    "status" to subscription.status.name,
                    "expiryDate" to subscription.expiryDate,
                    "trialEndsAt" to subscription.trialEndsAt,
                    "isTrialUsed" to subscription.isTrialUsed
                )
            )
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun updateSubscription(subscription: UserSubscription): Boolean {
        return try {
            val documents = appwriteService.databases.listDocuments(
                databaseId = appwriteService.databaseId,
                collectionId = "subscriptions",
                queries = listOf(io.appwrite.Query.equal("userId", subscription.userId))
            )
            
            if (documents.documents.isNotEmpty()) {
                val docId = documents.documents.firstOrNull()?.id ?: return false
                appwriteService.databases.updateDocument(
                    databaseId = appwriteService.databaseId,
                    collectionId = "subscriptions",
                    documentId = docId,
                    data = mapOf(
                        "status" to subscription.status.name,
                        "expiryDate" to subscription.expiryDate,
                        "trialEndsAt" to subscription.trialEndsAt,
                        "isTrialUsed" to subscription.isTrialUsed
                    )
                )
                _userSubscription.value = subscription
                true
            } else {
                createSubscription(subscription)
            }
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun startFreeTrial(userId: String): Boolean {
        val current = getUserSubscription(userId)
        if (current?.isTrialUsed == true) {
            return false // Trial already used
        }
        
        val trialEndDate = LocalDate.now().plusDays(TRIAL_DAYS.toLong())
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
        val trialSubscription = UserSubscription(
            userId = userId,
            status = SubscriptionStatus.TRIAL,
            trialEndsAt = trialEndDate,
            isTrialUsed = true
        )
        
        return updateSubscription(trialSubscription)
    }
    
    suspend fun upgradeToPremium(userId: String, isYearly: Boolean): Boolean {
        val expiryDate = if (isYearly) {
            LocalDate.now().plusYears(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
        } else {
            LocalDate.now().plusMonths(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
        }
        
        val premiumSubscription = UserSubscription(
            userId = userId,
            status = if (isYearly) SubscriptionStatus.PREMIUM_YEARLY else SubscriptionStatus.PREMIUM_MONTHLY,
            expiryDate = expiryDate,
            isTrialUsed = true
        )
        
        return updateSubscription(premiumSubscription)
    }
    
    fun isPremiumUser(): Boolean {
        val subscription = _userSubscription.value
        return when (subscription?.status) {
            SubscriptionStatus.PREMIUM_MONTHLY,
            SubscriptionStatus.PREMIUM_YEARLY,
            SubscriptionStatus.TRIAL -> {
                // Check if subscription is still valid
                try {
                    subscription.expiryDate?.let { expiryDate ->
                        LocalDate.parse(expiryDate).isAfter(LocalDate.now())
                    } ?: subscription.trialEndsAt?.let { trialEndDate ->
                        LocalDate.parse(trialEndDate).isAfter(LocalDate.now())
                    } ?: false
                } catch (e: Exception) {
                    // Si hay error de parsing, asumir que no es premium para seguridad
                    false
                }
            }
            else -> false
        }
    }
    
    fun isTrialAvailable(): Boolean {
        return _userSubscription.value?.isTrialUsed != true
    }
    
    fun getRemainingTrialDays(): Int {
        val subscription = _userSubscription.value
        return if (subscription?.status == SubscriptionStatus.TRIAL) {
            try {
                subscription.trialEndsAt?.let { trialEndDate ->
                    val endDate = LocalDate.parse(trialEndDate)
                    val today = LocalDate.now()
                    if (endDate.isAfter(today)) {
                        endDate.toEpochDay().toInt() - today.toEpochDay().toInt()
                    } else 0
                } ?: 0
            } catch (e: Exception) {
                0 // Si hay error de parsing, retornar 0 días
            }
        } else 0
    }
}