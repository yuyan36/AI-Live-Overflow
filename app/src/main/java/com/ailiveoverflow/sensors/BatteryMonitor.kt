package com.ailiveoverflow.sensors

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.*

class BatteryMonitor(
    private val context: Context,
    private val onBatteryChange: (Int, Boolean) -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false
    private var lastLevel = -1
    private var lastCharging: Boolean? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val isCharging = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ==
                    BatteryManager.BATTERY_STATUS_CHARGING

            val percent = (level * 100 / scale)
            if (percent != lastLevel || isCharging != lastCharging) {
                lastLevel = percent
                lastCharging = isCharging
                onBatteryChange(percent, isCharging)
            }
        }
    }

    fun start() {
        if (isRunning) return
        isRunning = true
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)
    }

    fun stop() {
        isRunning = false
        try {
            context.unregisterReceiver(receiver)
        } catch (_: Exception) {}
    }
}