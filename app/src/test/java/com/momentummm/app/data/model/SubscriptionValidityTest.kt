package com.momentummm.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SubscriptionValidityTest {

    private val today = LocalDate.of(2026, 10, 30)

    private fun sub(status: SubscriptionStatus, expiry: String? = null, trial: String? = null) =
        UserSubscription(userId = "u", status = status, expiryDate = expiry, trialEndsAt = trial)

    @Test
    fun `premium leido de Appwrite con hora y zona sigue vigente`() {
        assertTrue(
            SubscriptionValidity.isPremium(
                sub(SubscriptionStatus.PREMIUM_YEARLY, expiry = "2027-12-31T00:00:00.000+00:00"),
                today
            )
        )
    }

    @Test
    fun `premium escrito por la app como fecha simple sigue vigente`() {
        assertTrue(SubscriptionValidity.isPremium(sub(SubscriptionStatus.PREMIUM_MONTHLY, expiry = "2026-11-30"), today))
    }

    @Test
    fun `premium caducado o que termina hoy no da acceso`() {
        assertFalse(SubscriptionValidity.isPremium(sub(SubscriptionStatus.PREMIUM_YEARLY, expiry = "2026-10-29"), today))
        assertFalse(SubscriptionValidity.isPremium(sub(SubscriptionStatus.PREMIUM_YEARLY, expiry = "2026-10-30"), today))
    }

    @Test
    fun `prueba gratuita usa su fecha de fin`() {
        val trial = sub(SubscriptionStatus.TRIAL, trial = "2026-11-06T00:00:00.000+00:00")
        assertTrue(SubscriptionValidity.isPremium(trial, today))
        assertEquals(7, SubscriptionValidity.remainingTrialDays(trial, today))
    }

    @Test
    fun `gratis, sin documento o sin fecha valida no da acceso`() {
        assertFalse(SubscriptionValidity.isPremium(null, today))
        assertFalse(SubscriptionValidity.isPremium(sub(SubscriptionStatus.FREE, expiry = "2030-01-01"), today))
        assertFalse(SubscriptionValidity.isPremium(sub(SubscriptionStatus.PREMIUM_YEARLY), today))
        assertFalse(SubscriptionValidity.isPremium(sub(SubscriptionStatus.PREMIUM_YEARLY, expiry = "mañana"), today))
    }

    @Test
    fun `dias restantes nunca son negativos ni cuentan fuera de la prueba`() {
        assertEquals(0, SubscriptionValidity.remainingTrialDays(sub(SubscriptionStatus.TRIAL, trial = "2026-10-01"), today))
        assertEquals(0, SubscriptionValidity.remainingTrialDays(sub(SubscriptionStatus.PREMIUM_YEARLY, trial = "2026-11-06"), today))
    }

    @Test
    fun `parseDay acepta solo fechas reales`() {
        assertEquals(LocalDate.of(2027, 12, 31), SubscriptionValidity.parseDay("2027-12-31T00:00:00.000+00:00"))
        assertNull(SubscriptionValidity.parseDay("2027-13-01"))
        assertNull(SubscriptionValidity.parseDay(""))
    }
}
