package com.campusconnect.app.push

import com.campusconnect.app.BuildConfig
import com.campusconnect.app.CampusApp
import com.campusconnect.app.Lang
import com.campusconnect.app.data.Settings
import com.campusconnect.app.tr
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.launch

/**
 * Receives FCM data messages sent by the `send-push` Edge Function.
 * Data-only messages let us build the notification ourselves, so tapping it opens the right screen.
 */
class CampusMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        if (BuildConfig.SUPABASE_URL.isBlank()) return
        val app = application as CampusApp
        app.appScope.launch {
            try {
                if (app.container.userId != null) Push.register(app.container, token)
            } catch (_: Exception) {
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val d = message.data
        val rawTitle = d["title"] ?: return
        Lang.set(Settings(applicationContext).lang.value)
        val title = if (d["kind"] == "announcement") rawTitle else tr(rawTitle)
        val id = d["notification_id"] ?: message.messageId ?: title.hashCode().toString()
        Notifier.markSeen(applicationContext, id)
        Notifier.show(applicationContext, id, title, d["body"].orEmpty(), d["kind"], d["target_id"])
    }
}
