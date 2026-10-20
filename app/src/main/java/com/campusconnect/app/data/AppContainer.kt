package com.campusconnect.app.data

import android.content.Context
import androidx.work.WorkManager
import com.campusconnect.app.BuildConfig
import com.campusconnect.app.push.Push
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

/** Manual dependency container: one Supabase client, one repository, one cache. */
class AppContainer(val appContext: Context) {
    val client = createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY) {
        install(Auth)
        install(Postgrest)
        install(Storage)
    }
    val cache = Cache(appContext)
    val repo = Repo(client)
    val settings = Settings(appContext)

    /** Set when the user taps a push/system notification; consumed by the main screen. */
    val pendingLink = MutableStateFlow<DeepLink?>(null)

    val userId: String? get() = client.auth.currentUserOrNull()?.id

    suspend fun signOut() {
        try { Push.unregister(this) } catch (_: Exception) {}
        WorkManager.getInstance(appContext).cancelUniqueWork("campus_sync")
        appContext.getSharedPreferences("sync", Context.MODE_PRIVATE).edit().clear().apply()
        cache.clear()
        File(appContext.filesDir, "materials").deleteRecursively()
        repo.signOut()
    }
}
