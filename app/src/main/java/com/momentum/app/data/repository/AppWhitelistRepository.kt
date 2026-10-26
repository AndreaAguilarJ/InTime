package com.momentummm.app.data.repository

import android.content.Context
import com.momentummm.app.data.dao.AppWhitelistDao
import com.momentummm.app.data.entity.AppWhitelist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class AppWhitelistRepository @Inject constructor(
    private val appWhitelistDao: AppWhitelistDao,
    @ApplicationContext private val context: Context
) {

    fun getAllWhitelistedApps(): Flow<List<AppWhitelist>> =
        appWhitelistDao.getAllWhitelistedApps()

    suspend fun getWhitelistedApp(packageName: String): AppWhitelist? =
        appWhitelistDao.getWhitelistedApp(packageName)

    suspend fun isAppWhitelisted(packageName: String): Boolean =
        appWhitelistDao.isAppWhitelisted(packageName)

    suspend fun addToWhitelist(packageName: String, appName: String, reason: String = "Emergencias") {
        val app = AppWhitelist(
            packageName = packageName,
            appName = appName,
            reason = reason
        )
        appWhitelistDao.insertWhitelistedApp(app)
    }

    suspend fun removeFromWhitelist(packageName: String) {
        appWhitelistDao.removeWhitelistedAppByPackage(packageName)
    }

    suspend fun removeFromWhitelist(app: AppWhitelist) {
        appWhitelistDao.removeWhitelistedApp(app)
    }

    suspend fun clearWhitelist() {
        appWhitelistDao.clearWhitelist()
    }

    /**
     * Apps de emergencia predeterminadas que deberían estar en whitelist
     */
    suspend fun addDefaultEmergencyApps() {
        val emergencyApps = listOf(
            AppWhitelist(
                packageName = "com.android.phone",
                appName = "Teléfono",
                reason = "Llamadas de emergencia"
            ),
            AppWhitelist(
                packageName = "com.android.contacts",
                appName = "Contactos",
                reason = "Acceso a contactos de emergencia"
            ),
            AppWhitelist(
                packageName = "com.android.mms",
                appName = "Mensajes",
                reason = "SMS de emergencia"
            ),
            AppWhitelist(
                packageName = "com.google.android.apps.messaging",
                appName = "Mensajes (Google)",
                reason = "SMS de emergencia"
            ),
            AppWhitelist(
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                reason = "Comunicación de emergencia"
            ),
            AppWhitelist(
                packageName = "com.android.settings",
                appName = "Configuración",
                reason = "Configuración del sistema"
            )
        )

        emergencyApps.forEach { app ->
            try {
                // Verificar si la app está instalada
                context.packageManager.getApplicationInfo(app.packageName, 0)
                // Solo agregar si no está ya en la whitelist
                if (!isAppWhitelisted(app.packageName)) {
                    appWhitelistDao.insertWhitelistedApp(app)
                }
            } catch (_: Exception) {
                // App no instalada, ignorar
            }
        }
    }

    /**
     * Obtener lista de paquetes en whitelist (para verificación rápida)
     */
    suspend fun getWhitelistedPackages(): Set<String> {
        return getAllWhitelistedApps().first().map { it.packageName }.toSet()
    }
}
