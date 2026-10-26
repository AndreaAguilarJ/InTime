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
                val doc = documents.documents.first()
                val subscription = UserSubscription(
                    userId = doc.data["userId"] as String,
                    status = SubscriptionStatus.valueOf(doc.data["status"] as String),
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
                val docId = documents.documents.first().id
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
        
        val trialEndDate = LocalDate.now().plusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE)
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
                subscription.expiryDate?.let { expiryDate ->
                    LocalDate.parse(expiryDate).isAfter(LocalDate.now())
                } ?: subscription.trialEndsAt?.let { trialEndDate ->
                    LocalDate.parse(trialEndDate).isAfter(LocalDate.now())
                } ?: false
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
            subscription.trialEndsAt?.let { trialEndDate ->
                val endDate = LocalDate.parse(trialEndDate)
                val today = LocalDate.now()
                if (endDate.isAfter(today)) {
                    endDate.toEpochDay().toInt() - today.toEpochDay().toInt()
                } else 0
            } ?: 0
        } else 0
    }
}