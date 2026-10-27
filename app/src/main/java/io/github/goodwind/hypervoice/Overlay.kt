package io.github.goodwind.hypervoice

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

/** A small pill at the bottom of the screen that says what the microphone is doing, over any app. Main thread only. */
class Overlay(private val service: AccessibilityService) {
    private val windows = service.getSystemService(AccessibilityService.WINDOW_SERVICE) as WindowManager
    private val main = Handler(Looper.getMainLooper())
    private val view = TextView(service).apply {
        textSize = 26f
        setTextColor(Color.WHITE)
        setPadding(48, 22, 48, 22)
        background = GradientDrawable().apply { setColor(0xE6202024.toInt()); cornerRadius = 80f }
    }
    private var shown = false
    private val hide = Runnable { remove() }

    /** [forMs] 0 keeps it up until the next call. */
    fun show(text: String, forMs: Long = 0) {
        view.text = text
        main.removeCallbacks(hide)
        if (!shown) {
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT,
            ).apply { gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL; y = 90 }
            windows.addView(view, params)
            shown = true
        }
        if (forMs > 0) main.postDelayed(hide, forMs)
    }

    fun remove() {
        main.removeCallbacks(hide)
        if (shown) { windows.removeView(view); shown = false }
    }
}
