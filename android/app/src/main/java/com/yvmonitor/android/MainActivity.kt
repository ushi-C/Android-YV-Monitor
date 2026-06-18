package com.yvmonitor.android

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.yvmonitor.android.bridge.YVMonitorBridge
import com.yvmonitor.android.data.AvatarCache
import com.yvmonitor.android.data.CSVManager
import com.yvmonitor.android.data.ChannelRepository
import com.yvmonitor.android.service.ScanService
import com.yvmonitor.android.service.YoutubeDlService

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val csvManager = CSVManager(assets)
        val avatarCache = AvatarCache(cacheDir)
        val youtubeDlService = YoutubeDlService(applicationContext)
        val scanService = ScanService(youtubeDlService, avatarCache)
        val channelRepository = ChannelRepository(csvManager, scanService)

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            addJavascriptInterface(
                YVMonitorBridge(channelRepository, scanService, avatarCache, this),
                YVMonitorBridge.NAME,
            )
        }

        setContentView(webView)
        webView.loadUrl("file:///android_asset/www/index.html")
    }

    override fun onDestroy() {
        webView.removeJavascriptInterface(YVMonitorBridge.NAME)
        webView.destroy()
        super.onDestroy()
    }
}
