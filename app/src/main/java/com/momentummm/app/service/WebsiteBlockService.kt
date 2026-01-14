package com.momentummm.app.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.momentummm.app.R
import com.momentummm.app.data.repository.WebsiteBlockRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WebsiteBlockService : AccessibilityService() {

    @Inject
    lateinit var websiteBlockRepository: WebsiteBlockRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val browserPackages = setOf(
        "com.android.chrome",
        "org.mozilla.firefox",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.android.browser",
        "com.UCMobile.intl",
        "com.brave.browser",
        "com.microsoft.emmx",
        "com.sec.android.app.sbrowser", // Samsung Internet
        "org.chromium.chrome",
        "com.kiwibrowser.browser",
        "com.duckduckgo.mobile.android"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Service connected
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return

        // Solo procesar eventos de navegadores
        if (!browserPackages.contains(packageName)) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                checkUrlAndBlock()
            }
            else -> {} // Ignorar otros eventos
        }
    }

    private fun checkUrlAndBlock() {
        val rootNode = rootInActiveWindow ?: return
        val url = extractUrl(rootNode) ?: return

        serviceScope.launch {
            val isBlocked = websiteBlockRepository.isUrlBlocked(url)
            if (isBlocked) {
                blockWebsite()
                showBlockedNotification(url)
            }
        }
    }

    private fun extractUrl(node: AccessibilityNodeInfo?): String? {
        if (node == null) return null

        // Intentar obtener la URL de la barra de direcciones
        if (node.viewIdResourceName?.contains("url_bar") == true ||
            node.viewIdResourceName?.contains("address") == true ||
            node.viewIdResourceName?.contains("search") == true) {
            node.text?.toString()?.let { return it }
        }

        // Buscar recursivamente en los nodos hijos
        for (i in 0 until node.childCount) {
            val url = extractUrl(node.getChild(i))
            if (url != null) return url
        }

        return null
    }

    private fun blockWebsite() {
        // Cerrar la actividad del navegador actual
        performGlobalAction(GLOBAL_ACTION_BACK)
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    private fun showBlockedNotification(url: String) {
        val notification = NotificationCompat.Builder(this, "website_block_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Sitio web bloqueado")
            .setContentText("Has intentado acceder a un sitio bloqueado: $url")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(
                System.currentTimeMillis().toInt(),
                notification
            )
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }

    override fun onInterrupt() {
        // Service interrupted
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
