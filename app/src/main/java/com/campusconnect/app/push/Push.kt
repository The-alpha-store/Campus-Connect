package com.campusconnect.app.push

import android.content.Context
import com.campusconnect.app.BuildConfig
import com.campusconnect.app.data.AppContainer
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * Firebase Cloud Messaging glue. Configured from local.properties / CI secrets instead of
 * google-services.json, so the app still builds and runs (without push) when Firebase is not set up.
 */
object Push {
    private fun configured() = BuildConfig.FIREBASE_APP_ID.isNotBlank() &&
        BuildConfig.FIREBASE_API_KEY.isNotBlank() &&
        BuildConfig.FIREBASE_PROJECT_ID.isNotBlank() &&
        BuildConfig.FIREBASE_SENDER_ID.isNotBlank()

    /** Returns true when Firebase is ready to use. */
    fun init(ctx: Context): Boolean {
        if (!configured()) return false
        return try {
            if (FirebaseApp.getApps(ctx).isEmpty()) {
                FirebaseApp.initializeApp(
                    ctx,
                    FirebaseOptions.Builder()
                        .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                        .setApiKey(BuildConfig.FIREBASE_API_KEY)
                        .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                        .setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID)
                        .build(),
                )
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("push", Context.MODE_PRIVATE)

    /** Fetches this phone's token and registers it for the signed-in user. Safe to call repeatedly. */
    suspend fun register(c: AppContainer, knownToken: String? = null) {
        if (!init(c.appContext)) return
        val token = knownToken ?: FirebaseMessaging.getInstance().token.await()
        c.repo.registerToken(token)
        prefs(c.appContext).edit().putString("token", token).apply()
    }

    /** Called before sign-out so the next person on this phone does not get this user's alerts. */
    suspend fun unregister(c: AppContainer) {
        val token = prefs(c.appContext).getString("token", null) ?: return
        c.repo.unregisterToken(token)
        prefs(c.appContext).edit().remove("token").apply()
    }
}
