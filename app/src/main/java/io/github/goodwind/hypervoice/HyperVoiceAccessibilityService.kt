package io.github.goodwind.hypervoice

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import io.github.goodwind.hypervoice.core.Dictation
import io.github.goodwind.hypervoice.core.MicButton
import io.github.goodwind.hypervoice.core.Outcome
import io.github.goodwind.hypervoice.core.TextInsertion
import io.github.goodwind.hypervoice.core.VoiceSession

/**
 * The voice remote's microphone button reaches Android as an unmapped key (scan code 63, no key code), so no app
 * sees it. This service does: while it is held, what is said is turned to text and typed into the field that has
 * focus, in any app. If an app's own microphone is already listening, the button belongs to it and only ends its phrase.
 */
class HyperVoiceAccessibilityService : AccessibilityService() {
    private val main = Handler(Looper.getMainLooper())
    private lateinit var overlay: Overlay
    private var dictation: VoiceSession? = null

    override fun onServiceConnected() {
        overlay = Overlay(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.scanCode != MIC_SCAN_CODE || event.keyCode != KeyEvent.KEYCODE_UNKNOWN) return false
        if (event.repeatCount == 0) Log.i(TAG, "microphone key, action ${event.action}")
        when (event.action) {
            KeyEvent.ACTION_DOWN -> if (event.repeatCount == 0) main.post { keyDown() }
            KeyEvent.ACTION_UP -> main.post { keyUp() }
        }
        return true
    }

    private fun keyDown() {
        if (Hub.recognition != null) return // an app is listening: the audio is going to it
        if (!Settings(this).hasKey) {
            overlay.show(getString(R.string.set_key), 3000)
            Hub.openSettingsForKey(this)
            return
        }
        // No field to type into, but the app has a microphone button of its own: press it, as a finger would, and
        // the app asks the recogniser for its text, which it knows what to do with (a search box, for one).
        val root = rootInActiveWindow
        if (root?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.isEditable != true) {
            val button = clickable(findMicButton(root))
            if (button != null && button.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                Log.i(TAG, "pressed the app's microphone button")
                return
            }
        }
        dictation?.cancel()
        overlay.show(getString(R.string.overlay_listening))
        val mine = Hub.session(this, object : VoiceSession.Listener {
            override fun onReady() = Unit
            override fun onSpeechStarted() = Unit
            override fun onLevel(rms: Double) = Unit
            override fun onSpeechEnded() { main.post { overlay.show(getString(R.string.overlay_recognizing)) } }
            override fun onResult(outcome: Outcome) { main.post { typed(outcome) } }
        })
        dictation = mine
        Hub.run(mine)
    }

    /** The first thing on screen that looks like a microphone button, looking through at most [LOOK_AT] elements. */
    private fun findMicButton(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null
        val queue = ArrayDeque<AccessibilityNodeInfo>().apply { add(root) }
        var seen = 0
        while (queue.isNotEmpty() && seen < LOOK_AT) {
            val node = queue.removeFirst()
            seen++
            if (node.isVisibleToUser && node.isEnabled && MicButton.matches(node.viewIdResourceName, node.contentDescription)) return node
            for (i in 0 until node.childCount) node.getChild(i)?.let { queue.add(it) }
        }
        return null
    }

    /** The element itself if it can be pressed, else the nearest one above it that can (the orb's picture sits in the orb). */
    private fun clickable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        var at = node
        var up = 0
        while (at != null && up < 4) {
            if (at.isClickable) return at
            at = at.parent
            up++
        }
        return null
    }

    private fun keyUp() {
        (Hub.recognition ?: dictation)?.finish()
    }

    private fun typed(outcome: Outcome) {
        dictation = null
        Log.i(TAG, "dictation result: ${if (outcome is Outcome.Text) "text, ${outcome.text.length} characters" else outcome}")
        if (outcome !is Outcome.Text) {
            overlay.show(outcome.notice(this), 2500)
            if (outcome == Outcome.NoKey || outcome == Outcome.Unauthorized) Hub.openSettingsForKey(this)
            return
        }
        val text = Dictation.clean(outcome.text)
        val field = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        Log.i(TAG, "focused input: ${field?.className}, editable ${field?.isEditable}")
        if (field == null || !field.isEditable) {
            (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("HyperVoice", text))
            overlay.show(getString(R.string.overlay_no_field, text), 3500)
            return
        }
        val hint = Build.VERSION.SDK_INT >= 26 && field.isShowingHintText
        val result = TextInsertion.insert(if (hint) null else field.text, field.textSelectionStart, field.textSelectionEnd, text)
        field.performAction(
            AccessibilityNodeInfo.ACTION_SET_TEXT,
            Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, result.text) },
        )
        field.performAction(
            AccessibilityNodeInfo.ACTION_SET_SELECTION,
            Bundle().apply {
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, result.cursor)
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, result.cursor)
            },
        )
        overlay.show(text, 1500)
    }

    override fun onDestroy() {
        dictation?.cancel()
        if (::overlay.isInitialized) overlay.remove()
        super.onDestroy()
    }

    companion object {
        /** The scan code of the microphone button on Xiaomi's Bluetooth voice remote (it arrives as KEYCODE_UNKNOWN). */
        const val MIC_SCAN_CODE = 63
        private const val TAG = "HyperVoice"
        private const val LOOK_AT = 500
    }
}
