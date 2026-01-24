package com.momentummm.app.data.manager

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class LanguageManager(private val context: Context) {
    
    private val prefs = context.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
    private val _currentLanguage = MutableStateFlow(getCurrentLanguage())
    val currentLanguage: Flow<String> = _currentLanguage.asStateFlow()

    companion object {
        private const val KEY_LANGUAGE = "selected_language"
        const val LANGUAGE_SYSTEM = "system"
        const val LANGUAGE_SPANISH = "es"
        const val LANGUAGE_ENGLISH = "en"
        const val LANGUAGE_PORTUGUESE = "pt"
        const val LANGUAGE_FRENCH = "fr"
        const val LANGUAGE_GERMAN = "de"
    }

    data class Language(
        val code: String,
        val displayName: String,
        val nativeName: String
    )

    val availableLanguages = listOf(
        Language(LANGUAGE_SYSTEM, "Sistema", "System Default"),
        Language(LANGUAGE_SPANISH, "Español", "Español"),
        Language(LANGUAGE_ENGLISH, "Inglés", "English"),
        Language(LANGUAGE_PORTUGUESE, "Portugués", "Português"),
        Language(LANGUAGE_FRENCH, "Francés", "Français"),
        Language(LANGUAGE_GERMAN, "Alemán", "Deutsch")
    )

    fun getCurrentLanguage(): String {
        return prefs.getString(KEY_LANGUAGE, LANGUAGE_SYSTEM) ?: LANGUAGE_SYSTEM
    }

    fun setLanguage(languageCode: String) {
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
        _currentLanguage.value = languageCode
        
        // Aplicar el cambio de idioma
        val localeList = if (languageCode == LANGUAGE_SYSTEM) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageCode)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun getLanguageDisplayName(code: String): String {
        return availableLanguages.find { it.code == code }?.nativeName ?: code
    }
}
