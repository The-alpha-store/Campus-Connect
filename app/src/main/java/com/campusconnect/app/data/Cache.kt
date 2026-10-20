package com.campusconnect.app.data

import android.content.Context
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File

/** Tiny JSON file cache so the app opens instantly and works offline. */
class Cache(context: Context) {
    private val dir = File(context.filesDir, "cache").apply { mkdirs() }
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun file(key: String) = File(dir, key.replace(Regex("[^A-Za-z0-9_.-]"), "_") + ".json")

    fun <T> read(key: String, serializer: KSerializer<T>): T? = try {
        file(key).takeIf { it.exists() }?.readText()?.let { json.decodeFromString(serializer, it) }
    } catch (e: Exception) {
        null
    }

    fun <T> write(key: String, serializer: KSerializer<T>, value: T) {
        try { file(key).writeText(json.encodeToString(serializer, value)) } catch (_: Exception) {}
    }

    /** Called on logout so private data never survives a sign-out. */
    fun clear() {
        dir.listFiles()?.forEach { it.delete() }
    }
}
