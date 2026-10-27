package io.github.goodwind.hypervoice

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import io.github.goodwind.hypervoice.core.Dictation
import io.github.goodwind.hypervoice.core.KeyPlan
import io.github.goodwind.hypervoice.core.MicButton
import io.github.goodwind.hypervoice.core.SearchButton
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
    private var keyHeld = false

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
        keyHeld = true
        val settings = Settings(this)
        val root = rootInActiveWindow
        val field = root?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.takeIf { it.isEditable }
        // Only look for buttons when there is no field, and only for the kinds that are switched on.
        val noField = field == null && Hub.recognition == null
        val mic = if (noField && settings.pressMicButton) clickable(find(root) { MicButton.matches(it.viewIdResourceName, it.contentDescription) }) else null
        val search = if (noField && mic == null && settings.openSearchButton) clickable(find(root) { SearchButton.matches(it.viewIdResourceName, it.contentDescription) }) else null
        when (KeyPlan.decide(Hub.recognition != null, field != null, settings.pressMicButton, mic != null, settings.openSearchButton, search != null, settings.hasKey)) {
            KeyPlan.Action.Ignore -> Unit // nothing to speak to: no sound, no note, no request
            KeyPlan.Action.NeedKey -> {
                overlay.show(getString(R.string.set_key), 3000)
                Hub.openSettingsForKey(this)
            }
            KeyPlan.Action.PressMicButton -> {
                // The app asks the recogniser for its text, which it knows what to do with (a search box, for one).
                Log.i(TAG, "pressed the app's microphone button")
                mic?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            KeyPlan.Action.OpenSearch -> {
                Log.i(TAG, "pressed the app's search button")
                if (search?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) waitForField(0)
            }
            KeyPlan.Action.Dictate -> dictate()
        }
    }

    /** The search screen opens and its field takes focus a moment later: wait for it while the button is still held. */
    private fun waitForField(tries: Int) {
        main.postDelayed({
            if (!keyHeld) return@postDelayed
            val field = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.takeIf { it.isEditable }
            when {
                field != null -> dictate()
                tries < 14 -> waitForField(tries + 1)
            }
        }, 150)
    }

    private fun dictate() {
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

    /** The first element of the window, looking through at most [LOOK_AT], that is visible, enabled and fits. */
    private fun find(root: AccessibilityNodeInfo?, fits: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (root == null) return null
        val queue = ArrayDeque<AccessibilityNodeInfo>().apply { add(root) }
        var seen = 0
        while (queue.isNotEmpty() && seen < LOOK_AT) {
            val node = queue.removeFirst()
            seen++
            if (node.isVisibleToUser && node.isEnabled && fits(node)) return node
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
        keyHeld = false
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
            // The field went away while the phrase was being recognised. Nothing is copied anywhere; the text is shown.
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
