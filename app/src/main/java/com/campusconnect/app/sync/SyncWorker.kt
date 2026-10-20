package com.campusconnect.app.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.campusconnect.app.CampusApp
import com.campusconnect.app.Lang
import com.campusconnect.app.data.Settings
import com.campusconnect.app.push.Notifier
import com.campusconnect.app.tr
import io.github.jan.supabase.auth.auth
import java.util.concurrent.TimeUnit

/**
 * Checks for unread notifications every ~15 minutes and shows a system notification.
 * Works without Firebase. Swap for FCM later if you need instant delivery.
 */
class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val c = (applicationContext as CampusApp).container
        return try {
            c.client.auth.awaitInitialization()
            if (c.client.auth.currentUserOrNull() == null) return Result.success()

            Lang.set(Settings(applicationContext).lang.value)
            val fresh = c.repo.unreadNotifications().filter { !Notifier.isSeen(applicationContext, it.id) }
            fresh.take(5).forEach { n ->
                val target = n.announcementId ?: n.sessionId ?: n.assignmentId ?: n.materialId
                // Announcement titles are written by lecturers; the other titles are standard phrases we translate.
                val title = if (n.kind == "announcement") n.title else tr(n.title)
                Notifier.show(applicationContext, n.id, title, n.body.orEmpty(), n.kind, target)
            }
            fresh.forEach { Notifier.markSeen(applicationContext, it.id) }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun schedule(ctx: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(ctx)
                .enqueueUniquePeriodicWork("campus_sync", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
