package com.momentummm.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * ÚNICO delegado del DataStore de preferencias de notificaciones de todo el
 * proceso.
 *
 * BUG CORREGIDO: existían DOS `by preferencesDataStore(name = "notification_preferences")`
 * —uno en [com.momentummm.app.data.manager.NotificationManager] y otro en
 * `NotificationSettingsScreen`— y cada delegado construye su PROPIA instancia de
 * DataStore sobre el mismo fichero. Al abrir la pantalla de notificaciones con el
 * manager ya activo (lo está en cuanto la app programa o comprueba una
 * notificación), Android lanzaba
 * `IllegalStateException: There are multiple DataStores active for the same file:
 * .../notification_preferences.preferences_pb` y tumbaba la app.
 *
 * El delegado de `preferencesDataStore` está diseñado para declararse UNA sola
 * vez por fichero en todo el proceso; esta es esa declaración única, que ambos
 * consumidores importan.
 */
internal val Context.notificationPrefs: DataStore<Preferences> by preferencesDataStore(
    name = "notification_preferences"
)
