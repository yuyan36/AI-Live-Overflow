package com.ailiveoverflow.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.app.NotificationCompat
import com.ailiveoverflow.R
import com.ailiveoverflow.emotion.EmotionEngine
import com.ailiveoverflow.notification.NotificationWhisper
import com.ailiveoverflow.sensors.AppDetector
import com.ailiveoverflow.sensors.BatteryMonitor
import com.ailiveoverflow.sensors.ScreenshotDetector
import com.ailiveoverflow.supabase.SupabaseSyncManager
import kotlinx.coroutines.*

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var webView: WebView
    private lateinit var layoutParams: WindowManager.LayoutParams
    private lateinit var supabaseSync: SupabaseSyncManager
    private lateinit var appDetector: AppDetector
    private lateinit var screenshotDetector: ScreenshotDetector
    private lateinit var batteryMonitor: BatteryMonitor
    private lateinit var emotionEngine: EmotionEngine
    private lateinit var notificationWhisper: NotificationWhisper
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private var clickCount = 0
    private var lastClickTime = 0L
    private var lastInteractionTime = System.currentTimeMillis()

    companion object {
        var isRunning = false
            private set
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "ailive_pet"
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        initWebView()
        initSensors()
        initEmotionEngine()
        initSupabaseSync()
        startPeriodicTasks()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isRunning = false
        serviceScope.cancel()
        supabaseSync.stop()
        appDetector.stop()
        screenshotDetector.stop()
        batteryMonitor.stop()
        notificationWhisper.stop()
        if (::webView.isInitialized) {
            windowManager.removeView(webView)
            webView.destroy()
        }
        super.onDestroy()
    }

    private fun initWebView() {
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)

        webView = WebView(this).apply {
            setBackgroundColor(0x00000000)
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            settings.apply {
                javaScriptEnabled = true
                allowFileAccess = true
                domStorageEnabled = true
            }
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    setPetState("idle")
                }
            }
        }

        val petSize = (size.x * 0.15).toInt().coerceIn(100, 200)

        layoutParams = WindowManager.LayoutParams(
            petSize,
            petSize,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = size.x - petSize - 50
            y = size.y - petSize - 200
        }

        webView.loadUrl("file:///android_asset/pet.html")
        windowManager.addView(webView, layoutParams)
        setupTouchListener()
    }

    private fun setupTouchListener() {
        webView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    lastInteractionTime = System.currentTimeMillis()
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isDragging = true
                        layoutParams.x = initialX + dx
                        layoutParams.y = initialY + dy
                        windowManager.updateViewLayout(webView, layoutParams)
                    }
                    false
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        handleClick()
                    } else {
                        val flingThreshold = 50
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > flingThreshold * 3 || Math.abs(dy) > flingThreshold * 3) {
                            handleFling(dx, dy)
                        }
                    }
                    false
                }
                else -> false
            }
        }
    }

    private fun handleClick() {
        val now = System.currentTimeMillis()
        if (now - lastClickTime < 500) {
            clickCount++
            if (clickCount >= 3) {
                evaluatePet("Wow, 连戳我${clickCount}次了！")
                clickCount = 0
            }
        } else {
            clickCount = 1
        }
        lastClickTime = now

        if (clickCount == 1) {
            evaluatePet("你戳我干嘛？")
        }
        if (clickCount == 2) {
            evaluatePet("哼！戳上瘾了是吧？")
        }

        supabaseSync.reportInteraction("click")
    }

    private fun handleFling(dx: Int, dy: Int) {
        evaluatePet("哎哟！把我甩这么远！")
        serviceScope.launch {
            delay(1000)
            withContext(Dispatchers.Main) {
                val display = windowManager.defaultDisplay
                val size = Point()
                display.getSize(size)
                layoutParams.x = size.x - webView.width - 50
                layoutParams.y = size.y - webView.height - 200
                windowManager.updateViewLayout(webView, layoutParams)
                evaluatePet("我爬回来啦！")
            }
        }
    }

    private fun initSensors() {
        appDetector = AppDetector(this) { appName ->
            onAppChanged(appName)
        }
        appDetector.start()

        screenshotDetector = ScreenshotDetector(this) {
            evaluatePet("你截图了？拍到我了吗？")
            supabaseSync.reportScreenshot()
        }
        screenshotDetector.start()

        batteryMonitor = BatteryMonitor(this) { level, isCharging ->
            handleBatteryChange(level, isCharging)
        }
        batteryMonitor.start()

        notificationWhisper = NotificationWhisper(this) { message ->
            updateNotificationWhisper(message)
        }
        notificationWhisper.start()
    }

    private fun initEmotionEngine() {
        emotionEngine = EmotionEngine()
    }

    private fun initSupabaseSync() {
        supabaseSync = SupabaseSyncManager(this) { state ->
            onStateUpdate(state)
        }
        supabaseSync.start()
    }

    private fun onStateUpdate(state: Map<String, Any>) {
        val expression = state["expression"] as? String
        val bubble = state["bubble"] as? String
        val heat = state["heat"] as? Double

        if (expression != null) {
            setPetState(expression)
        }
        if (bubble != null) {
            showBubble(bubble, "normal")
        }
        if (heat != null) {
            emotionEngine.setHeat(heat)
        }
    }

    private fun onAppChanged(appName: String) {
        val reaction = when {
            appName.contains("douyin") || appName.contains("tiktok") -> "吃醋"
            appName.contains("taobao") || appName.contains("jingdong") -> "审批"
            appName.contains("wechat") || appName.contains("qq") -> "偷看"
            appName.contains("study") || appName.contains("xuexi") -> "开心"
            else -> null
        }
        if (reaction != null) {
            evaluatePet("在看$appName？$reaction!")
        }
        supabaseSync.reportCurrentApp(appName)
    }

    private fun handleBatteryChange(level: Int, isCharging: Boolean) {
        if (isCharging) {
            evaluatePet("充电了！陪我多玩会儿~")
        } else if (level < 20) {
            evaluatePet("电量只剩$level%了，快去充电！")
        }
    }

    private fun evaluatePet(message: String) {
        showBubble(message, "normal")
        emotionEngine.boostHeat(10.0)
    }

    private fun showBubble(message: String, style: String) {
        val escapedMsg = message.replace("'", "\\'").replace("\n", "\\n")
        webView.evaluateJavascript("showBubble('$escapedMsg', '$style');", null)
    }

    private fun setPetState(state: String) {
        webView.evaluateJavascript("setState('$state');", null)
    }

    private fun updateNotificationWhisper(message: String) {
        val notification = createNotification(message)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun startPeriodicTasks() {
        serviceScope.launch {
            while (isActive) {
                delay(5000)
                withContext(Dispatchers.Main) {
                    checkLoneliness()
                    checkTimeBasedBehavior()
                }
            }
        }
    }

    private fun checkLoneliness() {
        val idleMinutes = (System.currentTimeMillis() - lastInteractionTime) / 60000
        val message = when {
            idleMinutes > 30 -> "Zzz... 你都不理我..."
            idleMinutes > 20 -> "好无聊啊... 你在干嘛？"
            idleMinutes > 15 -> "我都要睡着了..."
            idleMinutes > 10 -> "喂——还在吗？"
            idleMinutes > 5 -> "...?" 
            else -> return
        }
        showBubble(message, "whisper")
    }

    private fun checkTimeBasedBehavior() {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..5 -> if (Math.random() < 0.3) showBubble("这么晚了还不睡...", "whisper")
            in 6..8 -> if (Math.random() < 0.3) showBubble("早安呀宝宝~", "normal")
            in 12..13 -> if (Math.random() < 0.3) showBubble("吃饭了吗？", "normal")
            in 22..23 -> if (Math.random() < 0.3) showBubble("该睡觉了！", "normal")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AI桌宠",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "桌宠常驻通知"
                setShowBadge(false)
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String = "我在这里看着你哦 ♡"): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AI Live Overflow")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}