package com.example.aidiarycheomsak.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object GeminiService {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkDiary(
        serverUrl: String,
        content: String,
        originalContent: String? = null,
        feedback: String? = null,
        apiKey: String? = null
    ): GeminiDiaryResponse = withContext(Dispatchers.IO) {
        val cleanUrl = serverUrl.trim().removeSuffix("/")
        val urlConnection = URL("$cleanUrl/check-diary").openConnection() as HttpURLConnection
        try {
            urlConnection.requestMethod = "POST"
            urlConnection.setRequestProperty("Content-Type", "application/json")
            urlConnection.setRequestProperty("Accept", "application/json")
            urlConnection.doOutput = true
            urlConnection.connectTimeout = 60000
            urlConnection.readTimeout = 60000

            val payload = mutableMapOf<String, String>()
            payload["content"] = content
            if (originalContent != null) {
                payload["original_content"] = originalContent
            }
            if (feedback != null) {
                payload["feedback"] = feedback
            }
            if (!apiKey.isNullOrBlank()) {
                payload["api_key"] = apiKey
            }
            val requestBody = json.encodeToString(payload)

            OutputStreamWriter(urlConnection.outputStream).use { writer ->
                writer.write(requestBody)
                writer.flush()
            }

            val responseCode = urlConnection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = urlConnection.inputStream.bufferedReader().use { it.readText() }
                json.decodeFromString<GeminiDiaryResponse>(responseText)
            } else {
                val errorText = urlConnection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                val friendlyMessage = try {
                    val jsonObj = org.json.JSONObject(errorText)
                    jsonObj.optString("detail", errorText)
                } catch (e: Exception) {
                    errorText
                }
                throw Exception("서버 오류 ($responseCode): $friendlyMessage")
            }
        } finally {
            urlConnection.disconnect()
        }
    }
}
