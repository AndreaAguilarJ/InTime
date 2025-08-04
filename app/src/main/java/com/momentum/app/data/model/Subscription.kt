package com.momentum.app.data.model

data class SubscriptionPlan(
    val id: String,
    val name: String,
    val priceMonthly: String,
    val priceYearly: String,
    val features: List<String>,
    val isPopular: Boolean = false
)

enum class SubscriptionStatus {
    FREE,
    PREMIUM_MONTHLY,
    PREMIUM_YEARLY,
    TRIAL
}

data class UserSubscription(
    val userId: String,
    val status: SubscriptionStatus,
    val expiryDate: String? = null,
    val trialEndsAt: String? = null,
    val isTrialUsed: Boolean = false
)

object SubscriptionPlans {
    val FREE = SubscriptionPlan(
        id = "free",
        name = "Gratis",
        priceMonthly = "0€",
        priceYearly = "0€",
        features = listOf(
            "Tiempo de pantalla básico",
            "5 apps más usadas",
            "Visualización básica de semanas de vida",
            "Widget básico",
            "Modo teléfono mínimo básico"
        )
    )
    
    val PREMIUM = SubscriptionPlan(
        id = "premium",
        name = "Premium",
        priceMonthly = "4.99€",
        priceYearly = "39.99€",
        features = listOf(
            "Análisis avanzado de uso",
            "Informes detallados semanales/mensuales",
            "Sesiones de enfoque con bloqueo de apps",
            "Metas personalizadas y recordatorios",
            "Temas y widgets personalizados",
            "Exportación de datos (CSV/PDF)",
            "Generación de fondos de pantalla",
            "Desafíos de desintoxicación digital",
            "Respaldo en la nube avanzado",
            "Perfiles múltiples (trabajo/personal)",
            "Acceso a todas las frases motivacionales",
            "Soporte prioritario"
        ),
        isPopular = true
    )
    
    fun getAllPlans() = listOf(FREE, PREMIUM)
}