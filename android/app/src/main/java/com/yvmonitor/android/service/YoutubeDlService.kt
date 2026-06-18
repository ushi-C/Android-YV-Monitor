package com.yvmonitor.android.service

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class YoutubeDlService(private val context: Context) {
    @Volatile private var initialized = false

    private fun ensureInitialized() {
        if (initialized) return
        synchronized(this) {
            if (!initialized) {
                YoutubeDL.getInstance().init(context)
                initialized = true
            }
        }
    }

    suspend fun probe(query: String, title: String): JSONObject = withContext(Dispatchers.IO) {
        ensureInitialized()
        val request = YoutubeDLRequest(query).apply {
            addOption("--dump-single-json")
            addOption("--no-playlist")
            addOption("--skip-download")
        }
        val response = YoutubeDL.getInstance().execute(request)
        val parsed = runCatching { JSONObject(response.out) }.getOrNull()
        JSONObject()
            .put("id", parsed?.optString("id") ?: query)
            .put("title", parsed?.optString("title")?.ifBlank { title } ?: title)
            .put("url", parsed?.optString("webpage_url")?.ifBlank { query } ?: query)
            .put("is_live", parsed?.optBoolean("is_live", false) ?: false)
            .put("availability", parsed?.optString("availability", "unknown") ?: "unknown")
            .put("status", "ok")
    }

    suspend fun canReachYoutube(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            ensureInitialized()
            true
        }.getOrDefault(false)
    }
}
