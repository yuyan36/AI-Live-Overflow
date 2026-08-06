package com.ailiveoverflow.sensors

import android.content.Context
import android.os.Environment
import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import java.io.File

class ScreenshotDetector(
    private val context: Context,
    private val onScreenshot: () -> Unit
) {
    private var observer: FileObserver? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun start() {
        val screenshotsDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        ).resolve("Screenshots")

        if (!screenshotsDir.exists()) return

        observer = object : FileObserver(screenshotsDir.path, FileObserver.CREATE) {
            override fun onEvent(event: Int, path: String?) {
                if (path != null && (path.endsWith(".png") || path.endsWith(".jpg"))) {
                    mainHandler.post {
                        onScreenshot()
                    }
                }
            }
        }
        observer?.startWatching()
    }

    fun stop() {
        observer?.stopWatching()
        observer = null
    }
}