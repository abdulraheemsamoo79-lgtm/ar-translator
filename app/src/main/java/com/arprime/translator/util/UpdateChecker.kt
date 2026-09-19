package com.arprime.translator.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val changelog: String,
    val forceUpdate: Boolean
)

/**
 * Points at the Node/Fastify backend in /backend. Change this once you deploy
 * it to Railway (e.g. "https://ar-translator-backend.up.railway.app/version").
 */
private const val VERSION_ENDPOINT = "https://ar-translator-backend.up.railway.app/version"

object UpdateChecker {

    private val client = OkHttpClient()

    /** Returns null if the check fails (offline, server down) or the app is already current. */
    suspend fun checkForUpdate(currentVersionCode: Int): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(VERSION_ENDPOINT).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val info = UpdateInfo(
                    versionCode = json.getInt("versionCode"),
                    versionName = json.getString("versionName"),
                    downloadUrl = json.getString("downloadUrl"),
                    changelog = json.optString("changelog", "Bug fixes and improvements."),
                    forceUpdate = json.optBoolean("forceUpdate", false)
                )
                if (info.versionCode > currentVersionCode) info else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
