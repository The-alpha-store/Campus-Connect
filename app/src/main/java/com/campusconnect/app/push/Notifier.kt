package com.campusconnect.app.push

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.campusconnect.app.CampusApp
import com.campusconnect.app.MainActivity

/** Shows a system notification that opens the right screen when tapped. Shared by FCM and the sync worker. */
object Notifier {
    const val EXTRA_KIND = "kind"
    const val EXTRA_TARGET = "target_id"

    private fun canNotify(ctx: Context) = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Remember an id so the fallback sync worker does not show the same notification again. */
    fun markSeen(ctx: Context, id: String) {
        val prefs = ctx.getSharedPreferences("sync", Context.MODE_PRIVATE)
        val seen = prefs.getStringSet("seen", emptySet())!!.toMutableSet()
        seen.add(id)
        prefs.edit().putStringSet("seen", seen.toList().takeLast(200).toSet()).apply()
    }

    fun isSeen(ctx: Context, id: String): Boolean =
        ctx.getSharedPreferences("sync", Context.MODE_PRIVATE).getStringSet("seen", emptySet())!!.contains(id)

    @SuppressLint("MissingPermission")
    fun show(ctx: Context, id: String, title: String, body: String, kind: String?, targetId: String?) {
        if (!canNotify(ctx)) return
        val intent = Intent(ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_KIND, kind)
            .putExtra(EXTRA_TARGET, targetId)
        val pending = PendingIntent.getActivity(
            ctx, id.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(ctx, CampusApp.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(id.hashCode(), n)
    }
}
