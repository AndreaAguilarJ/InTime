package com.momentummm.app.data.model

import java.time.LocalDate

/**
 * Decide si una suscripción guardada da acceso premium en un día dado.
 *
 * Appwrite guarda `expiryDate` y `trialEndsAt` como atributos de fecha y los
 * devuelve con hora y zona («2027-12-31T00:00:00.000+00:00»), aunque la app
 * los escriba como «2027-12-31». `LocalDate.parse` del texto completo fallaba,
 * así que toda suscripción leída del servidor contaba como caducada. Se toma
 * solo la parte del día.
 */
object SubscriptionValidity {

    fun isPremium(subscription: UserSubscription?, today: LocalDate): Boolean {
        if (subscription == null || subscription.status == SubscriptionStatus.FREE) return false
        val end = endDay(subscription) ?: return false
        return end.isAfter(today)
    }

    fun remainingTrialDays(subscription: UserSubscription?, today: LocalDate): Int {
        if (subscription?.status != SubscriptionStatus.TRIAL) return 0
        val end = parseDay(subscription.trialEndsAt) ?: return 0
        return (end.toEpochDay() - today.toEpochDay()).coerceAtLeast(0).toInt()
    }

    private fun endDay(subscription: UserSubscription): LocalDate? =
        parseDay(subscription.expiryDate) ?: parseDay(subscription.trialEndsAt)

    /** «2027-12-31» o «2027-12-31T00:00:00.000+00:00» -> 2027-12-31; lo demás, null. */
    fun parseDay(raw: String?): LocalDate? {
        if (raw == null || raw.length < DAY_LENGTH) return null
        return runCatching { LocalDate.parse(raw.substring(0, DAY_LENGTH)) }.getOrNull()
    }

    private const val DAY_LENGTH = 10
}
