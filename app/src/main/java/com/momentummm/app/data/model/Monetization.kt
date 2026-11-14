package com.momentummm.app.data.model

/**
 * Si la app puede ofrecer compras.
 *
 * Medido el 13 nov 2026 en Play Console: Momentum no tiene ninguna suscripción
 * ni ningún producto único creado, y la cuenta de desarrollador no ha declarado
 * su estado de comerciante. Con eso ningún cobro puede funcionar:
 *  - la tarjeta premium del inicio llevaba a una pantalla cuyo «Suscribirse» no
 *    hacía nada (el flujo de compra nunca se lanzó);
 *  - el desbloqueo de emergencia de pago siempre terminaba en error, porque el
 *    producto no existe.
 *
 * Google Play rechaza botones que no responden («funcionalidad defectuosa»),
 * así que mientras esto sea false ninguna pantalla ofrece comprar. Las
 * funciones premium ya estaban abiertas para todos (Análisis y Metas tenían el
 * muro de pago desactivado).
 *
 * Para activarlo hace falta, por este orden: declarar el estado de comerciante,
 * crear en Play los productos [com.momentummm.app.data.manager.BillingManager]
 * `PREMIUM_MONTHLY_SKU`, `PREMIUM_YEARLY_SKU` y `EMERGENCY_UNLOCK_SKU`, y conectar
 * «Suscribirse» con `BillingManager.launchPurchaseFlow`.
 */
object Monetization {
    const val PLAY_PRODUCTS_ON_SALE = false
}
