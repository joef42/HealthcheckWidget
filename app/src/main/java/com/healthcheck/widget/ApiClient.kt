package com.healthcheck.widget

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ChecksStatus(val ok: Int, val total: Int) {
    val allOk: Boolean get() = total > 0 && ok == total
}

object ApiClient {

    private const val API_URL = "https://healthchecks.io/api/v1/checks/"
    private const val TIMEOUT_MS = 15_000

    /**
     * Fetches checks from the healthchecks.io API.
     * Returns null on network/parse error or non-200 response.
     * Must be called from a background thread.
     */
    fun fetchStatus(apiKey: String): ChecksStatus? {
        return try {
            val conn = (URL(API_URL).openConnection() as HttpURLConnection).apply {
                setRequestProperty("X-Api-Key", apiKey)
                setRequestProperty("Accept", "application/json")
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                requestMethod = "GET"
            }

            if (conn.responseCode != HttpURLConnection.HTTP_OK) return null

            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val checks = JSONObject(body).getJSONArray("checks")
            var ok = 0
            val total = checks.length()

            for (i in 0 until total) {
                val status = checks.getJSONObject(i).optString("status", "")
                if (status == "up") ok++
            }

            ChecksStatus(ok, total)
        } catch (e: Exception) {
            null
        }
    }
}
