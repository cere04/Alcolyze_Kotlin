package com.example.alcolyze.data

import android.content.Context

/** Ricorda una sola cosa: se l'utente ha scelto il tema chiaro o quello scuro. */
class ThemePreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isDarkTheme(): Boolean = prefs.getBoolean(KEY_DARK_THEME, DEFAULT_DARK_THEME)

    fun setDarkTheme(isDark: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_THEME, isDark).apply()
    }

    companion object {
        private const val PREFS_NAME = "alcolyze_prefs"
        private const val KEY_DARK_THEME = "dark_theme"
        // Se l'utente non ha ancora scelto, si parte col tema scuro.
        private const val DEFAULT_DARK_THEME = true
    }
}
