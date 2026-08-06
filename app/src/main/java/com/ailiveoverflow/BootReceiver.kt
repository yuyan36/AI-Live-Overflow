package com.ailiveoverflow

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ailiveoverflow.overlay.OverlayService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, OverlayService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}