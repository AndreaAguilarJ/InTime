package com.momentummm.app.build

import com.momentummm.app.data.model.Monetization
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Google Play rechazó la 1.3.1 (17) el 4 nov 2026 por tres motivos que venían
 * del mismo sitio: el revisor no pudo pasar del inicio de sesión (el servidor
 * de cuentas estaba en solo lectura) y la app entera estaba detrás de él.
 * Además había botones de compra que no podían cobrar.
 *
 * Estas guardas leen el código y fallan si:
 *  - la entrada a la app vuelve a depender solo de tener sesión;
 *  - la bienvenida pierde «Usar sin cuenta»;
 *  - una pantalla ofrece comprar sin que haya productos a la venta;
 *  - se activa la venta sin conectar el flujo real de compra.
 */
class PlayReviewAccessGuardTest {

    private val appDir: File = listOf(File("."), File("app"))
        .firstOrNull { File(it, "src/main/AndroidManifest.xml").isFile }
        ?: error("No se encontró el módulo app desde ${File(".").absolutePath}")

    private val ui = File(appDir, "src/main/java/com/momentummm/app/ui")

    private fun source(relative: String): String = File(ui, relative).readText()

    @Test
    fun `se entra en la app con sesion o sin cuenta`() {
        val app = source("screen/MomentumApp.kt")
        assertTrue(
            "MomentumApp debe decidir la entrada con AccountAccess.entersApp(isLoggedIn, localMode)",
            app.contains("AccountAccess.entersApp(isLoggedIn, localMode)")
        )
        assertTrue(
            "Si el servidor de cuentas no atiende hay que entrar sin cuenta",
            app.contains("AccountAccess.shouldAutoEnterLocal(")
        )
        assertTrue(
            "La bienvenida tiene que ofrecer «Usar sin cuenta»",
            app.contains("onUseWithoutAccount = { application.localModeStore.setLocalMode(true) }")
        )
        val auth = source("screen/auth/AuthScreens.kt")
        val welcome = auth.substringAfter("fun WelcomeScreen(").substringBefore("fun SignUpScreen(")
        assertTrue(
            "WelcomeScreen tiene que mostrar el botón «Usar sin cuenta»",
            welcome.contains("onClick = onUseWithoutAccount") &&
                welcome.contains("R.string.auth_use_without_account")
        )
    }

    @Test
    fun `ninguna pantalla ofrece comprar si no hay productos a la venta`() {
        val dashboard = source("screen/DashboardScreen.kt")
        val promoLine = dashboard.lines().indexOfFirst { it.contains("item { PremiumPromotionCard(") }
        assertTrue("No se encontró la tarjeta premium del inicio", promoLine > 0)
        assertTrue(
            "La tarjeta premium del inicio debe depender de Monetization.PLAY_PRODUCTS_ON_SALE",
            dashboard.lines()[promoLine - 1].contains("Monetization.PLAY_PRODUCTS_ON_SALE")
        )

        val unlock = source("component/EmergencyUnlockScreen.kt")
        val paymentOption = unlock.substringAfter("OPCIÓN 1").substringBefore("UnlockOptionCard(")
        assertTrue(
            "El desbloqueo de pago debe depender de Monetization.PLAY_PRODUCTS_ON_SALE",
            paymentOption.contains("Monetization.PLAY_PRODUCTS_ON_SALE")
        )
    }

    @Test
    fun `sin cuenta ninguna pantalla depende del servidor de cuentas`() {
        val focus = source("screen/focus/FocusSessionScreen.kt")
        assertTrue(
            "Enfoque debe guardar cada sesión en el teléfono, haya o no cuenta",
            focus.contains("application.localFocusSessionStore.save(focusSession)") &&
                !focus.substringBefore("localFocusSessionStore.save").contains("currentUser.value?.let { user ->")
        )
        assertTrue(
            "Sin cuenta, las cifras de Enfoque salen del registro local",
            focus.contains("LocalFocusLog.stats(localSessions")
        )

        val app = source("screen/MomentumApp.kt")
        val lifeWeeksLine = app.lines().first { it.contains("LifeWeeksScreen(viewModel = viewModel") }
        assertTrue(
            "El botón de fecha de Mi vida no puede llevar a Ajustes de cuenta (exige sesión)",
            !lifeWeeksLine.contains("\"account_settings\"")
        )

        val settings = source("screen/SettingsScreen.kt")
        val syncItem = settings.lines().indexOfFirst { it.contains("onNavigateToScreen(\"sync_settings\")") }
        val window = settings.lines().subList(maxOf(0, syncItem - 8), syncItem).joinToString("\n")
        assertTrue("«Sincronización» solo se muestra con cuenta", window.contains("if (hasAccount)"))
    }

    @Test
    fun `al arrancar sin sesion se comprueba si el servidor atiende`() {
        val service = File(appDir, "src/main/java/com/momentummm/app/data/appwrite/AppwriteService.kt").readText()
        val check = service.substringAfter("private fun checkCurrentUser()").substringBefore("private suspend fun probeServer()")
        assertTrue(
            "Sin sesión, GET /account da 401 aunque el proyecto esté en solo lectura: hay que sondear el servidor",
            check.contains("probeServer()")
        )
        val probe = service.substringAfter("private suspend fun probeServer()").substringBefore("suspend fun createAccount")
        assertTrue("El sondeo debe usar una consulta que el proyecto en solo lectura rechace con 402", probe.contains("Locale(client).get()"))
        assertTrue("El sondeo debe tener un límite de tiempo corto", probe.contains("withTimeout(PROBE_TIMEOUT_MS)"))
    }

    @Test
    fun `la venta solo se activa con el flujo real de compra conectado`() {
        if (!Monetization.PLAY_PRODUCTS_ON_SALE) return
        val callers = ui.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { it.readText().contains("launchPurchaseFlow(") }
            .toList()
        assertTrue(
            "PLAY_PRODUCTS_ON_SALE=true pero ninguna pantalla lanza BillingManager.launchPurchaseFlow: " +
                "«Suscribirse» no cobraría nada",
            callers.isNotEmpty()
        )
    }
}
