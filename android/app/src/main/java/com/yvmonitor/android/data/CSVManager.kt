package com.yvmonitor.android.data

import android.content.res.AssetManager
import org.json.JSONArray
import org.json.JSONObject

class CSVManager(private val assets: AssetManager) {
    fun loadChannels(): JSONArray {
        val result = JSONArray()
        listCsvAssets().forEach { assetPath ->
            assets.open(assetPath).bufferedReader(Charsets.UTF_8).useLines { lines ->
                val rows = lines.filter { it.isNotBlank() }.toList()
                if (rows.size < 2) return@useLines
                val headers = parseCsvLine(rows.first())
                rows.drop(1).forEach { row ->
                    val columns = parseCsvLine(row)
                    val item = JSONObject()
                    headers.forEachIndexed { index, header ->
                        item.put(header, columns.getOrNull(index).orEmpty())
                    }
                    normalizeChannel(item)?.let(result::put)
                }
            }
        }
        return result
    }

    private fun listCsvAssets(): List<String> {
        val channels = assets.list("channels")?.filter { it.endsWith(".csv", ignoreCase = true) }.orEmpty()
            .map { "channels/$it" }
        val root = assets.list("")?.filter { it.endsWith(".csv", ignoreCase = true) }.orEmpty()
        return channels + root
    }

    private fun normalizeChannel(raw: JSONObject): JSONObject? {
        val id = raw.optString("id").ifBlank { raw.optString("channel_id") }
            .ifBlank { raw.optString("url") }
            .ifBlank { raw.optString("频道ID") }
        val title = raw.optString("title").ifBlank { raw.optString("name") }
            .ifBlank { raw.optString("频道名") }
        if (id.isBlank() && title.isBlank()) return null
        return JSONObject()
            .put("id", id)
            .put("url", raw.optString("url", id))
            .put("title", title.ifBlank { id })
            .put("group", raw.optString("group").ifBlank { raw.optString("分组") })
    }

    private fun parseCsvLine(line: String): List<String> {
        val values = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        var index = 0
        while (index < line.length) {
            val char = line[index]
            when {
                char == '"' && quoted && index + 1 < line.length && line[index + 1] == '"' -> {
                    current.append('"')
                    index++
                }
                char == '"' -> quoted = !quoted
                char == ',' && !quoted -> {
                    values += current.toString().trim()
                    current.clear()
                }
                else -> current.append(char)
            }
            index++
        }
        values += current.toString().trim()
        return values
    }
}
