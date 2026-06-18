package com.yvmonitor.android.bridge

import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.yvmonitor.android.data.AvatarCache
import com.yvmonitor.android.data.ChannelRepository
import com.yvmonitor.android.service.ScanService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

class YVMonitorBridge(
    private val channelRepository: ChannelRepository,
    private val scanService: ScanService,
    private val avatarCache: AvatarCache,
    private val webView: WebView,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @JavascriptInterface
    fun call(method: String, payloadJson: String?, callbackId: String) {
        scope.launch {
            val response = runCatching {
                val payload = payloadJson?.takeIf { it.isNotBlank() }?.let(::JSONObject) ?: JSONObject()
                when (method) {
                    "getChannels" -> channelRepository.getChannels()
                    "checkChannel" -> channelRepository.checkChannel(payload.optString("query"), payload.optString("title"))
                    "refreshScan" -> scanService.refreshScan(channelRepository.getChannelsList())
                    "getStatus" -> scanService.getStatus()
                    "getNetworkStatus" -> scanService.getNetworkStatus()
                    "reportNetworkStatus" -> scanService.reportNetworkStatus(
                        payload.optBoolean("youtube_available"),
                        payload.optString("reason", "UNKNOWN"),
                    )
                    "requestNetworkCheck" -> scanService.requestNetworkCheck()
                    "getAvatar" -> avatarCache.resolveAvatar(payload.optString("url"))
                    else -> throw IllegalArgumentException("Unknown bridge method: $method")
                }
            }.fold(
                onSuccess = { JSONObject().put("ok", true).put("data", it) },
                onFailure = { JSONObject().put("ok", false).put("error", it.message ?: it.javaClass.simpleName) },
            )
            postCallback(callbackId, response)
        }
    }

    private fun postCallback(callbackId: String, response: JSONObject) {
        webView.post {
            val script = "window.__yvAndroidBridgeResolve && window.__yvAndroidBridgeResolve(" +
                JSONObject.quote(callbackId) + "," + response.toString() + ")"
            webView.evaluateJavascript(script, null)
        }
    }

    companion object {
        const val NAME = "YVMonitorAndroid"
    }
}
