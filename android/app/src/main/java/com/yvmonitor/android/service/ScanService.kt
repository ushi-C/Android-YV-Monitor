package com.yvmonitor.android.service

import com.yvmonitor.android.data.AvatarCache
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

class ScanService(
    private val youtubeDlService: YoutubeDlService,
    private val avatarCache: AvatarCache,
) {
    private val scanning = AtomicBoolean(false)
    @Volatile private var results = JSONArray()
    @Volatile private var progress = 0
    @Volatile private var total = 0
    @Volatile private var networkAvailable = true
    @Volatile private var networkReason = "UNKNOWN"

    suspend fun checkSingleChannel(query: String, title: String): JSONObject {
        require(query.isNotBlank()) { "query is required" }
        return youtubeDlService.probe(query, title)
    }

    suspend fun refreshScan(channels: JSONArray): JSONObject {
        if (!scanning.compareAndSet(false, true)) {
            return JSONObject().put("running", true).put("message", "scan already running")
        }
        return try {
            progress = 0
            total = channels.length()
            val nextResults = coroutineScope {
                (0 until channels.length()).map { index ->
                    async {
                        val channel = channels.getJSONObject(index)
                        runCatching {
                            checkSingleChannel(channel.optString("url").ifBlank { channel.optString("id") }, channel.optString("title"))
                        }.onSuccess { progress += 1 }
                            .onFailure { progress += 1 }
                            .getOrElse { error ->
                            JSONObject()
                                .put("id", channel.optString("id"))
                                .put("title", channel.optString("title"))
                                .put("status", "error")
                                .put("error", error.message ?: error.javaClass.simpleName)
                        }
                    }
                }.awaitAll()
            }
            results = JSONArray().also { array -> nextResults.forEach(array::put) }
            JSONObject().put("running", false).put("count", results.length())
        } finally {
            scanning.set(false)
        }
    }

    fun getStatus(): JSONObject = JSONObject()
        .put("is_running", scanning.get())
        .put("scanning", scanning.get())
        .put("is_monitoring", !scanning.get())
        .put("progress", progress)
        .put("total", total)
        .put("results", results)
        .put("network", getNetworkStatus())

    fun getNetworkStatus(): JSONObject = JSONObject()
        .put("youtube_available", networkAvailable)
        .put("reason", networkReason)

    fun reportNetworkStatus(available: Boolean, reason: String): JSONObject {
        networkAvailable = available
        networkReason = reason.ifBlank { "UNKNOWN" }
        return getNetworkStatus()
    }

    suspend fun requestNetworkCheck(): JSONObject {
        networkAvailable = youtubeDlService.canReachYoutube()
        networkReason = if (networkAvailable) "OK" else "YOUTUBE_DL_INIT_FAILED"
        return getNetworkStatus()
    }
}
