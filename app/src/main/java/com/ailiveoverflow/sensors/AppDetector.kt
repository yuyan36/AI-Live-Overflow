package com.ailiveoverflow.sensors

import android.app.usage.UsageStatsManager
import android.content.Context
import kotlinx.coroutines.*

class AppDetector(
    private val context: Context,
    private val onAppChanged: (String) -> Unit
) {
    private var lastApp = ""
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true
        scope.launch {
            while (isActive) {
                val currentApp = getCurrentApp()
                if (currentApp != lastApp && currentApp.isNotEmpty()) {
                    lastApp = currentApp
                    onAppChanged(currentApp)
                }
                delay(3000)
            }
        }
    }

    fun stop() {
        isRunning = false
        scope.cancel()
    }

    private fun getCurrentApp(): String {
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val currentTime = System.currentTimeMillis()
            val stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                currentTime - 1000 * 60 * 5,
                currentTime
            )
            if (stats.isNotEmpty()) {
                val latest = stats.maxByOrNull { it.lastTimeUsed }
                latest?.packageName ?: ""
            } else ""
        } catch (e: Exception) {
            ""
        }
    }

    fun getLastApp(): String = lastApp
}