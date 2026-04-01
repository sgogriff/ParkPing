package com.gowain.parkping.ui

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.gowain.parkping.model.AppLanguage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLanguageManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun currentLanguage(): AppLanguage {
        return preferences.getString(KEY_LANGUAGE, null)
            ?.let { stored -> AppLanguage.entries.firstOrNull { it.name == stored } }
            ?: AppLanguage.ENGLISH
    }

    fun applyCurrentLanguage() {
        apply(currentLanguage())
    }

    fun persistAndApply(language: AppLanguage) {
        preferences.edit().putString(KEY_LANGUAGE, language.name).apply()
        apply(language)
    }

    fun apply(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.languageTag))
    }

    companion object {
        private const val PREFERENCES_NAME = "park_ping_app"
        private const val KEY_LANGUAGE = "app_language"
    }
}
