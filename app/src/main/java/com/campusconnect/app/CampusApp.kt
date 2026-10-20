package com.campusconnect.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.campusconnect.app.data.AppContainer
import com.campusconnect.app.push.Push
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class CampusApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(CHANNEL_ID, "Campus updates", NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = "Announcements, timetable changes and deadlines" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        Push.init(this)
    }

    companion object {
        const val CHANNEL_ID = "campus_updates"
    }
}
