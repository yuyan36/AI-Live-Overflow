package com.ailiveoverflow

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ailiveoverflow.overlay.OverlayService

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var startButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        startButton = findViewById(R.id.startButton)

        startButton.setOnClickListener {
            checkPermissionsAndStart()
        }

        checkAndRequestPermissions()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun checkPermissionsAndStart() {
        if (!checkOverlayPermission()) {
            requestOverlayPermission()
            return
        }
        if (!checkUsageStatsPermission()) {
            requestUsageStatsPermission()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!checkNotificationPermission()) {
                requestNotificationPermission()
                return
            }
        }
        startOverlayService()
    }

    private fun checkOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    private fun checkUsageStatsPermission(): Boolean {
        return try {
            val appOps = getSystemService(APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), packageName
            )
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    private fun checkNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun requestOverlayPermission() {
        AlertDialog.Builder(this)
            .setTitle("悬浮窗权限")
            .setMessage("桌宠需要悬浮窗权限才能显示在其他应用上方")
            .setPositiveButton("去设置") { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun requestUsageStatsPermission() {
        AlertDialog.Builder(this)
            .setTitle("使用情况访问权限")
            .setMessage("桌宠需要此权限才能感知你在用什么应用")
            .setPositiveButton("去设置") { _, _ ->
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100
            )
        }
    }

    private fun startOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "桌宠已启动 ♡", Toast.LENGTH_SHORT).show()
        updateStatus()
    }

    private fun updateStatus() {
        val running = OverlayService.isRunning
        statusText.text = if (running) "桌宠正在运行 ♡" else "桌宠未启动"
        startButton.text = if (running) "重新启动" else "启动桌宠"
    }

    private fun checkAndRequestPermissions() {
        val missing = mutableListOf<String>()
        if (!checkOverlayPermission()) missing.add("悬浮窗")
        if (!checkUsageStatsPermission()) missing.add("使用情况访问")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !checkNotificationPermission()) {
            missing.add("通知")
        }
        if (missing.isNotEmpty()) {
            statusText.text = "需要授权: ${missing.joinToString(", ")}"
        }
    }
}