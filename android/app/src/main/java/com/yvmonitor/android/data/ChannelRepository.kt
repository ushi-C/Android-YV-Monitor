package com.yvmonitor.android.data

import com.yvmonitor.android.service.ScanService
import org.json.JSONArray
import org.json.JSONObject

class ChannelRepository(
    private val csvManager: CSVManager,
    private val scanService: ScanService,
) {
    @Volatile private var channelsCache: JSONArray? = null

    fun getChannels(): JSONObject = JSONObject().put("channels", getChannelsList())

    fun getChannelsList(): JSONArray {
        channelsCache?.let { return it }
        return csvManager.loadChannels().also { channelsCache = it }
    }

    suspend fun checkChannel(query: String, title: String): JSONObject {
        val result = scanService.checkSingleChannel(query, title)
        return JSONObject().put("result", result)
    }
}
