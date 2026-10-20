package com.campusconnect.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/** Small user settings. Language is "en" or "sw"; defaults to the phone's language. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _lang = MutableStateFlow(
        prefs.getString("lang", null) ?: if (Locale.getDefault().language == "sw") "sw" else "en",
    )
    val lang: StateFlow<String> = _lang.asStateFlow()

    fun setLang(code: String) {
        prefs.edit().putString("lang", code).apply()
        _lang.value = code
    }
}
