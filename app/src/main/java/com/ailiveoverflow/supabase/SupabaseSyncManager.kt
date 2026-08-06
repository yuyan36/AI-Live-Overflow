package com.ailiveoverflow.supabase

import android.content.Context
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class SupabaseSyncManager(
    private val context: Context,
    private val onStateUpdate: (Map<String, Any>) -> Unit
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false
    private var lastPollTime = 0L

    companion object {
        private const val SUPABASE_URL = "https://fkmtigrnsigsnpkgrtzh.supabase.co"
        private const val SUPABASE_KEY = "sb_publishable_aexNYZST5sAI949xp6VjWA_jVaTJFVd"
        private const val JSON_MEDIA = "application/json"
    }

    fun start() {
        if (isRunning) return
        isRunning = true
        scope.launch {
            while (isActive) {
                pollState()
                delay(5000)
            }
        }
    }

    fun stop() {
        isRunning = false
        scope.cancel()
    }

    private fun pollState() {
        try {
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/clawd_state?select=*&order=created_at.desc&limit=1")
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer $SUPABASE_KEY")
                .get()
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {}

                override fun onResponse(call: Call, response: Response) {
                    response.body?.let { body ->
                        val json = JSONArray(body.string())
                        if (json.length() > 0) {
                            val state = json.getJSONObject(0)
                            val map = mutableMapOf<String, Any>()
                            if (state.has("expression"))
                                map["expression"] = state.getString("expression")
                            if (state.has("bubble"))
                                map["bubble"] = state.getString("bubble")
                            if (state.has("heat"))
                                map["heat"] = state.getDouble("heat")
                            if (map.isNotEmpty()) {
                                onStateUpdate(map)
                            }
                        }
                    }
                    response.close()
                }
            })
        } catch (_: Exception) {}
    }

    fun reportInteraction(type: String) {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", type)
                    put("timestamp", System.currentTimeMillis())
                    put("device_id", android.provider.Settings.Secure.getString(
                        context.contentResolver, android.provider.Settings.Secure.ANDROID_ID
                    ))
                }
                val request = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/interactions")
                    .header("apikey", SUPABASE_KEY)
                    .header("Authorization", "Bearer $SUPABASE_KEY")
                    .header("Content-Type", JSON_MEDIA)
                    .post(json.toString().toRequestBody(JSON_MEDIA.toMediaType()))
                    .build()
                client.newCall(request).execute()
            } catch (_: Exception) {}
        }
    }

    fun reportCurrentApp(appName: String) {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("app", appName)
                    put("timestamp", System.currentTimeMillis())
                }
                val request = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/app_usage")
                    .header("apikey", SUPABASE_KEY)
                    .header("Authorization", "Bearer $SUPABASE_KEY")
                    .header("Content-Type", JSON_MEDIA)
                    .post(json.toString().toRequestBody(JSON_MEDIA.toMediaType()))
                    .build()
                client.newCall(request).execute()
            } catch (_: Exception) {}
        }
    }

    fun reportScreenshot() {
        scope.launch {
            try {
                val json = JSONObject().apply {
                    put("type", "screenshot")
                    put("timestamp", System.currentTimeMillis())
                }
                val request = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/interactions")
                    .header("apikey", SUPABASE_KEY)
                    .header("Authorization", "Bearer $SUPABASE_KEY")
                    .header("Content-Type", JSON_MEDIA)
                    .post(json.toString().toRequestBody(JSON_MEDIA.toMediaType()))
                    .build()
                client.newCall(request).execute()
            } catch (_: Exception) {}
        }
    }
}