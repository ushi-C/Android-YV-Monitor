package com.yvmonitor.android.data

import org.json.JSONObject
import java.io.File

class AvatarCache(private val cacheDir: File) {
    private val avatarDir = File(cacheDir, "avatar_cache").apply { mkdirs() }

    fun resolveAvatar(url: String): JSONObject {
        // Placeholder for native avatar download/cache. Vue can keep using remote URLs until implemented.
        return JSONObject()
            .put("url", url)
            .put("cached", false)
            .put("cacheDir", avatarDir.absolutePath)
    }
}
