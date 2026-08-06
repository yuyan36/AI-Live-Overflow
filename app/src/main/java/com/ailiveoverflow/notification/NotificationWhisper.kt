package com.ailiveoverflow.notification

import android.content.Context
import kotlinx.coroutines.*

class NotificationWhisper(
    private val context: Context,
    private val onWhisper: (String) -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false

    private val whispers = mapOf(
        "morning" to listOf("早安呀~ 今天也要开开心心的！", "太阳晒屁股啦！", "新的一天，加油！"),
        "afternoon" to listOf("下午好~ 在忙什么呢？", "该喝口水了！", "我在这里看着你哦~"),
        "evening" to listOf("天黑了，该休息了~", "晚上好呀，今天过得开心吗？", "夜深了，别熬夜哦！"),
        "night" to listOf("这么晚了还不睡？", "熬夜对身体不好！", "快睡吧，晚安~"),
        "idle" to listOf("...好无聊", "戳我一下嘛", "我在这里~", "在干嘛呢？")
    )

    fun start() {
        if (isRunning) return
        isRunning = true
        scope.launch {
            while (isActive) {
                delay(3600000)
                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                val pool = when (hour) {
                    in 0..5 -> whispers["night"]
                    in 6..11 -> whispers["morning"]
                    in 12..17 -> whispers["afternoon"]
                    in 18..23 -> whispers["evening"]
                    else -> whispers["idle"]
                }
                val message = pool?.random() ?: "我在呢~"
                onWhisper(message)
            }
        }
    }

    fun stop() {
        isRunning = false
        scope.cancel()
    }
}